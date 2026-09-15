package com.gkstudy.errordiagnosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.errordiagnosis.engine.ErrorDiagnosisEngine;
import com.gkstudy.errordiagnosis.mapper.ErrorDiagnosisMapper;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.errordiagnosis.service.ErrorDiagnosisService;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.practice.mapper.AnswerRecordMapper;
import com.gkstudy.practice.model.AnswerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErrorDiagnosisServiceTest {
    private ErrorDiagnosisMapper diagnosisMapper;
    private AnswerRecordMapper answerMapper;
    private LearningProblemMapper problemMapper;
    private ErrorDiagnosisService service;
    private AtomicReference<ErrorDiagnosis> stored;
    private List<AnswerRecord> records;

    @BeforeEach
    void setUp() {
        diagnosisMapper = mock(ErrorDiagnosisMapper.class); answerMapper = mock(AnswerRecordMapper.class);
        problemMapper = mock(LearningProblemMapper.class); stored = new AtomicReference<>(); records = new ArrayList<>();
        service = new ErrorDiagnosisService(new ErrorDiagnosisEngine(), diagnosisMapper, answerMapper, problemMapper, new ObjectMapper());
        when(answerMapper.findAllByUserId(7L)).thenReturn(records);
        when(diagnosisMapper.findForUpdate(eq(7L), eq(12L), anyString())).thenAnswer(invocation -> stored.get());
        when(diagnosisMapper.insert(any())).thenAnswer(invocation -> {
            ErrorDiagnosis diagnosis = invocation.getArgument(0); diagnosis.setId(91L); stored.set(diagnosis); return 1;
        });
    }

    @Test
    void repeatedEvidenceUpdatesOneDiagnosisInsteadOfCreatingDuplicates() {
        AnswerRecord first = wrong(1L); records.add(first);
        ErrorDiagnosis firstResult = service.diagnose(first);
        int firstConfidence = firstResult.getConfidence().intValue();
        AnswerRecord second = wrong(2L); records.add(second);
        ErrorDiagnosis secondResult = service.diagnose(second);

        assertEquals(40, firstConfidence);
        assertEquals(55, secondResult.getConfidence().intValue());
        assertEquals(2, secondResult.getOccurrenceCount());
        assertTrue(secondResult.getEvidenceJson().contains("\"sameCauseOccurrences\":2"));
        verify(diagnosisMapper, times(1)).insert(any());
        verify(diagnosisMapper, times(1)).updateEvidence(any());
    }

    @Test
    void userCanConfirmAndLearningProblemReceivesMainCause() {
        ErrorDiagnosis diagnosis = existing();
        when(diagnosisMapper.findByIdForUpdate(91L, 7L)).thenReturn(diagnosis);
        when(diagnosisMapper.findMain(7L, 12L)).thenReturn(diagnosis);
        ErrorDiagnosis result = service.decide(7L, 91L, true);
        assertEquals("CONFIRMED", result.getStatus()); assertTrue(result.getConfirmedByUser());
        assertEquals(100, result.getConfidence().intValue());
        verify(problemMapper).updateRootCause(7L, 12L, "条件理解偏差");
    }

    @Test
    void userCanRejectAndRejectedCauseIsRemovedFromLearningProblem() {
        ErrorDiagnosis diagnosis = existing();
        when(diagnosisMapper.findByIdForUpdate(91L, 7L)).thenReturn(diagnosis);
        when(diagnosisMapper.findMain(7L, 12L)).thenReturn(null);
        ErrorDiagnosis result = service.decide(7L, 91L, false);
        assertEquals("REJECTED", result.getStatus()); assertFalse(result.getConfirmedByUser());
        verify(problemMapper).updateRootCause(7L, 12L, null);
    }

    @Test
    void correctAnswerDoesNotTouchDiagnosisStorage() {
        AnswerRecord record = wrong(1L); record.setCorrect(true);
        assertNull(service.diagnose(record));
        verifyNoInteractions(diagnosisMapper, answerMapper, problemMapper);
    }

    private ErrorDiagnosis existing() {
        ErrorDiagnosis diagnosis = new ErrorDiagnosis(); diagnosis.setId(91L); diagnosis.setUserId(7L);
        diagnosis.setKnowledgePointId(12L); diagnosis.setSuspectedCause("条件理解偏差");
        diagnosis.setStatus("PENDING_CONFIRMATION"); diagnosis.setConfidence(java.math.BigDecimal.valueOf(40));
        return diagnosis;
    }

    private AnswerRecord wrong(Long id) {
        AnswerRecord record = new AnswerRecord(); record.setId(id); record.setUserId(7L); record.setQuestionId(1001L);
        record.setCorrect(false); record.setUserAnswer("A"); record.setCorrectAnswerSnapshot("B");
        record.setErrorType("CONDITION"); record.setConfidenceType("HESITANT"); record.setDurationMs(68000L);
        record.setStandardTimeSecondsSnapshot(60);
        record.setKnowledgeSnapshot("[{\"id\":12,\"code\":\"AVG_GROWTH_RATE\",\"name\":\"年均增长率\"}]");
        return record;
    }
}
