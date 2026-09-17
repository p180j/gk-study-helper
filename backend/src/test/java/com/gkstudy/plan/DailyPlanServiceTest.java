package com.gkstudy.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.engine.DailyPlanEngine;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.model.MaintenanceCandidate;
import com.gkstudy.plan.service.DailyPlanService;
import com.gkstudy.priority.service.ProblemPriorityService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
        DailyPlanService service = new DailyPlanService(new DailyPlanEngine(), mapper, priorityService, new ObjectMapper());

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
        DailyPlanService service = new DailyPlanService(new DailyPlanEngine(), mapper, priorityService, new ObjectMapper());

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
        DailyPlanService service = new DailyPlanService(new DailyPlanEngine(), mapper, priorityService, new ObjectMapper());

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
        DailyPlanService service = new DailyPlanService(new DailyPlanEngine(), mapper, priorityService, new ObjectMapper());

        DailyPlan generated = service.generate(7L, 20);

        assertEquals(1, generated.getItems().size());
        DailyPlanItem item = generated.getItems().get(0);
        assertEquals("ESSAY", item.getItemType());
        assertEquals(60L, item.getKnowledgePointId());
        assertEquals("GRASSROOTS_GOVERNANCE", item.getKnowledgePointCode());
        assertTrue(item.getReason().contains("基层治理申论'归纳概括结构'问题"));
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
        DailyPlanService service = new DailyPlanService(new DailyPlanEngine(), mapper, priorityService, new ObjectMapper());

        DailyPlan generated = service.generate(7L, 20);

        DailyPlanItem item = generated.getItems().get(0);
        assertEquals("ESSAY", item.getItemType());
        assertEquals(6L, item.getKnowledgePointId());
        assertTrue(item.getReason().contains("申论'要点完整性'问题，安排申论专项训练"));
    }
}
