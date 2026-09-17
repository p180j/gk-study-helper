package com.gkstudy.learningproblem;

import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.engine.LearningProblemEngine.Evaluation;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.practice.model.AnswerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LearningProblemEngineTest {
    private final LearningProblemEngine engine = new LearningProblemEngine();

    @Test
    void unassessedAbilityNeverCreatesProblem() {
        AbilityProfile profile = profile(0, 0, 0, 0, 0); profile.setStatus("UNASSESSED");
        assertNull(engine.evaluate("MASTERY", null, profile, Collections.emptyList()));
        assertNull(engine.evaluate("SPEED", null, profile, Collections.emptyList()));
        assertNull(engine.evaluate("STABILITY", null, profile, Collections.emptyList()));
    }

    @Test
    void insufficientSamplesStayObserving() {
        Evaluation result = engine.evaluate(LearningProblemEngine.MASTERY, null, profile(1, 35, 55, 55, 10), records(false));
        assertEquals("OBSERVING", result.getStatus());
    }

    @Test
    void persistentLowMasteryBecomesConfirmed() {
        Evaluation result = engine.evaluate(LearningProblemEngine.MASTERY, null, profile(4, 40, 55, 50, 35), records(false, false, true, false));
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(3, result.getIncorrectCount());
    }

    @Test
    void persistentSlowAnswersCreateSpeedProblem() {
        Evaluation result = engine.evaluate(LearningProblemEngine.SPEED, null, profile(4, 55, 35, 55, 35), slowRecords(4));
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(4, result.getSlowCount());
    }

    @Test
    void alternatingAnswersCreateStabilityProblem() {
        Evaluation result = engine.evaluate(LearningProblemEngine.STABILITY, null, profile(4, 55, 55, 35, 35), records(true, false, true, false));
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(3, result.getTransitionCount());
    }

    @Test
    void improvementMovesThroughProcessingAndVerifying() {
        LearningProblem problem = problem("CONFIRMED", 0, 0);
        Evaluation processing = engine.evaluate(LearningProblemEngine.MASTERY, problem, profile(6, 47, 55, 55, 40), records(false, false, true, true));
        assertEquals("PROCESSING", processing.getStatus());

        problem.setStatus("PROCESSING");
        Evaluation verifying = engine.evaluate(LearningProblemEngine.MASTERY, problem, profile(8, 55, 60, 65, 45), records(false, true, true, true, true));
        assertEquals("VERIFYING", verifying.getStatus());
    }

    @Test
    void threeStableValidationPassesResolveProblem() {
        LearningProblem problem = problem("VERIFYING", 2, 2);
        List<AnswerRecord> records = records(true, true, true, true);
        records.get(records.size() - 1).setPracticeType("VALIDATION");
        Evaluation result = engine.evaluate(LearningProblemEngine.MASTERY, problem, profile(10, 65, 65, 70, 60), records);
        assertEquals("RESOLVED", result.getStatus());
        assertEquals(3, result.getValidationPassCount());
    }

    @Test
    void failedValidationResetsConsecutiveValidationPasses() {
        LearningProblem problem = problem("VERIFYING", 2, 2);
        List<AnswerRecord> records = records(true, true, true, false);
        records.get(records.size() - 1).setPracticeType("VALIDATION");
        Evaluation result = engine.evaluate(LearningProblemEngine.MASTERY, problem, profile(10, 58, 65, 60, 60), records);
        assertEquals("VERIFYING", result.getStatus());
        assertEquals(0, result.getValidationCount());
        assertEquals(0, result.getValidationPassCount());
    }

    @Test
    void resolvedProblemReopensAfterPersistentRegression() {
        LearningProblem problem = problem("RESOLVED", 3, 3);
        Evaluation result = engine.evaluate(LearningProblemEngine.MASTERY, problem, profile(14, 35, 55, 40, 70), records(false, false, true, false));
        assertEquals("REOPENED", result.getStatus());
    }

    @Test
    void lowAbilityConfidenceCannotConfirmProblem() {
        Evaluation result = engine.evaluate(LearningProblemEngine.MASTERY, null, profile(8, 35, 55, 45, 20), records(false, false, true, false));
        assertEquals("OBSERVING", result.getStatus());
    }

    private AbilityProfile profile(int samples, double mastery, double speed, double stability, double confidence) {
        AbilityProfile profile = new AbilityProfile(); profile.setSampleCount(samples); profile.setMasteryScore(BigDecimal.valueOf(mastery));
        profile.setSpeedScore(BigDecimal.valueOf(speed)); profile.setStabilityScore(BigDecimal.valueOf(stability)); profile.setConfidenceScore(BigDecimal.valueOf(confidence));
        return profile;
    }

    private LearningProblem problem(String status, int validationCount, int validationPassCount) {
        LearningProblem problem = new LearningProblem(); problem.setStatus(status); problem.setValidationCount(validationCount); problem.setValidationPassCount(validationPassCount);
        return problem;
    }

    private List<AnswerRecord> records(Boolean... correctness) {
        List<AnswerRecord> records = new java.util.ArrayList<>();
        for (int index = 0; index < correctness.length; index++) records.add(record(correctness[index], 60000, "DAILY", index));
        return records;
    }

    private List<AnswerRecord> slowRecords(int count) {
        AnswerRecord[] records = new AnswerRecord[count];
        for (int index = 0; index < count; index++) records[index] = record(true, 120000, "DAILY", index);
        return Arrays.asList(records);
    }

    private AnswerRecord record(boolean correct, long duration, String practiceType, int index) {
        AnswerRecord record = new AnswerRecord(); record.setCorrect(correct); record.setDurationMs(duration); record.setStandardTimeSecondsSnapshot(60);
        record.setPracticeType(practiceType); record.setAnswerTime(LocalDateTime.of(2026, 1, 1, 9, 0).plusMinutes(index)); return record;
    }
}
