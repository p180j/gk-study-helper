package com.gkstudy.essay;

import com.gkstudy.essay.engine.EssayProblemEngine;
import com.gkstudy.learningproblem.model.LearningProblem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EssayProblemEngineTest {
    private final EssayProblemEngine engine = new EssayProblemEngine();

    @Test
    void firstLowScoreIsObserving() {
        EssayProblemEngine.Evaluation evaluation = engine.evaluate("ESSAY_MISSING_POINTS", null, 30.0, 1, "{}");
        assertEquals("OBSERVING", evaluation.getStatus());
        assertEquals(0, evaluation.getValidationCount());
        assertEquals(0, evaluation.getValidationPassCount());
    }

    @Test
    void secondLowScoreIsConfirmed() {
        assertEquals("CONFIRMED", engine.evaluate("ESSAY_MISSING_POINTS", null, 30.0, 2, "{}").getStatus());
        LearningProblem observing = problem("OBSERVING", 0, 0);
        assertEquals("CONFIRMED", engine.evaluate("ESSAY_MISSING_POINTS", observing, 30.0, 2, "{}").getStatus());
    }

    @Test
    void scoreAtOrAbove55MovesToVerifying() {
        LearningProblem confirmed = problem("CONFIRMED", 1, 0);
        EssayProblemEngine.Evaluation evaluation = engine.evaluate("ESSAY_MISSING_POINTS", confirmed, 56.0, 3, "{}");
        assertEquals("VERIFYING", evaluation.getStatus());
        assertEquals(2, evaluation.getValidationCount());
        assertEquals(0, evaluation.getValidationPassCount());
    }

    @Test
    void twoConsecutivePassesResolve() {
        EssayProblemEngine.Evaluation first = engine.evaluate("ESSAY_MISSING_POINTS", problem("CONFIRMED", 0, 0), 65.0, 3, "{}");
        assertEquals("VERIFYING", first.getStatus());
        assertEquals(1, first.getValidationPassCount());
        EssayProblemEngine.Evaluation second = engine.evaluate("ESSAY_MISSING_POINTS", problem("VERIFYING", 1, 1), 65.0, 3, "{}");
        assertEquals("RESOLVED", second.getStatus());
        assertEquals(2, second.getValidationPassCount());
    }

    @Test
    void lowScoreAfterResolvedReopens() {
        EssayProblemEngine.Evaluation evaluation = engine.evaluate("ESSAY_MISSING_POINTS", problem("RESOLVED", 3, 2), 30.0, 5, "{}");
        assertEquals("REOPENED", evaluation.getStatus());
    }

    @Test
    void midRangeScoreCreatesNothingOrKeepsStatus() {
        assertNull(engine.evaluate("ESSAY_MISSING_POINTS", null, 50.0, 2, "{}"));
        assertEquals("CONFIRMED", engine.evaluate("ESSAY_MISSING_POINTS", problem("CONFIRMED", 2, 0), 50.0, 4, "{}").getStatus());
    }

    private LearningProblem problem(String status, int validationCount, int validationPassCount) {
        LearningProblem problem = new LearningProblem();
        problem.setId(88L); problem.setUserId(7L); problem.setKnowledgePointId(21L);
        problem.setProblemType("ESSAY_MISSING_POINTS"); problem.setStatus(status);
        problem.setValidationCount(validationCount); problem.setValidationPassCount(validationPassCount);
        return problem;
    }
}
