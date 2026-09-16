package com.gkstudy.priority;

import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.priority.engine.ProblemPriorityEngine;
import com.gkstudy.priority.service.ProblemPriorityService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ProblemPriorityServiceTest {
    private LearningProblem problem(long id, String type, String status, double priority) {
        LearningProblem problem = new LearningProblem(); problem.setId(id); problem.setKnowledgePointId(id);
        problem.setProblemType(type); problem.setStatus(status); problem.setPriorityScore(java.math.BigDecimal.valueOf(priority));
        problem.setSeverity(java.math.BigDecimal.valueOf(50)); problem.setDiscoveredTime(java.time.LocalDateTime.now());
        return problem;
    }

    private ProblemPriorityService service(LearningProblemMapper mapper, List<LearningProblem> candidates) {
        ProblemPriorityEngine engine = mock(ProblemPriorityEngine.class);
        when(mapper.findPriorityCandidates(anyLong())).thenReturn(candidates);
        when(engine.rank(anyList(), any())).thenReturn(candidates);
        return new ProblemPriorityService(engine, mapper);
    }

    @Test
    void coreForPlanWithReadingGapAppendsHighestPriorityUnresolvedContentGap() {
        LearningProblemMapper mapper = mock(LearningProblemMapper.class);
        List<LearningProblem> ranked = Arrays.asList(
                problem(1, "ESSAY_MISSING_POINTS", "CONFIRMED", 90),
                problem(2, "ESSAY_STRUCTURE", "CONFIRMED", 80),
                problem(3, "ESSAY_EXPRESSION", "CONFIRMED", 70),
                problem(4, "CONTENT_GAP", "CONFIRMED", 60));
        ProblemPriorityService service = service(mapper, ranked);

        List<LearningProblem> core = service.coreForPlanWithReadingGap(7L);

        assertEquals(4, core.size());
        assertEquals("CONTENT_GAP", core.get(3).getProblemType());
        verify(mapper, times(4)).updatePriority(anyLong(), any(java.math.BigDecimal.class));
    }

    @Test
    void coreForPlanWithReadingGapSkipsResolvedAndAlreadyIncludedGap() {
        LearningProblemMapper mapper = mock(LearningProblemMapper.class);
        List<LearningProblem> ranked = Arrays.asList(
                problem(1, "CONTENT_GAP", "RESOLVED", 90),
                problem(2, "MASTERY", "CONFIRMED", 80));
        ProblemPriorityService service = service(mapper, ranked);

        List<LearningProblem> core = service.coreForPlanWithReadingGap(7L);

        // RESOLVED 的 CONTENT_GAP 不追加也不进入核心
        assertEquals(1, core.size());
        assertEquals("MASTERY", core.get(0).getProblemType());
    }

    @Test
    void coreForPlanWithReadingGapKeepsThreeWhenGapAlreadyInCore() {
        LearningProblemMapper mapper = mock(LearningProblemMapper.class);
        List<LearningProblem> ranked = Arrays.asList(
                problem(1, "CONTENT_GAP", "CONFIRMED", 90),
                problem(2, "ESSAY_STRUCTURE", "CONFIRMED", 80),
                problem(3, "ESSAY_EXPRESSION", "CONFIRMED", 70));
        ProblemPriorityService service = service(mapper, ranked);

        List<LearningProblem> core = service.coreForPlanWithReadingGap(7L);

        assertEquals(3, core.size());
        assertTrue(core.stream().anyMatch(problem -> LearningProblemEngine.CONTENT_GAP.equals(problem.getProblemType())));
    }

    @Test
    void coreForPlanWithReadingGapWithoutGapReturnsCoreOnly() {
        LearningProblemMapper mapper = mock(LearningProblemMapper.class);
        List<LearningProblem> ranked = Collections.singletonList(problem(1, "MASTERY", "CONFIRMED", 80));
        ProblemPriorityService service = service(mapper, ranked);

        assertEquals(1, service.coreForPlanWithReadingGap(7L).size());
    }
}
