package com.gkstudy.coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ai.AiProvider;
import com.gkstudy.ai.AiProviderException;
import com.gkstudy.ai.AiResponse;
import com.gkstudy.coach.dto.CoachResponse;
import com.gkstudy.common.BusinessException;
import com.gkstudy.errordiagnosis.mapper.ErrorDiagnosisMapper;
import com.gkstudy.essay.mapper.EssayEvaluationMapper;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.service.DailyPlanService;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.reading.mapper.ReadingRecordMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiCoachServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void answersOnlyFromRealContextAndKeepsPlanAsFactSource() throws Exception {
        DailyPlan plan = new DailyPlan(); plan.setGenerationReason("优先处理年均增长率"); plan.setItems(Collections.emptyList());
        AiResponse ai = new AiResponse("OPENAI_COMPATIBLE", "model-x", mapper.readTree(
                "{\"currentStatus\":\"正在补强资料分析\",\"coreProblems\":[\"年均增长率掌握不足\"],\"todayReason\":\"优先处理年均增长率\",\"answer\":\"按今日计划训练\",\"evidence\":[\"优先处理年均增长率\"]}"),
                "raw", LocalDateTime.now(), 10);
        AiCoachService service = service((system, user, max) -> ai, plan);

        CoachResponse result = service.ask(1L, "我该学什么？");

        assertEquals("优先处理年均增长率", result.getTodayReason());
        assertEquals(Collections.singletonList("优先处理年均增长率"), result.getEvidence());
    }

    @Test
    void returnsControlledErrorWhenAiUnavailable() {
        DailyPlan plan = new DailyPlan(); plan.setGenerationReason("保持训练"); plan.setItems(Collections.emptyList());
        AiCoachService service = service((system, user, max) -> { throw new AiProviderException("AI_HTTP_ERROR", "down"); }, plan);
        BusinessException error = assertThrows(BusinessException.class, () -> service.ask(1L, null));
        assertEquals("AI_COACH_UNAVAILABLE", error.getCode());
    }

    private AiCoachService service(Completion completion, DailyPlan plan) {
        AiProvider provider = new AiProvider() {
            public AiResponse completeStructured(String system, String user, int max) { return completion.complete(system, user, max); }
            public boolean healthCheck() { return true; }
        };
        AbilityMapper ability = mock(AbilityMapper.class); LearningProblemMapper problems = mock(LearningProblemMapper.class);
        ErrorDiagnosisMapper diagnoses = mock(ErrorDiagnosisMapper.class); EssayEvaluationMapper evaluations = mock(EssayEvaluationMapper.class);
        ReadingRecordMapper readings = mock(ReadingRecordMapper.class); DailyPlanService plans = mock(DailyPlanService.class);
        AnswerRecordMapper answers = mock(AnswerRecordMapper.class);
        when(ability.findByUserId(1L)).thenReturn(Collections.emptyList()); when(problems.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(diagnoses.findRecentByUser(1L, 10)).thenReturn(Collections.emptyList());
        when(evaluations.findRecentSuccessfulByUser(1L, 5)).thenReturn(Collections.emptyList());
        when(plans.today(1L)).thenReturn(plan); when(answers.findByUserId(1L, 0, 20)).thenReturn(Collections.emptyList());
        return new AiCoachService(provider, ability, problems, diagnoses, evaluations, readings, plans, answers, mapper);
    }

    private interface Completion { AiResponse complete(String system, String user, int max); }
}
