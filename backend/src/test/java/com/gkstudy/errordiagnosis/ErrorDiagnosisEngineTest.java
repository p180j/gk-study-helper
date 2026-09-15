package com.gkstudy.errordiagnosis;

import com.gkstudy.errordiagnosis.engine.ErrorDiagnosisEngine;
import com.gkstudy.errordiagnosis.engine.ErrorDiagnosisEngine.Candidate;
import com.gkstudy.practice.model.AnswerRecord;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ErrorDiagnosisEngineTest {
    private final ErrorDiagnosisEngine engine = new ErrorDiagnosisEngine();

    @Test
    void singleErrorCreatesOnlyLowConfidenceCandidate() {
        AnswerRecord record = wrong("CONDITION", "HESITANT");
        Candidate candidate = engine.evaluate(record, Collections.singletonList(record));
        assertNotNull(candidate);
        assertEquals("条件理解偏差", candidate.getSuspectedCause());
        assertEquals(40, candidate.getConfidence().intValue());
        assertEquals(1, candidate.getOccurrenceCount());
    }

    @Test
    void repeatedSameCauseRaisesConfidence() {
        AnswerRecord first = wrong("CONDITION", "HESITANT");
        AnswerRecord second = wrong("CONDITION", "SURE");
        AnswerRecord third = wrong("CONDITION", "GUESS");
        Candidate candidate = engine.evaluate(third, Arrays.asList(first, second, third));
        assertEquals(70, candidate.getConfidence().intValue());
        assertEquals(3, candidate.getOccurrenceCount());
    }

    @Test
    void correctOrEvidenceFreeAnswerDoesNotCreateMeaninglessCandidate() {
        AnswerRecord correct = wrong("UNKNOWN", "UNKNOWN"); correct.setCorrect(true);
        assertNull(engine.evaluate(correct, Collections.singletonList(correct)));
        AnswerRecord unclear = wrong("UNKNOWN", "UNKNOWN"); unclear.setDurationMs(30000L);
        assertNull(engine.evaluate(unclear, Collections.singletonList(unclear)));
    }

    private AnswerRecord wrong(String errorType, String confidenceType) {
        AnswerRecord record = new AnswerRecord(); record.setCorrect(false); record.setErrorType(errorType);
        record.setConfidenceType(confidenceType); record.setDurationMs(70000L); record.setStandardTimeSecondsSnapshot(60);
        return record;
    }
}
