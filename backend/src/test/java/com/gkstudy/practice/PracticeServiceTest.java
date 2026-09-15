package com.gkstudy.practice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.common.BusinessException;
import com.gkstudy.learningproblem.service.LearningProblemService;
import com.gkstudy.practice.dto.AnswerResult;
import com.gkstudy.practice.dto.SubmitAnswerRequest;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.practice.service.PracticeService;
import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PracticeServiceTest {
    private QuestionService questionService;
    private AnswerRecordMapper answerRecordMapper;
    private PracticeService practiceService;
    private AbilityService abilityService;
    private LearningProblemService learningProblemService;

    @BeforeEach
    void setUp() {
        questionService = mock(QuestionService.class);
        answerRecordMapper = mock(AnswerRecordMapper.class);
        abilityService = mock(AbilityService.class);
        learningProblemService = mock(LearningProblemService.class);
        practiceService = new PracticeService(questionService, answerRecordMapper, new ObjectMapper(), abilityService, learningProblemService);
        doAnswer(invocation -> { AnswerRecord record = invocation.getArgument(0); record.setId(99L); return 1; }).when(answerRecordMapper).insert(any());
    }

    @Test
    void submitsCorrectAnswerAndSavesSnapshot() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        AnswerResult result = practiceService.submit(7L, request("B"));
        ArgumentCaptor<AnswerRecord> captor = ArgumentCaptor.forClass(AnswerRecord.class);
        verify(answerRecordMapper).insert(captor.capture());
        AnswerRecord record = captor.getValue();
        assertTrue(result.isCorrect());
        assertEquals(99L, result.getAnswerRecordId());
        assertEquals(3, record.getQuestionVersion());
        assertEquals("B", record.getCorrectAnswerSnapshot());
        assertEquals(68000L, record.getDurationMs());
        assertEquals("DAILY", record.getPracticeType());
        assertEquals("HESITANT", record.getConfidenceType());
        assertEquals("CONDITION", record.getErrorType());
        assertEquals(60, record.getStandardTimeSecondsSnapshot());
        assertTrue(record.getKnowledgeSnapshot().contains("GROWTH_RATE"));
        verify(abilityService).update(record);
        verify(learningProblemService).evaluate(record);
    }

    @Test
    void submitsWrongAnswer() {
        when(questionService.answerableDetail(1001L)).thenReturn(question("B"));
        AnswerResult result = practiceService.submit(7L, request("A"));
        assertFalse(result.isCorrect());
    }

    @Test
    void rejectsMissingQuestion() {
        when(questionService.answerableDetail(1001L)).thenThrow(new BusinessException("QUESTION_NOT_FOUND", "题目不存在"));
        BusinessException error = assertThrows(BusinessException.class, () -> practiceService.submit(7L, request("B")));
        assertEquals("QUESTION_NOT_FOUND", error.getCode());
        verify(answerRecordMapper, never()).insert(any());
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
