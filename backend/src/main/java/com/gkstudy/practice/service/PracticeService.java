package com.gkstudy.practice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.learningproblem.service.LearningProblemService;
import com.gkstudy.practice.dto.AnswerResult;
import com.gkstudy.practice.dto.SubmitAnswerRequest;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class PracticeService {
    private final QuestionService questionService;
    private final AnswerRecordMapper answerRecordMapper;
    private final ObjectMapper objectMapper;
    private final AbilityService abilityService;
    private final LearningProblemService learningProblemService;

    public PracticeService(QuestionService questionService, AnswerRecordMapper answerRecordMapper, ObjectMapper objectMapper,
                           AbilityService abilityService, LearningProblemService learningProblemService) {
        this.questionService = questionService;
        this.answerRecordMapper = answerRecordMapper;
        this.objectMapper = objectMapper;
        this.abilityService = abilityService;
        this.learningProblemService = learningProblemService;
    }

    @Transactional
    public AnswerResult submit(Long userId, SubmitAnswerRequest request) {
        Question question = questionService.answerableDetail(request.getQuestionId());
        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setQuestionId(question.getId());
        record.setQuestionVersion(question.getVersion());
        record.setPracticeType(request.getPracticeType());
        record.setUserAnswer(normalizeAnswer(request.getUserAnswer()));
        record.setCorrectAnswerSnapshot(normalizeAnswer(question.getAnswer()));
        record.setCorrect(record.getCorrectAnswerSnapshot().equals(record.getUserAnswer()));
        record.setDurationMs(request.getDurationMs());
        record.setStandardTimeSecondsSnapshot(question.getStandardTimeSeconds());
        record.setDifficultySnapshot(question.getDifficultyExpected());
        record.setKnowledgeSnapshot(writeKnowledgeSnapshot(question));
        record.setConfidenceType(request.getConfidenceType());
        record.setErrorType(request.getErrorType());
        record.setAnswerTime(LocalDateTime.now());
        answerRecordMapper.insert(record);
        List<AbilityChange> abilityChanges = abilityService.update(record);
        learningProblemService.evaluate(record);
        return new AnswerResult(record.getId(), record.getCorrect(), record.getCorrectAnswerSnapshot(), question.getAnalysis(), abilityChanges);
    }

    public List<AnswerRecord> history(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return answerRecordMapper.findByUserId(userId, (safePage - 1) * safeSize, safeSize);
    }

    private String writeKnowledgeSnapshot(Question question) {
        try {
            return objectMapper.writeValueAsString(question.getKnowledgePoints());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("知识点快照序列化失败", e);
        }
    }

    private String normalizeAnswer(String answer) { return answer == null ? null : answer.trim().toUpperCase(); }
}
