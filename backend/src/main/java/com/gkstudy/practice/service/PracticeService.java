package com.gkstudy.practice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.common.BusinessException;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.errordiagnosis.service.ErrorDiagnosisService;
import com.gkstudy.learningproblem.service.LearningProblemService;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.practice.dto.AnswerResult;
import com.gkstudy.practice.dto.SubmitAnswerRequest;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.service.QuestionInventoryService;
import com.gkstudy.question.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class PracticeService {
    private static final Pattern ERROR_TYPE_PATTERN = Pattern.compile("UNKNOWN|NOT_KNOW|FORMULA|CONDITION|CALCULATION|TIMEOUT|CARELESS");

    private final QuestionService questionService;
    private final AnswerRecordMapper answerRecordMapper;
    private final ObjectMapper objectMapper;
    private final AbilityService abilityService;
    private final ErrorDiagnosisService errorDiagnosisService;
    private final LearningProblemService learningProblemService;
    private final DailyPlanMapper planMapper;
    private final QuestionInventoryService inventoryService;

    public PracticeService(QuestionService questionService, AnswerRecordMapper answerRecordMapper, ObjectMapper objectMapper,
                           AbilityService abilityService, ErrorDiagnosisService errorDiagnosisService,
                           LearningProblemService learningProblemService, DailyPlanMapper planMapper,
                           QuestionInventoryService inventoryService) {
        this.questionService = questionService;
        this.answerRecordMapper = answerRecordMapper;
        this.objectMapper = objectMapper;
        this.abilityService = abilityService;
        this.errorDiagnosisService = errorDiagnosisService;
        this.learningProblemService = learningProblemService;
        this.planMapper = planMapper;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public AnswerResult submit(Long userId, SubmitAnswerRequest request) {
        if ("DAILY".equals(request.getPracticeType()) && request.getPlanItemId() == null) {
            throw new IllegalArgumentException("计划训练必须携带planItemId");
        }
        Question question = questionService.answerableDetail(request.getQuestionId());
        DailyPlanItem item = null;
        if (request.getPlanItemId() != null) {
            item = planMapper.findItemById(request.getPlanItemId());
            if (item == null) throw new BusinessException("PLAN_ITEM_NOT_FOUND", "计划任务不存在");
            AnswerResult duplicate = duplicateResult(userId, item, question);
            if (duplicate != null) return duplicate;
        }
        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setQuestionId(question.getId());
        record.setPlanItemId(item == null ? null : item.getId());
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
        record.setErrorType(request.getErrorType() == null ? "UNKNOWN" : request.getErrorType());
        record.setAnswerTime(LocalDateTime.now());
        answerRecordMapper.insert(record);
        List<AbilityChange> abilityChanges = abilityService.update(record);
        ErrorDiagnosis errorDiagnosis = errorDiagnosisService.diagnose(record);
        learningProblemService.evaluate(record);
        if (item != null) {
            planMapper.updateProgress(item.getId());
            planMapper.completePlanIfAllItemsCompleted(item.getId());
        }
        return new AnswerResult(record.getId(), record.getCorrect(), record.getCorrectAnswerSnapshot(), question.getAnalysis(),
                abilityChanges, errorDiagnosis, item == null ? null : taskProgress(userId, item, false));
    }

    public List<AnswerRecord> history(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return answerRecordMapper.findByUserId(userId, (safePage - 1) * safeSize, safeSize);
    }

    /** 修正答题记录错因类型：校验类型合法且记录归属当前用户。 */
    public void updateErrorType(Long userId, Long recordId, String errorType) {
        if (errorType == null || !ERROR_TYPE_PATTERN.matcher(errorType).matches())
            throw new BusinessException("INVALID_ERROR_TYPE", "错因类型不合法");
        if (answerRecordMapper.updateErrorType(userId, recordId, errorType) <= 0)
            throw new BusinessException("ANSWER_RECORD_NOT_FOUND", "答题记录不存在");
    }

    /** 幂等：同一 (user, planItem, question) 重复提交时直接返回旧记录结果，不写入、不更新能力、不推进任务。 */
    private AnswerResult duplicateResult(Long userId, DailyPlanItem item, Question question) {
        Long existingId = answerRecordMapper.findIdByUserPlanQuestion(userId, item.getId(), question.getId());
        if (existingId == null) return null;
        AnswerRecord existing = answerRecordMapper.findById(existingId);
        if (existing == null) return null;
        return new AnswerResult(existing.getId(), Boolean.TRUE.equals(existing.getCorrect()), existing.getCorrectAnswerSnapshot(),
                question.getAnalysis(), Collections.emptyList(), null, taskProgress(userId, item, true));
    }

    /** 组装任务进度：重读任务最新状态，判断是否达标与是否库存耗尽。 */
    private AnswerResult.TaskProgress taskProgress(Long userId, DailyPlanItem item, boolean duplicate) {
        DailyPlanItem latest = planMapper.findItemById(item.getId());
        if (latest == null) latest = item;
        Integer target = latest.getTargetQuestionCount();
        Integer completedCount = latest.getCompletedQuestionCount();
        boolean completed = target != null && completedCount != null && completedCount >= target;
        boolean insufficientStock = "INSUFFICIENT_STOCK".equals(latest.getStatus())
                || (!completed && inventoryService.countAvailable(latest.getKnowledgePointId(), latest.getPurpose(), userId) <= (completedCount == null ? 0 : completedCount));
        return new AnswerResult.TaskProgress(target, completedCount, completed, insufficientStock, duplicate);
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
