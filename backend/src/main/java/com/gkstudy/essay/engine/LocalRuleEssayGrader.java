package com.gkstudy.essay.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.dto.ReferencePoint;
import com.gkstudy.essay.model.EssayQuestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地规则评分实现（明确标记的测试实现，真实 AI 评分后续阶段接入）。
 * 评分基于参考要点关键词命中率与字数达标率的确定性规则，仅用于在真实 AI
 * 评分器接入前让申论训练闭环（答题→评分→能力→问题→计划）可以先运行与验证，
 * 不应被视为对作答质量的权威判断。替换真实评分器时实现 EssayGrader 接口即可，
 * essay_answer 原始作答与历史评分记录均不受影响。
 */
@Component
public class LocalRuleEssayGrader implements EssayGrader {
    public static final String EVALUATOR = "LOCAL_RULE_V1";
    private static final double KEYWORD_HIT_RATIO = 0.5;
    private static final double DEFAULT_HIT_RATIO = 0.6;
    private static final double STRENGTH_SCORE = 70.0;
    private static final double PROBLEM_SCORE = 50.0;
    private static final TypeReference<List<ReferencePoint>> REFERENCE_POINTS_TYPE = new TypeReference<List<ReferencePoint>>() { };

    private final ObjectMapper objectMapper;

    public LocalRuleEssayGrader(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public String evaluator() { return EVALUATOR; }

    @Override
    public EssayEvaluationResult grade(EssayQuestion question, String answerText, long durationMs) {
        String text = answerText == null ? "" : answerText;
        int wordCount = countWords(text);
        List<ReferencePoint> points = parseReferencePoints(question.getReferencePointsJson());
        int hitCount = 0;
        List<String> missingPoints = new ArrayList<>();
        for (ReferencePoint point : points) {
            if (pointHit(text, point.getKeywords())) hitCount++;
            else missingPoints.add(point.getPoint());
        }
        double hitRatio = points.isEmpty() ? DEFAULT_HIT_RATIO : hitCount / (double) points.size();
        double wordRatio = wordRatio(wordCount, question);
        double materialReading = clamp(35.0 + hitRatio * 50.0 + wordRatio * 15.0);
        double infoExtraction = clamp(35.0 + hitRatio * 55.0 + wordRatio * 10.0);
        double pointCompleteness = clamp(25.0 + hitRatio * 70.0 + wordRatio * 5.0);
        String primaryDimension = EssayConstants.primaryDimension(question.getQuestionType());
        double primaryScore = clamp(30.0 + hitRatio * 50.0 + wordRatio * 20.0);
        double expression = clamp(30.0 + wordRatio * 60.0 + hitRatio * 10.0);

        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        dimensionScores.put(EssayConstants.DIM_MATERIAL_READING, round1(materialReading));
        dimensionScores.put(EssayConstants.DIM_INFO_EXTRACTION, round1(infoExtraction));
        dimensionScores.put(EssayConstants.DIM_POINT_COMPLETENESS, round1(pointCompleteness));
        dimensionScores.put(primaryDimension, round1(primaryScore));
        dimensionScores.put(EssayConstants.DIM_EXPRESSION, round1(expression));

        double totalScore = round1(primaryScore * 0.35 + pointCompleteness * 0.25 + infoExtraction * 0.15
                + materialReading * 0.10 + expression * 0.15);

        List<String> strengths = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dimensionScores.entrySet()) {
            if (entry.getValue() >= STRENGTH_SCORE) strengths.add(EssayConstants.dimensionName(entry.getKey()) + "：表现较好（" + entry.getValue() + " 分）");
        }
        for (Map.Entry<String, Double> entry : dimensionScores.entrySet()) {
            if (entry.getValue() < PROBLEM_SCORE) problems.add(EssayConstants.dimensionName(entry.getKey()) + "：" + entry.getValue() + " 分，低于达标线");
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("wordCount", wordCount);
        evidence.put("wordLimitMin", question.getWordLimitMin());
        evidence.put("wordLimitMax", question.getWordLimitMax());
        evidence.put("durationMs", durationMs);
        evidence.put("standardTimeSeconds", question.getStandardTimeSeconds());
        evidence.put("hitCount", hitCount);
        evidence.put("totalPoints", points.size());
        evidence.put("hitRatio", round1(hitRatio));
        evidence.put("questionType", question.getQuestionType());
        evidence.put("topicCode", question.getTopicCode());
        evidence.put("topicName", question.getTopicName());
        evidence.put("topicKnowledgePointId", question.getTopicKnowledgePointId());
        evidence.put("primaryDimension", primaryDimension);

        return new EssayEvaluationResult(EVALUATOR, totalScore, dimensionScores, strengths, problems, missingPoints,
                suggestions(dimensionScores, primaryDimension, question.getTopicName()), evidence);
    }

    private List<String> suggestions(Map<String, Double> dimensionScores, String primaryDimension, String topicName) {
        List<String> suggestions = new ArrayList<>();
        Double completeness = dimensionScores.get(EssayConstants.DIM_POINT_COMPLETENESS);
        if (completeness != null && completeness < PROBLEM_SCORE) {
            suggestions.add("要点覆盖不足，建议阅读《" + (topicName == null ? "" : topicName) + "》专题政治阅读材料，补充规范表达与对策素材");
        }
        Double expression = dimensionScores.get(EssayConstants.DIM_EXPRESSION);
        if (expression != null && expression < PROBLEM_SCORE) suggestions.add("注意控制字数在要求范围内，积累规范表达");
        Double primary = dimensionScores.get(primaryDimension);
        if (primary != null && primary < PROBLEM_SCORE) {
            if (EssayConstants.DIM_ANALYSIS.equals(primaryDimension)) suggestions.add("综合分析不足，注意从问题、原因、对策多角度展开");
            else suggestions.add("针对题型进行专项训练，对照参考要点检查遗漏");
        }
        return suggestions;
    }

    private boolean pointHit(String answerText, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return false;
        int present = 0;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.trim().isEmpty() && answerText.contains(keyword.trim())) present++;
        }
        return present / (double) keywords.size() >= KEYWORD_HIT_RATIO;
    }

    private double wordRatio(int wordCount, EssayQuestion question) {
        int min = question.getWordLimitMin() == null ? 100 : question.getWordLimitMin();
        int max = question.getWordLimitMax() == null ? 400 : question.getWordLimitMax();
        if (min <= 0 || (wordCount >= min && wordCount <= max)) return 1.0;
        if (wordCount < min) return wordCount / (double) min;
        return Math.min(1.0, max * 1.25 / wordCount);
    }

    private int countWords(String answerText) { return answerText.replaceAll("\\s+", "").length(); }

    private List<ReferencePoint> parseReferencePoints(String referencePointsJson) {
        if (referencePointsJson == null || referencePointsJson.trim().isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(referencePointsJson, REFERENCE_POINTS_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("申论参考要点无法解析", e);
        }
    }

    private double clamp(double value) { return Math.max(0.0, Math.min(100.0, value)); }
    private double round1(double value) { return Math.round(value * 10.0) / 10.0; }
}
