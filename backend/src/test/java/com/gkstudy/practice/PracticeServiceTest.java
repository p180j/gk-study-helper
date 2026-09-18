package com.gkstudy.practice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.common.BusinessException;
import com.gkstudy.errordiagnosis.service.ErrorDiagnosisService;
import com.gkstudy.learningproblem.service.LearningProblemService;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.practice.dto.AnswerResult;
import com.gkstudy.practice.dto.SubmitAnswerRequest;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.practice.service.PracticeService;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.service.QuestionInventoryService;
import com.gkstudy.question.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PracticeServiceTest {
    private QuestionService questionService;
    private AnswerRecordMapper answerRecordMapper;
    private PracticeService practiceService;
    private AbilityService abilityService;
    private ErrorDiagnosisService errorDiagnosisService;
    private LearningProblemService learningProblemService;
    private DailyPlanMapper planMapper;
    private QuestionInventoryService inventoryService;

    @BeforeEach
    void setUp() {
        questionService = mock(QuestionService.class);
        answerRecordMapper = mock(AnswerRecordMapper.class);
        abilityService = mock(AbilityService.class);
        errorDiagnosisService = mock(ErrorDiagnosisService.class);
        learningProblemService = mock(LearningProblemService.class);
        planMapper = mock(DailyPlanMapper.class);
        inventoryService = mock(QuestionInventoryService.class);
        practiceService = new PracticeService(questionService, answerRecordMapper, new ObjectMapper(), abilityService,
                errorDiagnosisService, learningProblemService, planMapper, inventoryService);
        doAnswer(invocation -> { AnswerRecord record = invocation.getArgument(0); record.setId(99L); return 1; }).when(answerRecordMapper).insert(any());
    }

    @Test
    void submitsCorrectAnswerAndSavesSnapshot() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        SubmitAnswerRequest request = request("B"); request.setPracticeType("EXTRA");
        AnswerResult result = practiceService.submit(7L, request);
        ArgumentCaptor<AnswerRecord> captor = ArgumentCaptor.forClass(AnswerRecord.class);
        verify(answerRecordMapper).insert(captor.capture());
        AnswerRecord record = captor.getValue();
        assertTrue(result.isCorrect());
        assertEquals(99L, result.getAnswerRecordId());
        assertEquals(3, record.getQuestionVersion());
        assertEquals("B", record.getCorrectAnswerSnapshot());
        assertEquals(68000L, record.getDurationMs());
        assertEquals("EXTRA", record.getPracticeType());
        assertEquals("HESITANT", record.getConfidenceType());
        assertEquals("CONDITION", record.getErrorType());
        assertEquals(60, record.getStandardTimeSecondsSnapshot());
        assertTrue(record.getKnowledgeSnapshot().contains("GROWTH_RATE"));
        assertNull(record.getPlanItemId());
        verify(abilityService).update(record);
        verify(errorDiagnosisService).diagnose(record);
        verify(learningProblemService).evaluate(record);
    }

    @Test
    void submitsWrongAnswer() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        SubmitAnswerRequest request = request("A"); request.setPracticeType("EXTRA");
        AnswerResult result = practiceService.submit(7L, request);
        assertFalse(result.isCorrect());
    }

    @Test
    void defaultsOmittedErrorTypeToUnknownForCorrectAnswer() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        SubmitAnswerRequest request = request("B"); request.setPracticeType("EXTRA"); request.setErrorType(null);

        practiceService.submit(7L, request);

        ArgumentCaptor<AnswerRecord> captor = ArgumentCaptor.forClass(AnswerRecord.class);
        verify(answerRecordMapper).insert(captor.capture());
        assertEquals("UNKNOWN", captor.getValue().getErrorType());
    }

    @Test
    void rejectsMissingQuestion() {
        when(questionService.answerableDetail(1001L)).thenThrow(new BusinessException("QUESTION_NOT_FOUND", "题目不存在"));
        SubmitAnswerRequest request = request("B"); request.setPracticeType("EXTRA");
        BusinessException error = assertThrows(BusinessException.class, () -> practiceService.submit(7L, request));
        assertEquals("QUESTION_NOT_FOUND", error.getCode());
        verify(answerRecordMapper, never()).insert(any());
    }

    @Test
    void freePracticeWithoutPlanItemDoesNotTouchPlan() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        SubmitAnswerRequest request = request("B"); request.setPracticeType("EXTRA");

        AnswerResult result = practiceService.submit(7L, request);

        assertNull(result.getTaskProgress());
        verify(planMapper, never()).findItemById(anyLong());
        verify(planMapper, never()).updateProgress(anyLong());
    }

    @Test
    void firstAnswerOfFiveQuestionTaskAdvancesProgressWithoutCompletion() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        when(planMapper.findItemById(88L)).thenReturn(planItem(5, 0, "PENDING"), planItem(5, 1, "IN_PROGRESS"));
        when(inventoryService.countAvailable(12L, "TRAINING", 7L)).thenReturn(5);
        SubmitAnswerRequest request = request("B"); request.setPlanItemId(88L);

        AnswerResult result = practiceService.submit(7L, request);

        ArgumentCaptor<AnswerRecord> captor = ArgumentCaptor.forClass(AnswerRecord.class);
        verify(answerRecordMapper).insert(captor.capture());
        assertEquals(88L, captor.getValue().getPlanItemId());
        verify(planMapper).updateProgress(88L);
        AnswerResult.TaskProgress progress = result.getTaskProgress();
        assertNotNull(progress);
        assertEquals(5, progress.getTargetQuestionCount());
        assertEquals(1, progress.getCompletedQuestionCount());
        assertFalse(progress.isCompleted());
        assertFalse(progress.isInsufficientStock());
        assertFalse(progress.isDuplicate());
    }

    @Test
    void fourthAnswerOfFiveQuestionTaskIsStillNotCompleted() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        when(planMapper.findItemById(88L)).thenReturn(planItem(5, 3, "IN_PROGRESS"), planItem(5, 4, "IN_PROGRESS"));
        when(inventoryService.countAvailable(12L, "TRAINING", 7L)).thenReturn(5);
        SubmitAnswerRequest request = request("B"); request.setPlanItemId(88L);

        AnswerResult result = practiceService.submit(7L, request);

        AnswerResult.TaskProgress progress = result.getTaskProgress();
        assertEquals(4, progress.getCompletedQuestionCount());
        assertFalse(progress.isCompleted());
    }

    @Test
    void fifthAnswerOfFiveQuestionTaskCompletesTask() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        when(planMapper.findItemById(88L)).thenReturn(planItem(5, 4, "IN_PROGRESS"), planItem(5, 5, "COMPLETED"));
        when(inventoryService.countAvailable(12L, "TRAINING", 7L)).thenReturn(5);
        SubmitAnswerRequest request = request("B"); request.setPlanItemId(88L);

        AnswerResult result = practiceService.submit(7L, request);

        AnswerResult.TaskProgress progress = result.getTaskProgress();
        assertEquals(5, progress.getCompletedQuestionCount());
        assertTrue(progress.isCompleted());
        assertFalse(progress.isInsufficientStock());
        verify(planMapper).completePlanIfAllItemsCompleted(88L);
    }

    @Test
    void duplicatePlanItemSubmissionReturnsOldRecordWithoutProgress() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        when(planMapper.findItemById(88L)).thenReturn(planItem(5, 2, "IN_PROGRESS"));
        when(answerRecordMapper.findIdByUserPlanQuestion(7L, 88L, 1001L)).thenReturn(66L);
        AnswerRecord existing = new AnswerRecord(); existing.setId(66L); existing.setCorrect(false); existing.setCorrectAnswerSnapshot("B");
        when(answerRecordMapper.findById(66L)).thenReturn(existing);
        when(inventoryService.countAvailable(12L, "TRAINING", 7L)).thenReturn(5);
        SubmitAnswerRequest request = request("A"); request.setPlanItemId(88L);

        AnswerResult result = practiceService.submit(7L, request);

        verify(answerRecordMapper, never()).insert(any());
        verify(planMapper, never()).updateProgress(anyLong());
        verify(abilityService, never()).update(any());
        verify(errorDiagnosisService, never()).diagnose(any());
        verify(learningProblemService, never()).evaluate(any());
        assertEquals(66L, result.getAnswerRecordId());
        assertFalse(result.isCorrect());
        assertEquals("B", result.getCorrectAnswer());
        assertTrue(result.getAbilityChanges().isEmpty());
        assertNull(result.getErrorDiagnosis());
        AnswerResult.TaskProgress progress = result.getTaskProgress();
        assertTrue(progress.isDuplicate());
        assertEquals(2, progress.getCompletedQuestionCount());
        assertFalse(progress.isCompleted());
    }

    @Test
    void planItemSubmitMarksInsufficientStockWhenModuleExhausted() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        when(planMapper.findItemById(88L)).thenReturn(planItem(5, 2, "IN_PROGRESS"), planItem(5, 3, "IN_PROGRESS"));
        // 可用库存 3 = 已完成 3 → 任务仍差 2 题但已无题可取
        when(inventoryService.countAvailable(12L, "TRAINING", 7L)).thenReturn(3);
        SubmitAnswerRequest request = request("B"); request.setPlanItemId(88L);

        AnswerResult result = practiceService.submit(7L, request);

        AnswerResult.TaskProgress progress = result.getTaskProgress();
        assertFalse(progress.isCompleted());
        assertTrue(progress.isInsufficientStock());
    }

    @Test
    void rejectsUnknownPlanItem() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        when(planMapper.findItemById(88L)).thenReturn(null);
        SubmitAnswerRequest request = request("B"); request.setPlanItemId(88L);

        BusinessException error = assertThrows(BusinessException.class, () -> practiceService.submit(7L, request));

        assertEquals("PLAN_ITEM_NOT_FOUND", error.getCode());
        verify(answerRecordMapper, never()).insert(any());
    }

    @Test
    void rejectsDailyTrainingWithoutPlanItem() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> practiceService.submit(7L, request("B")));

        assertEquals("计划训练必须携带planItemId", error.getMessage());
        verify(questionService, never()).answerableDetail(anyLong());
        verify(answerRecordMapper, never()).insert(any());
    }

    @Test
    void updateErrorTypeValidatesValueAndUpdatesRecord() {
        when(answerRecordMapper.updateErrorType(7L, 66L, "CARELESS")).thenReturn(1);

        practiceService.updateErrorType(7L, 66L, "CARELESS");

        verify(answerRecordMapper).updateErrorType(7L, 66L, "CARELESS");
    }

    @Test
    void updateErrorTypeRejectsInvalidValue() {
        BusinessException error = assertThrows(BusinessException.class, () -> practiceService.updateErrorType(7L, 66L, "WRONG_TYPE"));

        assertEquals("INVALID_ERROR_TYPE", error.getCode());
        verify(answerRecordMapper, never()).updateErrorType(anyLong(), anyLong(), anyString());
    }

    @Test
    void updateErrorTypeRejectsRecordOfOtherUser() {
        when(answerRecordMapper.updateErrorType(7L, 66L, "CARELESS")).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> practiceService.updateErrorType(7L, 66L, "CARELESS"));

        assertEquals("ANSWER_RECORD_NOT_FOUND", error.getCode());
    }

    private DailyPlanItem planItem(int target, int completed, String status) {
        DailyPlanItem item = new DailyPlanItem(); item.setId(88L); item.setKnowledgePointId(12L);
        item.setPurpose("TRAINING"); item.setTargetQuestionCount(target); item.setCompletedQuestionCount(completed);
        item.setStatus(status); return item;
    }

    private Question question(String answer) {
        Question question = new Question();
        question.setId(1001L); question.setVersion(3); question.setAnswer(answer); question.setAnalysis("解析快照来源");
        question.setDifficultyExpected(new BigDecimal("62.50")); question.setStatus("ACTIVE");
        question.setStandardTimeSeconds(60);
        KnowledgePointRef knowledge = new KnowledgePointRef(); knowledge.setId(2L); knowledge.setCode("GROWTH_RATE"); knowledge.setName("增长率");
        question.setKnowledgePoints(Collections.singletonList(knowledge));
        return question;
    }

    private SubmitAnswerRequest request(String answer) {
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setQuestionId(1001L); request.setUserAnswer(answer); request.setDurationMs(68000L);
        request.setPracticeType("DAILY"); request.setConfidenceType("HESITANT"); request.setErrorType("CONDITION");
        return request;
    }
}
