package com.gkstudy.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.engine.DailyPlanEngine;
import com.gkstudy.plan.engine.QuestionTaskPolicy;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.model.MaintenanceCandidate;
import com.gkstudy.plan.service.DailyPlanService;
import com.gkstudy.priority.service.ProblemPriorityService;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.service.QuestionInventoryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DailyPlanServiceTest {
    @Test
    void todayRebuildsInvalidEmptyPlan() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        DailyPlan existing = new DailyPlan(); existing.setId(9L); existing.setUserId(7L); existing.setPlanDate(LocalDate.now()); existing.setPlannedMinutes(45);
        MaintenanceCandidate candidate = new MaintenanceCandidate(); candidate.setKnowledgePointId(12L); candidate.setKnowledgePointCode("AVG_GROWTH_RATE");
        candidate.setKnowledgePointName("年均增长率"); candidate.setPurpose("ASSESSMENT");
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(existing); when(mapper.findItems(9L)).thenReturn(Collections.emptyList());
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.emptyList());
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.singletonList(candidate));
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), mock(QuestionInventoryService.class));

        DailyPlan rebuilt = service.today(7L);

        assertEquals(9L, rebuilt.getId()); assertFalse(rebuilt.getItems().isEmpty()); assertEquals("ASSESSMENT", rebuilt.getItems().get(0).getPurpose());
        verify(mapper).deleteItems(9L); verify(mapper).updatePlan(rebuilt); verify(mapper).insertItem(rebuilt.getItems().get(0));
    }

    @Test
    void repeatedTodayReturnsExistingPlanWithoutRegeneration() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        DailyPlan existing = new DailyPlan(); existing.setId(9L); existing.setUserId(7L); existing.setPlanDate(LocalDate.now()); existing.setPlannedMinutes(45);
        DailyPlanItem item = new DailyPlanItem(); item.setId(10L);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(existing); when(mapper.findItems(9L)).thenReturn(Collections.singletonList(item));
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), mock(QuestionInventoryService.class));

        DailyPlan first = service.today(7L); DailyPlan second = service.today(7L);

        assertSame(existing, first); assertSame(existing, second); assertEquals(10L, first.getItems().get(0).getId());
        verify(mapper, times(2)).findItems(9L); verifyNoInteractions(priorityService); verify(mapper, never()).insertPlan(any());
    }

    @Test
    void explicitGenerateRebuildsExistingPlan() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        DailyPlan existing = new DailyPlan(); existing.setId(9L); existing.setActualMinutes(0);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(existing);
        LearningProblem problem = new LearningProblem(); problem.setId(1L); problem.setKnowledgePointId(12L); problem.setKnowledgePointName("年均增长率");
        problem.setKnowledgePointCode("AVG_GROWTH_RATE"); problem.setProblemType("MASTERY"); problem.setStatus("CONFIRMED");
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.singletonList(problem)); when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.emptyList());
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), mock(QuestionInventoryService.class));

        DailyPlan generated = service.generate(7L, 20);

        assertEquals(9L, generated.getId()); assertEquals(20, generated.getPlannedMinutes()); assertEquals(1, generated.getItems().size());
        verify(mapper).deleteItems(9L); verify(mapper).updatePlan(generated); verify(mapper).insertItem(generated.getItems().get(0));
    }

    @Test
    void essayCoreProblemEvidenceFillsTopicKnowledgePoint() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(null);
        LearningProblem problem = new LearningProblem(); problem.setId(1L); problem.setKnowledgePointId(6L);
        problem.setKnowledgePointCode("ESSAY_SUMMARY"); problem.setKnowledgePointName("归纳概括");
        problem.setProblemType("ESSAY_STRUCTURE"); problem.setStatus("CONFIRMED");
        problem.setEvidenceJson("{\"topicKnowledgePointId\":60,\"topicCode\":\"GRASSROOTS_GOVERNANCE\",\"topicName\":\"基层治理\"}");
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.singletonList(problem));
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.emptyList());
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), mock(QuestionInventoryService.class));

        DailyPlan generated = service.generate(7L, 20);

        assertEquals(1, generated.getItems().size());
        DailyPlanItem item = generated.getItems().get(0);
        assertEquals("ESSAY", item.getItemType());
        assertEquals(60L, item.getKnowledgePointId());
        assertEquals("GRASSROOTS_GOVERNANCE", item.getKnowledgePointCode());
        assertTrue(item.getReason().contains("基层治理申论'归纳概括结构'问题"));
        assertNull(item.getTargetQuestionCount()); assertEquals(0, item.getCompletedQuestionCount());
        verify(mapper).insertPlan(generated); verify(mapper).insertItem(item);
    }

    @Test
    void essayCoreProblemFallsBackToDimensionKnowledgePointOnBrokenEvidence() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(null);
        LearningProblem problem = new LearningProblem(); problem.setId(1L); problem.setKnowledgePointId(6L);
        problem.setKnowledgePointCode("ESSAY_POINT_COMPLETENESS"); problem.setKnowledgePointName("要点完整性");
        problem.setProblemType("ESSAY_MISSING_POINTS"); problem.setStatus("CONFIRMED");
        problem.setEvidenceJson("{broken-json");
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.singletonList(problem));
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.emptyList());
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), mock(QuestionInventoryService.class));

        DailyPlan generated = service.generate(7L, 20);

        DailyPlanItem item = generated.getItems().get(0);
        assertEquals("ESSAY", item.getItemType());
        assertEquals(6L, item.getKnowledgePointId());
        assertTrue(item.getReason().contains("申论'要点完整性'问题，安排申论专项训练"));
    }

    @Test
    void filtersAssessmentCandidatesWithInsufficientStockAndKeepsSufficientOnes() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        QuestionInventoryService inventoryService = mock(QuestionInventoryService.class);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(null);
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.emptyList());
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        MaintenanceCandidate insufficient = assessmentCandidate(21L, "逻辑判断");
        MaintenanceCandidate sufficient = assessmentCandidate(22L, "年均增长率");
        when(mapper.findExplorationCandidates(7L)).thenReturn(Arrays.asList(insufficient, sufficient));
        when(inventoryService.countAvailable(21L, "ASSESSMENT", 7L)).thenReturn(4);
        when(inventoryService.countAvailable(22L, "ASSESSMENT", 7L)).thenReturn(5);
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), inventoryService);

        DailyPlan generated = service.generate(7L, 20);

        // 21 库存 4 < ASSESSMENT_MIN 5 被过滤，22 库存充足保留
        assertEquals(1, generated.getItems().size());
        assertEquals(22L, generated.getItems().get(0).getKnowledgePointId());
        verify(inventoryService, times(1)).countAvailable(eq(21L), eq("ASSESSMENT"), any());
    }

    @Test
    void keepsAllExplorationCandidatesWhenEveryAssessmentStockIsInsufficient() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        QuestionInventoryService inventoryService = mock(QuestionInventoryService.class);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(null);
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.emptyList());
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        MaintenanceCandidate candidate = assessmentCandidate(21L, "逻辑判断");
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.singletonList(candidate));
        when(inventoryService.countAvailable(21L, "ASSESSMENT", 7L)).thenReturn(2);
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), inventoryService);

        DailyPlan generated = service.generate(7L, 20);

        // 全部摸底候选库存不足时不过滤，生成 INSUFFICIENT_STOCK 任务供用户感知
        assertEquals(1, generated.getItems().size());
        DailyPlanItem item = generated.getItems().get(0);
        assertEquals("INSUFFICIENT_STOCK", item.getStatus());
        assertEquals(21L, item.getKnowledgePointId());
    }

    @Test
    void insufficientModuleStockKeepsFullTargetAndMarksItem() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        QuestionInventoryService inventoryService = mock(QuestionInventoryService.class);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(null);
        LearningProblem problem = new LearningProblem(); problem.setId(1L); problem.setKnowledgePointId(12L); problem.setKnowledgePointName("年均增长率");
        problem.setKnowledgePointCode("AVG_GROWTH_RATE"); problem.setProblemType("MASTERY"); problem.setStatus("CONFIRMED");
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.singletonList(problem));
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.emptyList());
        when(inventoryService.countAvailable(12L, "TRAINING", 7L)).thenReturn(1);
        DailyPlanService service = service(mapper, priorityService, mock(QuestionMapper.class), inventoryService);

        DailyPlan generated = service.generate(7L, 20);

        DailyPlanItem item = generated.getItems().get(0);
        // 库存 1 < TRAINING_MIN 3：目标题量不缩水（20分钟/默认60秒 → 夹到 TRAINING_MAX 12），状态 INSUFFICIENT_STOCK
        assertEquals("INSUFFICIENT_STOCK", item.getStatus());
        assertEquals(12, item.getTargetQuestionCount());
        assertEquals(0, item.getCompletedQuestionCount());
        assertTrue(item.getReason().contains("（当前可用题目不足，暂无法完成本组训练）"));
    }

    @Test
    void targetQuestionCountIsCappedByAvailableStockWhenStockSufficientButSmaller() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class); QuestionInventoryService inventoryService = mock(QuestionInventoryService.class);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(null);
        LearningProblem problem = new LearningProblem(); problem.setId(1L); problem.setKnowledgePointId(12L); problem.setKnowledgePointName("年均增长率");
        problem.setKnowledgePointCode("AVG_GROWTH_RATE"); problem.setProblemType("MASTERY"); problem.setStatus("CONFIRMED");
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.singletonList(problem));
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.emptyList());
        when(questionMapper.avgStandardSeconds(12L)).thenReturn(60);
        when(inventoryService.countAvailable(12L, "TRAINING", 7L)).thenReturn(8);
        DailyPlanService service = service(mapper, priorityService, questionMapper, inventoryService);

        DailyPlan generated = service.generate(7L, 20);

        // 策略值 12 但可用库存 8（≥ TRAINING_MIN 3）→ 目标收敛到 8，状态保持 PENDING
        DailyPlanItem item = generated.getItems().get(0);
        assertEquals(8, item.getTargetQuestionCount());
        assertEquals("PENDING", item.getStatus());
    }

    @Test
    void assessmentTargetUsesAvgStandardSeconds() {
        DailyPlanMapper mapper = mock(DailyPlanMapper.class); ProblemPriorityService priorityService = mock(ProblemPriorityService.class);
        QuestionMapper questionMapper = mock(QuestionMapper.class); QuestionInventoryService inventoryService = mock(QuestionInventoryService.class);
        when(mapper.findForUpdate(7L, LocalDate.now())).thenReturn(null);
        when(priorityService.coreForPlanWithReadingGap(7L)).thenReturn(Collections.emptyList());
        when(mapper.findMaintenanceCandidates(7L)).thenReturn(Collections.emptyList());
        MaintenanceCandidate candidate = assessmentCandidate(22L, "年均增长率");
        when(mapper.findExplorationCandidates(7L)).thenReturn(Collections.singletonList(candidate));
        when(inventoryService.countAvailable(22L, "ASSESSMENT", 7L)).thenReturn(10);
        // 20分钟=1200秒，平均每题200秒 → base 6，摸底任务固定为一组 5 题。
        when(questionMapper.avgStandardSeconds(22L)).thenReturn(200);
        DailyPlanService service = service(mapper, priorityService, questionMapper, inventoryService);

        DailyPlan generated = service.generate(7L, 20);

        assertEquals(5, generated.getItems().get(0).getTargetQuestionCount());
        assertEquals("PENDING", generated.getItems().get(0).getStatus());
    }

    private DailyPlanService service(DailyPlanMapper mapper, ProblemPriorityService priorityService,
                                     QuestionMapper questionMapper, QuestionInventoryService inventoryService) {
        return new DailyPlanService(new DailyPlanEngine(), mapper, priorityService, new ObjectMapper(),
                questionMapper, inventoryService, new QuestionTaskPolicy());
    }

    private MaintenanceCandidate assessmentCandidate(Long knowledgePointId, String name) {
        MaintenanceCandidate candidate = new MaintenanceCandidate(); candidate.setKnowledgePointId(knowledgePointId);
        candidate.setKnowledgePointCode("K" + knowledgePointId); candidate.setKnowledgePointName(name);
        candidate.setPurpose("ASSESSMENT"); return candidate;
    }
}
