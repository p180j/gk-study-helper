package com.gkstudy.learningproblem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.errordiagnosis.mapper.ErrorDiagnosisMapper;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.engine.LearningProblemEngine.Evaluation;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.question.model.KnowledgePointRef;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LearningProblemService {
    private static final TypeReference<List<KnowledgePointRef>> KNOWLEDGE_TYPE = new TypeReference<List<KnowledgePointRef>>() { };
    private static final List<String> PROBLEM_TYPES = Arrays.asList(LearningProblemEngine.MASTERY, LearningProblemEngine.SPEED, LearningProblemEngine.STABILITY);
    private final LearningProblemEngine engine;
    private final LearningProblemMapper problemMapper;
    private final AbilityMapper abilityMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final ErrorDiagnosisMapper diagnosisMapper;
    private final ObjectMapper objectMapper;

    public LearningProblemService(LearningProblemEngine engine, LearningProblemMapper problemMapper, AbilityMapper abilityMapper,
                                  AnswerRecordMapper answerRecordMapper, ErrorDiagnosisMapper diagnosisMapper, ObjectMapper objectMapper) {
        this.engine = engine; this.problemMapper = problemMapper; this.abilityMapper = abilityMapper;
        this.answerRecordMapper = answerRecordMapper; this.diagnosisMapper = diagnosisMapper; this.objectMapper = objectMapper;
    }

    public List<LearningProblem> evaluate(AnswerRecord current) {
        List<LearningProblem> changed = new ArrayList<>();
        List<AnswerRecord> allRecords = answerRecordMapper.findAllByUserId(current.getUserId());
        for (KnowledgePointRef knowledge : parseKnowledge(current.getKnowledgeSnapshot())) {
            AbilityProfile profile = abilityMapper.findForUpdate(current.getUserId(), knowledge.getId());
            if (profile == null) continue;
            List<AnswerRecord> knowledgeRecords = matchingRecords(allRecords, knowledge.getId());
            for (String problemType : PROBLEM_TYPES) {
                LearningProblem problem = problemMapper.findForUpdate(current.getUserId(), knowledge.getId(), problemType);
                Evaluation evaluation = engine.evaluate(problemType, problem, profile, knowledgeRecords);
                if (evaluation == null) continue;
                String oldStatus = problem == null ? null : problem.getStatus();
                if (problem == null) problem = newProblem(current, knowledge, problemType);
                apply(problem, evaluation, profile, knowledge, current.getAnswerTime());
                if (problem.getId() == null) problemMapper.insert(problem); else problemMapper.update(problem);
                if (!Objects.equals(oldStatus, problem.getStatus())) problemMapper.insertHistory(problem, oldStatus, current.getAnswerTime());
                changed.add(problem);
            }
        }
        return changed;
    }

    public List<LearningProblem> problems(Long userId) { return problemMapper.findByUserId(userId); }

    private LearningProblem newProblem(AnswerRecord current, KnowledgePointRef knowledge, String problemType) {
        LearningProblem problem = new LearningProblem();
        problem.setUserId(current.getUserId()); problem.setKnowledgePointId(knowledge.getId()); problem.setProblemType(problemType);
        problem.setDiscoveredTime(current.getAnswerTime()); problem.setValidationCount(0); problem.setValidationPassCount(0);
        return problem;
    }

    private void apply(LearningProblem problem, Evaluation evaluation, AbilityProfile profile, KnowledgePointRef knowledge, LocalDateTime now) {
        problem.setTitle(knowledge.getName() + titleSuffix(problem.getProblemType()));
        problem.setDescription(description(problem.getProblemType(), evaluation, profile));
        problem.setSeverity(evaluation.getSeverity()); problem.setPriorityScore(evaluation.getPriorityScore()); problem.setStatus(evaluation.getStatus());
        problem.setValidationCount(evaluation.getValidationCount()); problem.setValidationPassCount(evaluation.getValidationPassCount());
        problem.setEvidenceJson(evidence(problem.getProblemType(), evaluation, profile));
        ErrorDiagnosis diagnosis = diagnosisMapper.findMain(problem.getUserId(), knowledge.getId());
        problem.setRootCause(diagnosis == null ? null : diagnosis.getSuspectedCause());
        if ("RESOLVED".equals(evaluation.getStatus()) && problem.getResolvedTime() == null) problem.setResolvedTime(now);
        if ("REOPENED".equals(evaluation.getStatus())) problem.setResolvedTime(null);
    }

    private String evidence(String problemType, Evaluation evaluation, AbilityProfile profile) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("problemType", problemType); evidence.put("recentCount", evaluation.getRecentCount());
        evidence.put("incorrectCount", evaluation.getIncorrectCount()); evidence.put("slowCount", evaluation.getSlowCount());
        evidence.put("transitionCount", evaluation.getTransitionCount()); evidence.put("abnormalCount", evaluation.getAbnormalCount());
        evidence.put("consecutivePasses", evaluation.getConsecutivePasses()); evidence.put("mastery", profile.getMasteryScore());
        evidence.put("speed", profile.getSpeedScore()); evidence.put("stability", profile.getStabilityScore());
        evidence.put("confidence", profile.getConfidenceScore()); evidence.put("sampleCount", profile.getSampleCount());
        evidence.put("validationCount", evaluation.getValidationCount()); evidence.put("validationPassCount", evaluation.getValidationPassCount());
        try { return objectMapper.writeValueAsString(evidence); }
        catch (Exception e) { throw new IllegalStateException("学习问题证据序列化失败", e); }
    }

    private String description(String problemType, Evaluation evaluation, AbilityProfile profile) {
        String recent = "最近" + evaluation.getRecentCount() + "题";
        if (LearningProblemEngine.MASTERY.equals(problemType)) recent += "错" + evaluation.getIncorrectCount() + "题";
        if (LearningProblemEngine.SPEED.equals(problemType)) recent += "超时" + evaluation.getSlowCount() + "题";
        if (LearningProblemEngine.STABILITY.equals(problemType)) recent += "对错切换" + evaluation.getTransitionCount() + "次";
        return recent + "；" + problemType.toLowerCase() + "=" + score(problemType, profile) + "；confidence=" + profile.getConfidenceScore();
    }

    private BigDecimal score(String type, AbilityProfile profile) {
        if (LearningProblemEngine.MASTERY.equals(type)) return profile.getMasteryScore();
        if (LearningProblemEngine.SPEED.equals(type)) return profile.getSpeedScore();
        return profile.getStabilityScore();
    }

    private String titleSuffix(String type) {
        if (LearningProblemEngine.MASTERY.equals(type)) return "掌握不足";
        if (LearningProblemEngine.SPEED.equals(type)) return "速度不足";
        return "稳定性不足";
    }

    private List<AnswerRecord> matchingRecords(List<AnswerRecord> records, Long knowledgePointId) {
        List<AnswerRecord> matching = new ArrayList<>();
        for (AnswerRecord record : records) {
            for (KnowledgePointRef knowledge : parseKnowledge(record.getKnowledgeSnapshot())) {
                if (knowledgePointId.equals(knowledge.getId())) { matching.add(record); break; }
            }
        }
        return matching;
    }

    private List<KnowledgePointRef> parseKnowledge(String snapshot) {
        try { return objectMapper.readValue(snapshot, KNOWLEDGE_TYPE); }
        catch (Exception e) { throw new IllegalStateException("知识点快照无法识别学习问题", e); }
    }
}
