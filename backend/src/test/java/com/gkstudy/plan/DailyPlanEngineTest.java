package com.gkstudy.plan;

import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.engine.DailyPlanEngine;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.MaintenanceCandidate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DailyPlanEngineTest {
    private final DailyPlanEngine engine = new DailyPlanEngine();

    @Test
    void supportsTwentyFortyFiveSixtyAndNinetyMinutePlans() {
        List<LearningProblem> problems = Arrays.asList(problem(1, "A", "CONFIRMED"), problem(2, "B", "VERIFYING"), problem(3, "C", "PROCESSING"));
        List<MaintenanceCandidate> maintenance = Collections.singletonList(maintenance(4, "D"));
        int[] minutes = {20, 45, 60, 90}; int[] counts = {1, 3, 3, 4};
        for (int index = 0; index < minutes.length; index++) {
            DailyPlan plan = engine.generate(7L, LocalDate.of(2026, 9, 15), minutes[index], problems, maintenance);
            assertEquals(counts[index], plan.getItems().size());
            assertEquals(minutes[index], plan.getItems().stream().mapToInt(item -> item.getPlannedMinutes()).sum());
        }
    }

    @Test
    void verifyingProblemProducesFirstValidationTask() {
        DailyPlan plan = engine.generate(7L, LocalDate.now(), 20,
                Arrays.asList(problem(1, "年均增长率", "CONFIRMED"), problem(2, "逻辑判断", "VERIFYING")), Collections.emptyList());
        assertEquals(2L, plan.getItems().get(0).getLearningProblemId());
        assertEquals("VALIDATION", plan.getItems().get(0).getPurpose());
        assertEquals("QUESTION_SET", plan.getItems().get(0).getItemType());
    }

    @Test
    void generationReasonExplainsProblemAndMaintenanceTasks() {
        DailyPlan plan = engine.generate(7L, LocalDate.now(), 45,
                Arrays.asList(problem(1, "年均增长率", "CONFIRMED"), problem(2, "逻辑判断", "VERIFYING")),
                Collections.singletonList(maintenance(3, "资料分析")));
        assertTrue(plan.getGenerationReason().contains("优先处理年均增长率"));
        assertTrue(plan.getGenerationReason().contains("验证逻辑判断"));
        assertTrue(plan.getGenerationReason().contains("保持资料分析"));
        assertEquals(3, plan.getItems().stream().map(item -> item.getKnowledgePointId()).distinct().count());
    }

    @Test
    void rejectsUnsupportedMinutes() {
        assertThrows(IllegalArgumentException.class, () -> engine.generate(7L, LocalDate.now(), 30, Collections.emptyList(), Collections.emptyList()));
    }

    private LearningProblem problem(long id, String name, String status) {
        LearningProblem problem = new LearningProblem(); problem.setId(id); problem.setKnowledgePointId(id); problem.setKnowledgePointCode("K" + id);
        problem.setKnowledgePointName(name); problem.setProblemType(id == 2 ? "STABILITY" : "MASTERY"); problem.setStatus(status); return problem;
    }

    private MaintenanceCandidate maintenance(long id, String name) {
        MaintenanceCandidate candidate = new MaintenanceCandidate(); candidate.setKnowledgePointId(id); candidate.setKnowledgePointCode("K" + id);
        candidate.setKnowledgePointName(name); return candidate;
    }
}
