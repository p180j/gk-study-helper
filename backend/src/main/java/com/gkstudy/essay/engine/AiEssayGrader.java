package com.gkstudy.essay.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.model.EssayQuestion;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class AiEssayGrader implements EssayGrader {
    public static final String EVALUATOR = "REAL_AI";
    public static final String PROMPT_VERSION = "ESSAY_GRADER_V1";
    private static final TypeReference<Map<String, Double>> SCORE_TYPE = new TypeReference<Map<String, Double>>() { };
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<List<String>>() { };
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;
    private final String model;

    public AiEssayGrader(AiProvider aiProvider, ObjectMapper objectMapper, @Value("${ai.model:}") String model) {
        this.aiProvider = aiProvider; this.objectMapper = objectMapper; this.model = model;
    }

    @Override
    public String evaluator() { return EVALUATOR; }
    @Override public String provider() { return "OPENAI_COMPATIBLE"; }
    @Override public String model() { return model; }
    @Override public String promptVersion() { return PROMPT_VERSION; }

    @Override
    public EssayEvaluationResult grade(EssayQuestion question, String answerText, long durationMs) {
        AiResponse response = aiProvider.completeStructured(systemPrompt(), userPrompt(question, answerText, durationMs), 2200);
        JsonNode root = response.getContent();
        double totalScore = requiredScore(root, "totalScore");
        Map<String, Double> dimensions = convert(root.get("dimensionScores"), SCORE_TYPE, "dimensionScores");
        if (dimensions.isEmpty()) throw invalid("dimensionScores 不能为空");
        for (Map.Entry<String, Double> entry : dimensions.entrySet()) validateScore(entry.getValue(), "dimensionScores." + entry.getKey());
        List<String> strengths = requiredList(root, "strengths");
        List<String> problems = requiredList(root, "problems");
        List<String> missingPoints = requiredList(root, "missingPoints");
        List<String> evidenceItems = requiredList(root, "evidence");
        List<String> suggestions = requiredList(root, "suggestions");
        double confidence = requiredConfidence(root, "confidence");
        validateEvidence(evidenceItems, question.getMaterial(), answerText);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("items", evidenceItems); evidence.put("durationMs", durationMs);
        evidence.put("questionType", question.getQuestionType()); evidence.put("topicCode", question.getTopicCode());
        evidence.put("topicName", question.getTopicName()); evidence.put("topicKnowledgePointId", question.getTopicKnowledgePointId());
        evidence.put("primaryDimension", EssayConstants.primaryDimension(question.getQuestionType()));
        return new EssayEvaluationResult(EVALUATOR, totalScore, dimensions, strengths, problems, missingPoints, suggestions,
                evidence, response.getProvider(), response.getModel(), PROMPT_VERSION, response.getRequestTime(),
                response.getLatencyMs(), "SUCCESS", confidence, response.getRawResponse());
    }

    private String systemPrompt() {
        return "你是严谨的公务员申论阅卷助手。只能依据给定材料、题目、评分规则、参考要点和用户原文评分。"
                + "只返回JSON对象，字段必须为totalScore、dimensionScores、strengths、problems、missingPoints、evidence、suggestions、confidence。"
                + "所有分数范围0到100；evidence必须是可在材料或用户答案中逐字找到的短句，不得虚构。";
    }

    private String userPrompt(EssayQuestion question, String answerText, long durationMs) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("material", question.getMaterial()); input.put("prompt", question.getPrompt());
        input.put("questionType", question.getQuestionType()); input.put("answerText", answerText);
        input.put("wordLimitMin", question.getWordLimitMin()); input.put("wordLimitMax", question.getWordLimitMax());
        input.put("referenceAnswer", question.getReferenceAnswer()); input.put("referencePoints", parseJson(question.getReferencePointsJson()));
        input.put("abilityDimensions", requiredDimensions(question)); input.put("durationMs", durationMs);
        input.put("scoringRule", "总分与各维度均按0-100评分；遗漏要点与证据分列；证据必须引用材料或作答原句");
        try { return objectMapper.writeValueAsString(input); }
        catch (Exception e) { throw new AiProviderException("AI_PROMPT_INVALID", "申论评分输入无法序列化", e); }
    }

    private List<String> requiredDimensions(EssayQuestion question) {
        List<String> dimensions = new ArrayList<>();
        dimensions.add(EssayConstants.DIM_MATERIAL_READING); dimensions.add(EssayConstants.DIM_INFO_EXTRACTION);
        dimensions.add(EssayConstants.DIM_POINT_COMPLETENESS); dimensions.add(EssayConstants.primaryDimension(question.getQuestionType()));
        dimensions.add(EssayConstants.DIM_EXPRESSION); return dimensions;
    }

    private Object parseJson(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try { return objectMapper.readTree(json); }
        catch (Exception e) { throw new AiProviderException("AI_PROMPT_INVALID", "参考要点 JSON 无法解析", e); }
    }

    private double requiredScore(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) throw invalid(field + " 必须是数字");
        double value = node.asDouble(); validateScore(value, field); return value;
    }

    private double requiredConfidence(JsonNode root, String field) {
        double value = requiredScore(root, field);
        return value <= 1 ? value * 100 : value;
    }

    private void validateScore(Double value, String field) {
        if (value == null || value.isNaN() || value < 0 || value > 100) throw invalid(field + " 超出0-100范围");
    }

    private List<String> requiredList(JsonNode root, String field) {
        return convert(root.get(field), LIST_TYPE, field);
    }

    private <T> T convert(JsonNode node, TypeReference<T> type, String field) {
        if (node == null || node.isNull()) throw invalid(field + " 缺失");
        try { return objectMapper.convertValue(node, type); }
        catch (Exception e) { throw new AiProviderException("AI_RESULT_INVALID", field + " 结构不合法", e); }
    }

    private void validateEvidence(List<String> evidence, String material, String answerText) {
        if (evidence.isEmpty()) throw invalid("evidence 不能为空");
        String sources = normalize((material == null ? "" : material) + (answerText == null ? "" : answerText));
        for (String item : evidence) {
            String normalized = normalize(item);
            if (normalized.length() < 4 || !sources.contains(normalized)) throw invalid("evidence 无法对应材料或用户答案");
        }
    }

    private String normalize(String value) { return value.replaceAll("[\\s，。；：、,.!?！？\"'“”‘’]", ""); }
    private AiProviderException invalid(String message) { return new AiProviderException("AI_RESULT_INVALID", message); }
}
