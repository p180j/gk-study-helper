package com.gkstudy.learningproblem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.ability.mapper.AbilityMapper;
import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.ability.service.AbilityReplayService;
import com.gkstudy.ability.service.AbilityService;
import com.gkstudy.errordiagnosis.mapper.ErrorDiagnosisMapper;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.learningproblem.service.LearningProblemService;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LearningProblemServiceTest {
    @Test
    void repeatedEvaluationUpdatesSameProblemAndKeepsExplainableEvidence() {
        LearningProblemMapper problemMapper = mock(LearningProblemMapper.class); AbilityMapper abilityMapper = mock(AbilityMapper.class);
        AnswerRecordMapper answerMapper = mock(AnswerRecordMapper.class); ErrorDiagnosisMapper diagnosisMapper = mock(ErrorDiagnosisMapper.class);
        AtomicReference<LearningProblem> stored = new AtomicReference<>();
        List<AnswerRecord> records = records(false, false, false, false); AnswerRecord current = records.get(3);
        when(answerMapper.findAllByUserId(7L)).thenReturn(records); when(abilityMapper.findForUpdate(7L, 12L)).thenReturn(profile());
        when(problemMapper.findForUpdate(eq(7L), eq(12L), anyString())).thenAnswer(invocation ->
                LearningProblemEngine.MASTERY.equals(invocation.getArgument(2)) ? stored.get() : null);
        when(problemMapper.insert(any())).thenAnswer(invocation -> {
            LearningProblem problem = invocation.getArgument(0); problem.setId(88L); stored.set(problem); return 1;
        });
        ErrorDiagnosis diagnosis = new ErrorDiagnosis(); diagnosis.setSuspectedCause("条件理解偏差");
        when(diagnosisMapper.findMain(7L, 12L)).thenReturn(diagnosis);
        LearningProblemService service = new LearningProblemService(new LearningProblemEngine(), problemMapper, abilityMapper,
                answerMapper, diagnosisMapper, new ObjectMapper());

        service.evaluate(current);
        service.evaluate(current);

        verify(problemMapper, times(1)).insert(any()); verify(problemMapper, times(1)).update(any());
        verify(problemMapper, times(1)).insertHistory(any(), isNull(), eq(current.getAnswerTime()));
        assertEquals(88L, stored.get().getId()); assertEquals("CONFIRMED", stored.get().getStatus());
        assertTrue(stored.get().getEvidenceJson().contains("\"incorrectCount\":4"));
        assertTrue(stored.get().getEvidenceJson().contains("\"mastery\":35"));
        assertEquals("条件理解偏差", stored.get().getRootCause());
    }

    @Test
    void abilityReplayDoesNotCreateAnotherLearningProblem() {
        LearningProblemMapper problemMapper = mock(LearningProblemMapper.class); AbilityMapper abilityMapper = mock(AbilityMapper.class);
        AnswerRecordMapper answerMapper = mock(AnswerRecordMapper.class); AbilityService abilityService = mock(AbilityService.class);
        ErrorDiagnosisMapper diagnosisMapper = mock(ErrorDiagnosisMapper.class);
        List<AnswerRecord> records = records(false, false, false, false); AnswerRecord current = records.get(3);
        when(answerMapper.findAllByUserId(7L)).thenReturn(records); when(abilityMapper.findForUpdate(7L, 12L)).thenReturn(profile());
        when(problemMapper.findForUpdate(eq(7L), eq(12L), anyString())).thenReturn(null);
        when(problemMapper.insert(any())).thenAnswer(invocation -> { ((LearningProblem) invocation.getArgument(0)).setId(88L); return 1; });
        LearningProblemService problemService = new LearningProblemService(new LearningProblemEngine(), problemMapper, abilityMapper,
                answerMapper, diagnosisMapper, new ObjectMapper());
        problemService.evaluate(current);
        AbilityReplayService replayService = new AbilityReplayService(abilityMapper, answerMapper, abilityService);
        clearInvocations(problemMapper);

        replayService.replay(7L);
        replayService.replay(7L);

        verifyNoInteractions(problemMapper);
    }

    private AbilityProfile profile() {
        AbilityProfile profile = new AbilityProfile(); profile.setUserId(7L); profile.setKnowledgePointId(12L); profile.setSampleCount(4);
        profile.setMasteryScore(BigDecimal.valueOf(35)); profile.setSpeedScore(BigDecimal.valueOf(60));
        profile.setStabilityScore(BigDecimal.valueOf(60)); profile.setConfidenceScore(BigDecimal.valueOf(40)); return profile;
    }

    private List<AnswerRecord> records(Boolean... correctness) {
        List<AnswerRecord> records = new ArrayList<>();
        for (int index = 0; index < correctness.length; index++) {
            AnswerRecord record = new AnswerRecord(); record.setId((long) index + 1); record.setUserId(7L); record.setCorrect(correctness[index]);
            record.setDurationMs(60000L); record.setStandardTimeSecondsSnapshot(60); record.setPracticeType("DAILY");
            record.setAnswerTime(LocalDateTime.of(2026, 1, 1, 9, 0).plusMinutes(index));
            record.setKnowledgeSnapshot("[{\"id\":12,\"code\":\"AVG_GROWTH_RATE\",\"name\":\"年均增长率\",\"relationType\":\"PRIMARY\",\"weight\":1.0}]");
            records.add(record);
        }
        return records;
    }
}
