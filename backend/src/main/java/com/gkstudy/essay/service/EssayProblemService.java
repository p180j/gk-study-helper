package com.gkstudy.essay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.engine.EssayProblemEngine;
import com.gkstudy.essay.engine.EssayProblemEngine.Evaluation;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.question.mapper.KnowledgePointMapper;
import com.gkstudy.reading.mapper.ReadingRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EssayProblemService {
    private static final double WEAK_SCORE = 45.0;
    private static final double THEME_WEAK_MASTERY = 50.0;

    private final EssayProblemEngine engine;
    private final LearningProblemMapper problemMapper;
    private final AbilityMapper abilityMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;

    public EssayProblemService(EssayProblemEngine engine, LearningProblemMapper problemMapper, AbilityMapper abilityMapper,
                               ReadingRecordMapper readingRecordMapper, KnowledgePointMapper knowledgePointMapper, ObjectMapper objectMapper) {
        this.engine = engine; this.problemMapper = problemMapper; this.abilityMapper = abilityMapper;
        this.readingRecordMapper = readingRecordMapper; this.knowledgePointMapper = knowledgePointMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void evaluate(Long userId, EssayQuestion question, EssayEvaluationResult result, LocalDateTime now) {
        for (Candidate candidate : candidates(question)) {
            Double score = result.getDimensionScores().get(candidate.dimensionCode);
            if (score == null) continue;
            Long kpId = knowledgePointMapper.findIdByCode(candidate.dimensionCode);
            if (kpId == null) continue;
            LearningProblem problem = problemMapper.findForUpdate(userId, kpId, candidate.problemType);
            if (problem == null && score >= WEAK_SCORE) continue;
            AbilityProfile profile = abilityMapper.findForUpdate(userId, kpId);
            int samples = profile == null || profile.getSampleCount() == null ? 0 : profile.getSampleCount();
            String oldStatus = problem == null ? null : problem.getStatus();
            Evaluation evaluation = engine.evaluate(candidate.problemType, problem, score, samples,
                    evidence(candidate, score, samples, question, result));
            if (evaluation == null) continue;
            if (problem == null) problem = newProblem(userId, kpId, candidate, now);
            apply(problem, evaluation, candidate, question, now);
            if (problem.getId() == null) problemMapper.insert(problem); else problemMapper.update(problem);
            if (!Objects.equals(oldStatus, problem.getStatus())) problemMapper.insertHistory(problem, oldStatus, now);
        }
    }

    @Transactional
    public void evaluateContentGap(Long userId, Long themeKpId, String themeCode, String themeName, LocalDateTime now) {
        if (themeKpId == null) return;
        LearningProblem problem = problemMapper.findForUpdate(userId, themeKpId, LearningProblemEngine.CONTENT_GAP);
        AbilityProfile profile = abilityMapper.findForUpdate(userId, themeKpId);
        if (profile == null) return;
        double mastery = profile.getMasteryScore() == null ? 50.0 : profile.getMasteryScore().doubleValue();
        int samples = profile.getSampleCount() == null ? 0 : profile.getSampleCount();
        int completed = readingRecordMapper.countCompletedByUserAndTopicKpId(userId, themeKpId);
        String oldStatus;
        if (problem == null) {
            if (samples < 1 || mastery >= THEME_WEAK_MASTERY || completed > 0) return;
            problem = new LearningProblem();
            problem.setUserId(userId); problem.setKnowledgePointId(themeKpId);
            problem.setProblemType(LearningProblemEngine.CONTENT_GAP);
            problem.setDiscoveredTime(now); problem.setValidationCount(0); problem.setValidationPassCount(0);
            problem.setTitle("《" + themeName + "》主题素材缺口");
            problem.setDescription("申论主题能力 " + mastery + " 分且未完成该主题任何政治阅读材料，判断为素材积累缺口");
            problem.setStatus("CONFIRMED");
            oldStatus = null;
        } else {
            oldStatus = problem.getStatus();
            if (completed >= 1) {
                if ("RESOLVED".equals(problem.getStatus())) return;
                problem.setStatus("RESOLVED");
            } else if (mastery >= THEME_WEAK_MASTERY) {
                if (!"CONFIRMED".equals(problem.getStatus())) return;
                problem.setStatus("OBSERVING");
            } else {
                problem.setStatus("CONFIRMED");
            }
        }
        BigDecimal severity = decimal(clamp((THEME_WEAK_MASTERY - mastery) / THEME_WEAK_MASTERY * 100.0, 20.0, 95.0));
        problem.setSeverity(severity);
        problem.setPriorityScore(decimal(severity.doubleValue() * 0.7 + 20.0));
        problem.setEvidenceJson(contentGapEvidence(themeCode, themeName, mastery, samples, completed));
        if ("RESOLVED".equals(problem.getStatus()) && problem.getResolvedTime() == null) problem.setResolvedTime(now);
        if (problem.getId() == null) problemMapper.insert(problem); else problemMapper.update(problem);
        if (!Objects.equals(oldStatus, problem.getStatus())) problemMapper.insertHistory(problem, oldStatus, now);
    }

    private List<Candidate> candidates(EssayQuestion question) {
        String primary = EssayConstants.primaryDimension(question.getQuestionType());
        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate(LearningProblemEngine.ESSAY_MISSING_POINTS, EssayConstants.DIM_POINT_COMPLETENESS, "要点完整性"));
        if (EssayConstants.DIM_ANALYSIS.equals(primary)) candidates.add(new Candidate(LearningProblemEngine.ESSAY_ANALYSIS, EssayConstants.DIM_ANALYSIS, "综合分析"));
        if (EssayConstants.DIM_SUMMARY.equals(primary) || EssayConstants.DIM_IMPLEMENTATION.equals(primary)) {
            candidates.add(new Candidate(LearningProblemEngine.ESSAY_STRUCTURE, primary, EssayConstants.dimensionName(primary)));
        }
        candidates.add(new Candidate(LearningProblemEngine.ESSAY_EXPRESSION, EssayConstants.DIM_EXPRESSION, "文字表达"));
        return candidates;
    }

    private LearningProblem newProblem(Long userId, Long kpId, Candidate candidate, LocalDateTime now) {
        LearningProblem problem = new LearningProblem();
        problem.setUserId(userId); problem.setKnowledgePointId(kpId); problem.setProblemType(candidate.problemType);
        problem.setDiscoveredTime(now); problem.setValidationCount(0); problem.setValidationPassCount(0);
        return problem;
    }

    private void apply(LearningProblem problem, Evaluation evaluation, Candidate candidate, EssayQuestion question, LocalDateTime now) {
        problem.setTitle(title(question, candidate));
        problem.setDescription("申论评分维度「" + candidate.dimensionName + "」低于达标线，需要针对性训练");
        problem.setSeverity(evaluation.getSeverity()); problem.setPriorityScore(evaluation.getPriorityScore());
        problem.setStatus(evaluation.getStatus());
        problem.setValidationCount(evaluation.getValidationCount()); problem.setValidationPassCount(evaluation.getValidationPassCount());
        problem.setEvidenceJson(evaluation.getEvidenceJson());
        if ("RESOLVED".equals(evaluation.getStatus()) && problem.getResolvedTime() == null) problem.setResolvedTime(now);
        if ("REOPENED".equals(evaluation.getStatus())) problem.setResolvedTime(null);
    }

    private String title(EssayQuestion question, Candidate candidate) {
        String topicName = question.getTopicName() == null ? "" : question.getTopicName();
        if (LearningProblemEngine.ESSAY_MISSING_POINTS.equals(candidate.problemType)) return topicName + "·要点完整性问题";
        if (LearningProblemEngine.ESSAY_ANALYSIS.equals(candidate.problemType)) return topicName + "·综合分析问题";
        if (LearningProblemEngine.ESSAY_STRUCTURE.equals(candidate.problemType)) return topicName + "·" + candidate.dimensionName + "结构问题";
        return topicName + "·文字表达问题";
    }

    private String evidence(Candidate candidate, double score, int samples, EssayQuestion question, EssayEvaluationResult result) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("dimension", candidate.dimensionCode);
        evidence.put("dimensionName", candidate.dimensionName);
        evidence.put("score", score);
        evidence.put("samples", samples);
        evidence.put("topicCode", question.getTopicCode());
        evidence.put("topicName", question.getTopicName());
        evidence.put("topicKnowledgePointId", question.getTopicKnowledgePointId());
        evidence.put("questionType", question.getQuestionType());
        if (LearningProblemEngine.ESSAY_MISSING_POINTS.equals(candidate.problemType)) evidence.put("missingPoints", result.getMissingPoints());
        try { return objectMapper.writeValueAsString(evidence); }
        catch (Exception e) { throw new IllegalStateException("申论问题证据序列化失败", e); }
    }

    private String contentGapEvidence(String themeCode, String themeName, double mastery, int samples, int completed) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("topicCode", themeCode);
        evidence.put("topicName", themeName);
        evidence.put("essayMastery", mastery);
        evidence.put("essaySamples", samples);
        evidence.put("completedMaterials", completed);
        try { return objectMapper.writeValueAsString(evidence); }
        catch (Exception e) { throw new IllegalStateException("主题素材缺口证据序列化失败", e); }
    }

    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private BigDecimal decimal(double value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP); }

    private static class Candidate {
        private final String problemType;
        private final String dimensionCode;
        private final String dimensionName;

        private Candidate(String problemType, String dimensionCode, String dimensionName) {
            this.problemType = problemType; this.dimensionCode = dimensionCode; this.dimensionName = dimensionName;
        }
    }
}
