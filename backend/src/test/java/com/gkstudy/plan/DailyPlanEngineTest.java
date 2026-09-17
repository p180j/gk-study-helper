package com.gkstudy.plan;

import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.engine.DailyPlanEngine;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
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

    @Test
    void newUserExplorationCandidateProducesAssessmentTask() {
        MaintenanceCandidate candidate = maintenance(4, "年均增长率"); candidate.setPurpose("ASSESSMENT");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 20, Collections.emptyList(), Collections.singletonList(candidate));

        assertEquals("QUESTION_SET", plan.getItems().get(0).getItemType());
        assertEquals("ASSESSMENT", plan.getItems().get(0).getPurpose());
        assertTrue(plan.getItems().get(0).getReason().contains("建立真实能力基线"));
    }

    @Test
    void realProblemNaturallyReplacesAssessmentWithTraining() {
        MaintenanceCandidate assessment = maintenance(4, "年均增长率"); assessment.setPurpose("ASSESSMENT");
        DailyPlan first = engine.generate(7L, LocalDate.now(), 20, Collections.emptyList(), Collections.singletonList(assessment));
        LearningProblem confirmed = problem(4, "年均增长率", "CONFIRMED");

        DailyPlan later = engine.generate(7L, LocalDate.now().plusDays(1), 20, Collections.singletonList(confirmed), Collections.emptyList());

        assertEquals("ASSESSMENT", first.getItems().get(0).getPurpose());
        assertEquals("TRAINING", later.getItems().get(0).getPurpose());
    }

    @Test
    void contentGapProblemProducesReadingItemOnThemeKnowledgePoint() {
        LearningProblem problem = problem(5, "基层治理", "CONFIRMED"); problem.setProblemType("CONTENT_GAP");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 20, Collections.singletonList(problem), Collections.emptyList());

        DailyPlanItem item = plan.getItems().get(0);
        assertEquals("READING", item.getItemType());
        assertEquals(5L, item.getKnowledgePointId());
        assertEquals("TRAINING", item.getPurpose());
        assertTrue(item.getReason().contains("《基层治理》主题申论表现弱且政治阅读覆盖不足，安排主题阅读补充素材"));
        assertEquals(20, item.getPlannedMinutes());
    }

    @Test
    void essayProblemProducesEssayItemOnTopicKnowledgePoint() {
        LearningProblem problem = problem(6, "归纳概括", "CONFIRMED"); problem.setProblemType("ESSAY_STRUCTURE");
        problem.setTopicKnowledgePointId(60L); problem.setTopicCode("GRASSROOTS_GOVERNANCE"); problem.setTopicName("基层治理");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 20, Collections.singletonList(problem), Collections.emptyList());

        DailyPlanItem item = plan.getItems().get(0);
        assertEquals("ESSAY", item.getItemType());
        assertEquals(60L, item.getKnowledgePointId());
        assertEquals("TRAINING", item.getPurpose());
        assertTrue(item.getReason().contains("基层治理申论'归纳概括结构'问题，安排申论专项训练"));
        assertEquals(20, item.getPlannedMinutes());
    }

    @Test
    void verifyingEssayProblemProducesValidationEssayItem() {
        LearningProblem problem = problem(6, "归纳概括", "VERIFYING"); problem.setProblemType("ESSAY_EXPRESSION");
        problem.setTopicKnowledgePointId(60L);

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 20, Collections.singletonList(problem), Collections.emptyList());

        DailyPlanItem item = plan.getItems().get(0);
        assertEquals("ESSAY", item.getItemType());
        assertEquals("VALIDATION", item.getPurpose());
        assertEquals(60L, item.getKnowledgePointId());
    }

    @Test
    void essayProblemWithoutTopicFallsBackToProblemKnowledgePoint() {
        LearningProblem problem = problem(6, "要点完整性", "CONFIRMED"); problem.setProblemType("ESSAY_MISSING_POINTS");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 20, Collections.singletonList(problem), Collections.emptyList());

        DailyPlanItem item = plan.getItems().get(0);
        assertEquals("ESSAY", item.getItemType());
        assertEquals(6L, item.getKnowledgePointId());
        assertTrue(item.getReason().contains("申论'要点完整性'问题，安排申论专项训练"));
    }

    @Test
    void sameTopicAllowsEssayAndReadingItemsInOnePlan() {
        LearningProblem essayProblem = problem(6, "归纳概括", "CONFIRMED"); essayProblem.setProblemType("ESSAY_STRUCTURE");
        essayProblem.setTopicKnowledgePointId(60L); essayProblem.setTopicName("基层治理");
        LearningProblem contentGap = problem(60, "基层治理", "CONFIRMED"); contentGap.setProblemType("CONTENT_GAP");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 45, Arrays.asList(essayProblem, contentGap), Collections.emptyList());

        assertEquals(2, plan.getItems().size());
        assertTrue(plan.getItems().stream().anyMatch(item -> "ESSAY".equals(item.getItemType()) && Long.valueOf(60L).equals(item.getKnowledgePointId())));
        assertTrue(plan.getItems().stream().anyMatch(item -> "READING".equals(item.getItemType()) && Long.valueOf(60L).equals(item.getKnowledgePointId())));
        assertEquals(27, plan.getItems().get(0).getPlannedMinutes());
        assertEquals(18, plan.getItems().get(1).getPlannedMinutes());
        assertEquals(45, plan.getItems().stream().mapToInt(DailyPlanItem::getPlannedMinutes).sum());
    }

    @Test
    void contentGapReadingTaskExceedsRegularItemLimitAsCompanionTask() {
        LearningProblem first = problem(1, "年均增长率", "CONFIRMED");
        LearningProblem second = problem(2, "逻辑判断", "CONFIRMED");
        LearningProblem third = problem(3, "定义判断", "CONFIRMED");
        LearningProblem gap = problem(60, "基层治理", "CONFIRMED"); gap.setProblemType("CONTENT_GAP");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 45, Arrays.asList(first, second, third, gap), Collections.emptyList());

        // 45 分钟常规上限 3 个任务，CONTENT_GAP 配套阅读任务允许第 4 个出现，保证申论-阅读联动
        assertEquals(4, plan.getItems().size());
        assertEquals("READING", plan.getItems().get(3).getItemType());
        assertEquals(60L, plan.getItems().get(3).getKnowledgePointId());
        assertEquals(45, plan.getItems().stream().mapToInt(DailyPlanItem::getPlannedMinutes).sum());
    }

    @Test
    void fixedOnlyItemsSplitRemainderProportionally() {
        LearningProblem essayProblem = problem(6, "归纳概括", "CONFIRMED"); essayProblem.setProblemType("ESSAY_STRUCTURE");
        essayProblem.setTopicKnowledgePointId(60L);
        LearningProblem gap = problem(60, "基层治理", "CONFIRMED"); gap.setProblemType("CONTENT_GAP");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 45, Arrays.asList(essayProblem, gap), Collections.emptyList());

        // 固定 15+10=25，剩余 20 按默认分钟 15:10 比例分配：ESSAY +12、READING +8
        assertEquals(27, plan.getItems().get(0).getPlannedMinutes());
        assertEquals(18, plan.getItems().get(1).getPlannedMinutes());
        assertEquals(45, plan.getItems().stream().mapToInt(DailyPlanItem::getPlannedMinutes).sum());
    }

    @Test
    void mixedItemsGiveFixedMinutesToEssayAndSplitRemainderEvenly() {
        LearningProblem essayProblem = problem(6, "归纳概括", "CONFIRMED"); essayProblem.setProblemType("ESSAY_STRUCTURE");
        essayProblem.setTopicKnowledgePointId(60L); essayProblem.setTopicName("基层治理");
        LearningProblem mastery = problem(1, "年均增长率", "CONFIRMED");

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 45, Arrays.asList(essayProblem, mastery),
                Collections.singletonList(maintenance(3, "资料分析")));

        assertEquals(3, plan.getItems().size());
        DailyPlanItem essayItem = plan.getItems().get(0);
        assertEquals("ESSAY", essayItem.getItemType());
        assertEquals(15, essayItem.getPlannedMinutes());
        assertEquals(15, plan.getItems().get(1).getPlannedMinutes());
        assertEquals(15, plan.getItems().get(2).getPlannedMinutes());
        assertEquals(45, plan.getItems().stream().mapToInt(DailyPlanItem::getPlannedMinutes).sum());
    }

    @Test
    void duplicateProblemTypeOnSameKnowledgePointIsDeduplicated() {
        LearningProblem first = problem(6, "归纳概括", "CONFIRMED"); first.setProblemType("ESSAY_STRUCTURE");
        first.setTopicKnowledgePointId(60L);
        LearningProblem second = problem(6, "归纳概括", "PROCESSING"); second.setProblemType("ESSAY_EXPRESSION");
        second.setTopicKnowledgePointId(60L);

        DailyPlan plan = engine.generate(7L, LocalDate.now(), 45, Arrays.asList(first, second), Collections.emptyList());

        assertEquals(1, plan.getItems().size());
        assertEquals("ESSAY", plan.getItems().get(0).getItemType());
        assertEquals(45, plan.getItems().stream().mapToInt(DailyPlanItem::getPlannedMinutes).sum());
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
