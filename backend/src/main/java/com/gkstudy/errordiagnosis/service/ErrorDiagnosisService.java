package com.gkstudy.errordiagnosis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.common.BusinessException;
import com.gkstudy.errordiagnosis.ai.AiRootCauseAnalyzer;
import com.gkstudy.errordiagnosis.engine.ErrorDiagnosisEngine;
import com.gkstudy.errordiagnosis.engine.ErrorDiagnosisEngine.Candidate;
import com.gkstudy.errordiagnosis.mapper.ErrorDiagnosisMapper;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ErrorDiagnosisService {
    private static final TypeReference<List<KnowledgePointRef>> KNOWLEDGE_TYPE = new TypeReference<List<KnowledgePointRef>>() { };
    private final ErrorDiagnosisEngine engine;
    private final ErrorDiagnosisMapper diagnosisMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final LearningProblemMapper learningProblemMapper;
    private final ObjectMapper objectMapper;
    private final AiRootCauseAnalyzer aiAnalyzer;
    private final QuestionMapper questionMapper;

    @Autowired
    public ErrorDiagnosisService(ErrorDiagnosisEngine engine, ErrorDiagnosisMapper diagnosisMapper,
                                 AnswerRecordMapper answerRecordMapper, LearningProblemMapper learningProblemMapper,
                                 ObjectMapper objectMapper, AiRootCauseAnalyzer aiAnalyzer, QuestionMapper questionMapper) {
        this.engine = engine; this.diagnosisMapper = diagnosisMapper; this.answerRecordMapper = answerRecordMapper;
        this.learningProblemMapper = learningProblemMapper; this.objectMapper = objectMapper;
        this.aiAnalyzer = aiAnalyzer; this.questionMapper = questionMapper;
    }

    public ErrorDiagnosisService(ErrorDiagnosisEngine engine, ErrorDiagnosisMapper diagnosisMapper,
                                 AnswerRecordMapper answerRecordMapper, LearningProblemMapper learningProblemMapper,
                                 ObjectMapper objectMapper) {
        this(engine, diagnosisMapper, answerRecordMapper, learningProblemMapper, objectMapper, null, null);
    }

    public List<ErrorDiagnosis> listRecent(Long userId, int limit) {
        return diagnosisMapper.findRecentByUser(userId, Math.max(1, Math.min(limit, 200)));
    }

    @Transactional
    public ErrorDiagnosis diagnose(AnswerRecord current) {
        if (Boolean.TRUE.equals(current.getCorrect())) return null;
        List<AnswerRecord> allRecords = answerRecordMapper.findAllByUserId(current.getUserId());
        ErrorDiagnosis primary = null;
        for (KnowledgePointRef knowledge : parseKnowledge(current.getKnowledgeSnapshot())) {
            Candidate candidate = engine.evaluate(current, matchingRecords(allRecords, knowledge.getId()));
            if (candidate == null) continue;
            ErrorDiagnosis diagnosis = diagnosisMapper.findForUpdate(current.getUserId(), knowledge.getId(), candidate.getSuspectedCause());
            if (diagnosis == null) {
                diagnosis = new ErrorDiagnosis();
                diagnosis.setUserId(current.getUserId()); diagnosis.setKnowledgePointId(knowledge.getId());
                diagnosis.setSuspectedCause(candidate.getSuspectedCause()); diagnosis.setStatus("PENDING_CONFIRMATION");
                applyEvidence(diagnosis, current, knowledge, candidate);
                diagnosisMapper.insert(diagnosis);
            } else {
                applyEvidence(diagnosis, current, knowledge, candidate);
                diagnosisMapper.updateEvidence(diagnosis);
            }
            enrichWithAi(diagnosis, current, matchingRecords(allRecords, knowledge.getId()));
            if (!"REJECTED".equals(diagnosis.getStatus()) && (primary == null || diagnosis.getConfidence().compareTo(primary.getConfidence()) > 0)) primary = diagnosis;
        }
        return primary;
    }

    @Transactional
    public ErrorDiagnosis decide(Long userId, Long diagnosisId, boolean confirmed) {
        ErrorDiagnosis diagnosis = diagnosisMapper.findByIdForUpdate(diagnosisId, userId);
        if (diagnosis == null) throw new BusinessException("DIAGNOSIS_NOT_FOUND", "错因诊断不存在");
        diagnosis.setConfirmedByUser(confirmed); diagnosis.setStatus(confirmed ? "CONFIRMED" : "REJECTED");
        if (confirmed) diagnosis.setConfidence(BigDecimal.valueOf(100));
        diagnosisMapper.updateDecision(diagnosis);
        ErrorDiagnosis main = diagnosisMapper.findMain(userId, diagnosis.getKnowledgePointId());
        learningProblemMapper.updateRootCause(userId, diagnosis.getKnowledgePointId(), main == null ? null : rootCause(main));
        return diagnosis;
    }

    private void enrichWithAi(ErrorDiagnosis diagnosis, AnswerRecord current, List<AnswerRecord> recent) {
        if (aiAnalyzer == null || questionMapper == null || "CONFIRMED".equals(diagnosis.getAiStatus())) return;
        try {
            AiRootCauseAnalyzer.Analysis analysis = aiAnalyzer.analyze(questionMapper.findById(current.getQuestionId()),
                    questionMapper.findOptions(current.getQuestionId()), current, recent, diagnosis);
            diagnosis.setAiExplanation(analysis.getSuspectedCause() + "：" + analysis.getExplanation());
            diagnosis.setAiProvider(analysis.getResponse().getProvider()); diagnosis.setAiModel(analysis.getResponse().getModel());
            diagnosis.setAiPromptVersion(AiRootCauseAnalyzer.PROMPT_VERSION);
            diagnosis.setAiConfidence(BigDecimal.valueOf(analysis.getConfidence())); diagnosis.setAiStatus("PENDING_CONFIRMATION");
            diagnosis.setAiRawResponse(analysis.getResponse().getRawResponse());
            diagnosis.setAiRequestTime(analysis.getResponse().getRequestTime()); diagnosis.setAiLatencyMs(analysis.getResponse().getLatencyMs());
        } catch (RuntimeException e) {
            diagnosis.setAiStatus("FAILED"); diagnosis.setAiPromptVersion(AiRootCauseAnalyzer.PROMPT_VERSION);
        }
        diagnosisMapper.updateAiAnalysis(diagnosis);
    }

    private String rootCause(ErrorDiagnosis diagnosis) {
        return diagnosis.getAiExplanation() == null || diagnosis.getAiExplanation().trim().isEmpty()
                ? diagnosis.getSuspectedCause() : diagnosis.getAiExplanation();
    }

    private void applyEvidence(ErrorDiagnosis diagnosis, AnswerRecord current, KnowledgePointRef knowledge, Candidate candidate) {
        diagnosis.setAnswerRecordId(current.getId()); diagnosis.setQuestionId(current.getQuestionId());
        diagnosis.setOccurrenceCount(Math.max(value(diagnosis.getOccurrenceCount()), candidate.getOccurrenceCount()));
        if (!"CONFIRMED".equals(diagnosis.getStatus())) {
            BigDecimal oldConfidence = diagnosis.getConfidence() == null ? BigDecimal.ZERO : diagnosis.getConfidence();
            diagnosis.setConfidence(oldConfidence.max(candidate.getConfidence()));
        }
        diagnosis.setSelectedOptionKey(current.getUserAnswer());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("userAnswer", current.getUserAnswer()); evidence.put("correctAnswer", current.getCorrectAnswerSnapshot());
        evidence.put("errorType", current.getErrorType()); evidence.put("confidenceType", current.getConfidenceType());
        evidence.put("durationMs", current.getDurationMs()); evidence.put("standardTimeSeconds", current.getStandardTimeSecondsSnapshot());
        evidence.put("knowledgePointId", knowledge.getId()); evidence.put("knowledgePoint", knowledge.getName());
        evidence.put("sameCauseOccurrences", diagnosis.getOccurrenceCount()); evidence.put("latestAnswerRecordId", current.getId());
        try { diagnosis.setEvidenceJson(objectMapper.writeValueAsString(evidence)); }
        catch (Exception e) { throw new IllegalStateException("错因诊断证据序列化失败", e); }
    }

    private int value(Integer value) { return value == null ? 0 : value; }

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
        catch (Exception e) { throw new IllegalStateException("知识点快照无法用于错因诊断", e); }
    }
}
