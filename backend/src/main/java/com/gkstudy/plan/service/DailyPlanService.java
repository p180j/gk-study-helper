package com.gkstudy.plan.service;

import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.engine.DailyPlanEngine;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.model.MaintenanceCandidate;
import com.gkstudy.priority.service.ProblemPriorityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailyPlanService {
    private final DailyPlanEngine engine;
    private final DailyPlanMapper planMapper;
    private final ProblemPriorityService priorityService;

    public DailyPlanService(DailyPlanEngine engine, DailyPlanMapper planMapper, ProblemPriorityService priorityService) {
        this.engine = engine; this.planMapper = planMapper; this.priorityService = priorityService;
    }

    @Transactional
    public DailyPlan today(Long userId) {
        LocalDate today = LocalDate.now(); DailyPlan existing = planMapper.findForUpdate(userId, today);
        if (existing != null) return loadItems(existing);
        return generate(userId, today, 45, false);
    }

    @Transactional
    public DailyPlan generate(Long userId, int plannedMinutes) {
        return generate(userId, LocalDate.now(), plannedMinutes, true);
    }

    DailyPlan generate(Long userId, LocalDate planDate, int plannedMinutes, boolean force) {
        DailyPlan existing = planMapper.findForUpdate(userId, planDate);
        if (existing != null && !force) return loadItems(existing);
        List<LearningProblem> coreProblems = priorityService.coreForPlan(userId);
        List<MaintenanceCandidate> candidates = new ArrayList<>(planMapper.findMaintenanceCandidates(userId));
        candidates.addAll(planMapper.findExplorationCandidates(userId));
        DailyPlan generated = engine.generate(userId, planDate, plannedMinutes, coreProblems, candidates);
        if (existing == null) {
            planMapper.insertPlan(generated);
        } else {
            generated.setId(existing.getId()); generated.setActualMinutes(existing.getActualMinutes());
            planMapper.deleteItems(existing.getId()); planMapper.updatePlan(generated);
        }
        for (DailyPlanItem item : generated.getItems()) { item.setPlanId(generated.getId()); planMapper.insertItem(item); }
        return generated;
    }

    private DailyPlan loadItems(DailyPlan plan) { plan.setItems(planMapper.findItems(plan.getId())); return plan; }
}
