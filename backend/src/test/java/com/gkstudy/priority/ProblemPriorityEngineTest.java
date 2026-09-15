package com.gkstudy.priority;

import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.priority.engine.ProblemPriorityEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProblemPriorityEngineTest {
    private final ProblemPriorityEngine engine = new ProblemPriorityEngine();
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 15, 9, 0);

    @Test
    void weakestProblemIsNotNecessarilyHighestPriority() {
        LearningProblem permutation = problem(1, "排列组合", "CONFIRMED", 65, 0.65, 0.35, 0.30, 75);
        LearningProblem averageGrowth = problem(2, "年均增长率", "CONFIRMED", 48, 1.00, 0.90, 0.95, 70);
        List<LearningProblem> ranked = engine.rank(Arrays.asList(permutation, averageGrowth), now);
        assertEquals("年均增长率", ranked.get(0).getKnowledgePointName());
        assertTrue(ranked.get(0).getPriorityScore().compareTo(ranked.get(1).getPriorityScore()) > 0);
    }

    @Test
    void verifyingProblemGetsPriorityForValidation() {
        LearningProblem confirmed = problem(1, "年均增长率", "CONFIRMED", 55, 1.00, 0.90, 0.95, 70);
        LearningProblem verifying = problem(2, "逻辑判断", "VERIFYING", 40, 0.95, 0.75, 0.85, 70);
        assertEquals("VERIFYING", engine.rank(Arrays.asList(confirmed, verifying), now).get(0).getStatus());
    }

    @Test
    void lowConfidenceObservingProblemCannotTakeFirstPlace() {
        LearningProblem observing = problem(1, "定义判断", "OBSERVING", 90, 0.95, 0.80, 0.80, 15);
        LearningProblem confirmed = problem(2, "年均增长率", "CONFIRMED", 45, 1.00, 0.90, 0.95, 65);
        assertEquals("CONFIRMED", engine.rank(Arrays.asList(observing, confirmed), now).get(0).getStatus());
    }

    @Test
    void resolvedProblemsAreExcludedAndCoreIsLimitedToThree() {
        List<LearningProblem> problems = Arrays.asList(
                problem(1, "A", "CONFIRMED", 60, 1, .8, .8, 70), problem(2, "B", "PROCESSING", 55, .9, .8, .7, 70),
                problem(3, "C", "VERIFYING", 45, .8, .7, .6, 70), problem(4, "D", "OBSERVING", 80, 1, .9, .9, 20),
                problem(5, "E", "RESOLVED", 90, 1, 1, 1, 90));
        List<LearningProblem> core = engine.core(problems, now);
        assertEquals(3, core.size());
        assertTrue(core.stream().noneMatch(problem -> "RESOLVED".equals(problem.getStatus())));
    }

    @Test
    void resolvedTopProblemAllowsCandidateToFillCore() {
        LearningProblem resolved = problem(1, "年均增长率", "RESOLVED", 80, 1, .9, .95, 80);
        LearningProblem logic = problem(2, "逻辑判断", "VERIFYING", 45, .95, .75, .85, 70);
        LearningProblem permutation = problem(3, "排列组合", "CONFIRMED", 65, .65, .35, .3, 75);
        LearningProblem candidate = problem(4, "定义判断", "CONFIRMED", 42, .8, .7, .6, 60);
        List<LearningProblem> core = engine.core(Arrays.asList(resolved, logic, permutation, candidate), now);
        assertEquals(3, core.size()); assertTrue(core.contains(candidate)); assertFalse(core.contains(resolved));
    }

    private LearningProblem problem(long id, String name, String status, double severity, double importance,
                                    double improvement, double transfer, double confidence) {
        LearningProblem problem = new LearningProblem(); problem.setId(id); problem.setKnowledgePointId(id); problem.setKnowledgePointName(name);
        problem.setStatus(status); problem.setSeverity(BigDecimal.valueOf(severity)); problem.setExamImportance(BigDecimal.valueOf(importance));
        problem.setImprovementPotential(BigDecimal.valueOf(improvement)); problem.setTransferValue(BigDecimal.valueOf(transfer));
        problem.setAbilityConfidence(BigDecimal.valueOf(confidence)); problem.setDiscoveredTime(now.minusDays(10)); return problem;
    }
}
