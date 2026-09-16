package com.gkstudy.essay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.dto.AbilityChange;
import com.gkstudy.common.BusinessException;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.essay.dto.EssayEvaluationResult;
import com.gkstudy.essay.dto.EssaySubmitRequest;
import com.gkstudy.essay.dto.EssaySubmitResult;
import com.gkstudy.essay.engine.EssayConstants;
import com.gkstudy.essay.engine.EssayGrader;
import com.gkstudy.essay.mapper.EssayAnswerMapper;
import com.gkstudy.essay.mapper.EssayEvaluationMapper;
import com.gkstudy.essay.mapper.EssayQuestionMapper;
import com.gkstudy.essay.model.EssayAnswer;
import com.gkstudy.essay.model.EssayEvaluation;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.essay.service.EssayAbilityService;
import com.gkstudy.essay.service.EssayProblemService;
import com.gkstudy.essay.service.EssayService;
import com.gkstudy.plan.service.DailyPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EssayServiceTest {
    private static final String ANSWER_TEXT = "产业兴旺 生态宜居\n治理有效"; // 12 个非空白字符

    private EssayQuestionMapper questionMapper;
    private EssayAnswerMapper answerMapper;
    private EssayEvaluationMapper evaluationMapper;
    private EssayGrader grader;
    private EssayAbilityService abilityService;
    private EssayProblemService problemService;
    private EssayService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        questionMapper = mock(EssayQuestionMapper.class);
        answerMapper = mock(EssayAnswerMapper.class);
        evaluationMapper = mock(EssayEvaluationMapper.class);
        grader = mock(EssayGrader.class);
        abilityService = mock(EssayAbilityService.class);
        problemService = mock(EssayProblemService.class);
        service = new EssayService(questionMapper, answerMapper, evaluationMapper, grader, abilityService,
                problemService, mock(DailyPlanService.class), mapper);
        doAnswer(invocation -> { EssayAnswer answer = invocation.getArgument(0); answer.setId(501L); return 1; })
                .when(answerMapper).insert(any());
    }

    @Test
    void submitInsertsAnswerWithSnapshotFields() {
        when(questionMapper.findActiveById(1001L)).thenReturn(question());
        when(grader.grade(any(), anyString(), anyLong())).thenReturn(graded());
        when(abilityService.apply(eq(7L), any(), any())).thenReturn(Collections.emptyList());

        service.submit(7L, request());

        ArgumentCaptor<EssayAnswer> captor = ArgumentCaptor.forClass(EssayAnswer.class);
        verify(answerMapper).insert(captor.capture());
        EssayAnswer answer = captor.getValue();
        assertEquals(7L, answer.getUserId());
        assertEquals(1001L, answer.getEssayQuestionId());
        assertEquals(3, answer.getQuestionVersion());
        assertEquals("DAILY", answer.getPracticeType());
        assertEquals(ANSWER_TEXT, answer.getAnswerText());
        assertEquals(680000L, answer.getDurationMs());
        assertEquals(12, answer.getWordCount());
        assertNotNull(answer.getSubmitTime());
    }

    @Test
    void submitNeverOverwritesRawAnswer() {
        when(questionMapper.findActiveById(1001L)).thenReturn(question());
        when(grader.grade(any(), anyString(), anyLong())).thenReturn(graded());
        when(abilityService.apply(eq(7L), any(), any())).thenReturn(Collections.emptyList());

        service.submit(7L, request());

        // essay_answer 只有 insert（mapper 本身无任何 update 方法），原始作答只写一次、不被评分覆盖
        ArgumentCaptor<EssayAnswer> captor = ArgumentCaptor.forClass(EssayAnswer.class);
        verify(answerMapper, times(1)).insert(captor.capture());
        verifyNoMoreInteractions(answerMapper);
        assertEquals(ANSWER_TEXT, captor.getValue().getAnswerText());
    }

    @Test
    void submitPersistsStructuredEvaluationAsJson() throws Exception {
        when(questionMapper.findActiveById(1001L)).thenReturn(question());
        when(grader.grade(any(), anyString(), anyLong())).thenReturn(graded());
        when(abilityService.apply(eq(7L), any(), any())).thenReturn(Collections.emptyList());

        service.submit(7L, request());

        ArgumentCaptor<EssayEvaluation> captor = ArgumentCaptor.forClass(EssayEvaluation.class);
        verify(evaluationMapper).insert(captor.capture());
        EssayEvaluation evaluation = captor.getValue();
        assertEquals(501L, evaluation.getEssayAnswerId());
        assertEquals("LOCAL_RULE_V1", evaluation.getEvaluator());
        assertEquals(0, evaluation.getTotalScore().compareTo(BigDecimal.valueOf(70.5)));
        assertEquals(5, mapper.readTree(evaluation.getDimensionScoresJson()).size());
        assertEquals(1, mapper.readTree(evaluation.getStrengthsJson()).size());
        assertEquals(1, mapper.readTree(evaluation.getProblemsJson()).size());
        assertEquals(1, mapper.readTree(evaluation.getMissingPointsJson()).size());
        assertTrue(mapper.readTree(evaluation.getEvidenceJson()).has("wordCount"));
        assertEquals(1, mapper.readTree(evaluation.getSuggestionsJson()).size());
    }

    @Test
    void rejectsInactiveQuestion() {
        when(questionMapper.findActiveById(1001L)).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class, () -> service.submit(7L, request()));
        assertEquals("ESSAY_QUESTION_NOT_FOUND", error.getCode());
        verify(answerMapper, never()).insert(any());
        verify(evaluationMapper, never()).insert(any());
        verify(abilityService, never()).apply(any(), any(), any());
        verify(problemService, never()).evaluate(any(), any(), any(), any());
    }

    @Test
    void submitReturnsEvaluationAndAbilityChanges() {
        EssayQuestion question = question();
        EssayEvaluationResult graded = graded();
        when(questionMapper.findActiveById(1001L)).thenReturn(question);
        when(grader.grade(any(), anyString(), anyLong())).thenReturn(graded);
        List<AbilityChange> changes = Collections.singletonList(new AbilityChange("ESSAY_SUMMARY",
                BigDecimal.valueOf(50), BigDecimal.valueOf(71), BigDecimal.valueOf(50), BigDecimal.valueOf(50),
                BigDecimal.valueOf(50), BigDecimal.valueOf(32), "LEARNING"));
        when(abilityService.apply(7L, question, graded)).thenReturn(changes);

        EssaySubmitResult result = service.submit(7L, request());

        assertEquals(501L, result.getEssayAnswerId());
        assertNotNull(result.getEvaluation());
        assertEquals(0, result.getEvaluation().getTotalScore().compareTo(BigDecimal.valueOf(70.5)));
        assertEquals(5, result.getEvaluation().getDimensionScores().size());
        assertSame(changes, result.getAbilityChanges());
        verify(problemService).evaluate(eq(7L), same(question), same(graded), any(LocalDateTime.class));
        verify(problemService).evaluateContentGap(eq(7L), eq(77L), eq("THEME_RURAL"), eq("乡村振兴"), any(LocalDateTime.class));
    }

    @Test
    void aiFailureKeepsAnswerAndDoesNotUpdateAbility() {
        when(questionMapper.findActiveById(1001L)).thenReturn(question());
        when(grader.evaluator()).thenReturn("REAL_AI");
        when(grader.provider()).thenReturn("OPENAI_COMPATIBLE");
        when(grader.model()).thenReturn("model-x");
        when(grader.promptVersion()).thenReturn("ESSAY_GRADER_V1");
        when(grader.grade(any(), anyString(), anyLong())).thenThrow(new AiProviderException("AI_HTTP_ERROR", "unavailable"));

        EssaySubmitResult result = service.submit(7L, request());

        assertEquals(501L, result.getEssayAnswerId());
        assertEquals("FAILED", result.getGradingStatus());
        assertTrue(result.isRetryable());
        verify(answerMapper).insert(any());
        ArgumentCaptor<EssayEvaluation> audit = ArgumentCaptor.forClass(EssayEvaluation.class);
        verify(evaluationMapper).insert(audit.capture());
        assertEquals("FAILED", audit.getValue().getStatus());
        verifyNoInteractions(abilityService);
        verifyNoInteractions(problemService);
    }

    private EssayQuestion question() {
        EssayQuestion question = new EssayQuestion();
        question.setId(1001L); question.setVersion(3); question.setStatus("ACTIVE");
        question.setQuestionType("SUMMARY"); question.setTopicKnowledgePointId(77L);
        question.setTopicCode("THEME_RURAL"); question.setTopicName("乡村振兴");
        question.setWordLimitMin(100); question.setWordLimitMax(400); question.setStandardTimeSeconds(1800);
        return question;
    }

    private EssaySubmitRequest request() {
        EssaySubmitRequest request = new EssaySubmitRequest();
        request.setEssayQuestionId(1001L); request.setAnswerText(ANSWER_TEXT);
        request.setDurationMs(680000L); request.setPracticeType("DAILY");
        return request;
    }

    private EssayEvaluationResult graded() {
        Map<String, Double> dims = new LinkedHashMap<>();
        dims.put(EssayConstants.DIM_MATERIAL_READING, 72.0);
        dims.put(EssayConstants.DIM_INFO_EXTRACTION, 70.0);
        dims.put(EssayConstants.DIM_POINT_COMPLETENESS, 68.0);
        dims.put(EssayConstants.DIM_SUMMARY, 71.0);
        dims.put(EssayConstants.DIM_EXPRESSION, 70.0);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("wordCount", 12); evidence.put("hitCount", 3); evidence.put("totalPoints", 3);
        return new EssayEvaluationResult("LOCAL_RULE_V1", 70.5, dims,
                Collections.singletonList("要点覆盖较好"), Collections.singletonList("文字表达偏弱"),
                Collections.singletonList("生态保护"), Collections.singletonList("补充规范表达"), evidence);
    }
}
