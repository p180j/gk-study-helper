package com.gkstudy.essay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.common.BusinessException;
import com.gkstudy.essay.dto.AdminEssayAnswerView;
import com.gkstudy.essay.dto.CreateEssayQuestionRequest;
import com.gkstudy.essay.dto.DimensionScore;
import com.gkstudy.essay.dto.EvaluationView;
import com.gkstudy.essay.dto.ReferencePoint;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.mapper.EssayAnswerMapper;
import com.gkstudy.essay.mapper.EssayEvaluationMapper;
import com.gkstudy.essay.mapper.EssayQuestionMapper;
import com.gkstudy.essay.model.EssayAnswer;
import com.gkstudy.essay.model.EssayEvaluation;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.question.mapper.KnowledgePointMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminEssayService {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() { };
    private static final TypeReference<Map<String, Double>> SCORE_MAP_TYPE = new TypeReference<Map<String, Double>>() { };
    private static final TypeReference<Map<String, Object>> EVIDENCE_TYPE = new TypeReference<Map<String, Object>>() { };

    private final EssayQuestionMapper questionMapper;
    private final EssayAnswerMapper answerMapper;
    private final EssayEvaluationMapper evaluationMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;

    public AdminEssayService(EssayQuestionMapper questionMapper, EssayAnswerMapper answerMapper, EssayEvaluationMapper evaluationMapper,
                             KnowledgePointMapper knowledgePointMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper; this.answerMapper = answerMapper; this.evaluationMapper = evaluationMapper;
        this.knowledgePointMapper = knowledgePointMapper; this.objectMapper = objectMapper;
    }

    @Transactional
    public EssayQuestion createQuestion(CreateEssayQuestionRequest request) {
        Long topicKpId = knowledgePointMapper.findIdByCode(request.getTopicCode());
        if (topicKpId == null) throw new BusinessException("TOPIC_KP_NOT_FOUND", "主题知识点不存在: " + request.getTopicCode());
        EssayQuestion question = new EssayQuestion();
        question.setTopicKnowledgePointId(topicKpId);
        question.setQuestionType(request.getQuestionType());
        question.setMaterial(request.getMaterial());
        question.setPrompt(request.getPrompt());
        question.setWordLimitMin(request.getWordLimitMin() == null ? 100 : request.getWordLimitMin());
        question.setWordLimitMax(request.getWordLimitMax() == null ? 400 : request.getWordLimitMax());
        question.setStandardTimeSeconds(request.getStandardTimeSeconds() == null ? 1200 : request.getStandardTimeSeconds());
        question.setReferenceAnswer(request.getReferenceAnswer());
        question.setReferencePointsJson(toJson(request.getReferencePoints()));
        question.setSourceType("MANUAL");
        question.setSourceYear(request.getSourceYear());
        question.setSourceExam(request.getSourceExam());
        question.setSourceName(request.getSourceName());
        question.setStatus(normalizeStatus(request.getStatus()));
        question.setVersion(1);
        questionMapper.insert(question);
        return questionMapper.findById(question.getId());
    }

    @Transactional(readOnly = true)
    public EssayQuestion questionDetail(Long id) {
        EssayQuestion question = questionMapper.findById(id);
        if (question == null) throw new BusinessException("ESSAY_QUESTION_NOT_FOUND", "申论题目不存在");
        return question;
    }

    @Transactional(readOnly = true)
    public List<EssayQuestion> questions(String keyword, String questionType, String topicCode, String status, int page, int pageSize) {
        Long topicKpId = resolveTopicKpId(topicCode);
        if (topicKpId == null && topicCode != null && !topicCode.trim().isEmpty()) return new ArrayList<>();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        return questionMapper.adminList(keyword, questionType, topicKpId, status, (safePage - 1) * safeSize, safeSize);
    }

    @Transactional(readOnly = true)
    public int countQuestions(String keyword, String questionType, String topicCode, String status) {
        Long topicKpId = resolveTopicKpId(topicCode);
        if (topicKpId == null && topicCode != null && !topicCode.trim().isEmpty()) return 0;
        return questionMapper.adminCount(keyword, questionType, topicKpId, status);
    }

    @Transactional(readOnly = true)
    public List<EssayAnswer> answers(Long userId, Long essayQuestionId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        return answerMapper.adminList(userId, essayQuestionId, (safePage - 1) * safeSize, safeSize);
    }

    @Transactional(readOnly = true)
    public int countAnswers(Long userId, Long essayQuestionId) {
        return answerMapper.adminCount(userId, essayQuestionId);
    }

    @Transactional(readOnly = true)
    public AdminEssayAnswerView answerDetail(Long id) {
        EssayAnswer answer = answerMapper.findById(id);
        if (answer == null) throw new BusinessException("ESSAY_ANSWER_NOT_FOUND", "申论作答不存在");
        EssayQuestion question = questionMapper.findById(answer.getEssayQuestionId());
        EssayEvaluation evaluation = evaluationMapper.findByAnswerId(id);
        return new AdminEssayAnswerView(answer, question, evaluation == null ? null : toEvaluationView(evaluation));
    }

    private EvaluationView toEvaluationView(EssayEvaluation evaluation) {
        List<DimensionScore> dimensionScores = new ArrayList<>();
        Map<String, Double> scores = parse(evaluation.getDimensionScoresJson(), SCORE_MAP_TYPE);
        if (scores == null) scores = java.util.Collections.emptyMap();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            dimensionScores.add(new DimensionScore(entry.getKey(), EssayConstants.dimensionName(entry.getKey()),
                    BigDecimal.valueOf(entry.getValue()).setScale(1, RoundingMode.HALF_UP)));
        }
        return new EvaluationView(evaluation.getEvaluator(), evaluation.getTotalScore(), dimensionScores,
                list(evaluation.getStrengthsJson()), list(evaluation.getProblemsJson()), list(evaluation.getMissingPointsJson()),
                list(evaluation.getSuggestionsJson()), evidence(evaluation.getEvidenceJson()), evaluation.getProvider(), evaluation.getModel(),
                evaluation.getPromptVersion(), evaluation.getStatus(), evaluation.getConfidence());
    }

    private List<String> list(String json) {
        List<String> result = parse(json, STRING_LIST_TYPE); return result == null ? java.util.Collections.emptyList() : result;
    }

    private Map<String, Object> evidence(String json) {
        Map<String, Object> result = parse(json, EVIDENCE_TYPE); return result == null ? java.util.Collections.emptyMap() : result;
    }

    private Long resolveTopicKpId(String topicCode) {
        if (topicCode == null || topicCode.trim().isEmpty()) return null;
        return knowledgePointMapper.findIdByCode(topicCode);
    }

    private String normalizeStatus(String status) {
        if (!"DRAFT".equals(status) && !"ACTIVE".equals(status) && !"ARCHIVED".equals(status)) {
            throw new BusinessException("INVALID_STATUS", "申论题目状态仅允许 DRAFT/ACTIVE/ARCHIVED");
        }
        return status;
    }

    private String toJson(List<ReferencePoint> referencePoints) {
        if (referencePoints == null) return null;
        try { return objectMapper.writeValueAsString(referencePoints); }
        catch (JsonProcessingException e) { throw new IllegalStateException("申论参考要点序列化失败", e); }
    }

    private <T> T parse(String json, TypeReference<T> type) {
        if (json == null || json.trim().isEmpty()) return null;
        try { return objectMapper.readValue(json, type); }
        catch (JsonProcessingException e) { throw new IllegalStateException("申论评分 JSON 字段解析失败", e); }
    }
}
