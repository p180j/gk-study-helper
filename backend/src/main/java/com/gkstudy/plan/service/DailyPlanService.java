package com.gkstudy.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.learningproblem.engine.LearningProblemEngine;
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
    private final ObjectMapper objectMapper;

    public DailyPlanService(DailyPlanEngine engine, DailyPlanMapper planMapper, ProblemPriorityService priorityService,
                            ObjectMapper objectMapper) {
        this.engine = engine; this.planMapper = planMapper; this.priorityService = priorityService; this.objectMapper = objectMapper;
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
        List<LearningProblem> coreProblems = priorityService.coreForPlanWithReadingGap(userId);
        enrichEssayTopicInfo(coreProblems);
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

    /** ESSAY_* 问题挂在申论维度知识点上，从 evidence_json 提取主题知识点供计划引擎生成申论训练项；解析失败回退到问题自身知识点。 */
    private void enrichEssayTopicInfo(List<LearningProblem> problems) {
        for (LearningProblem problem : problems) {
            if (!LearningProblemEngine.isEssayDimensionProblem(problem.getProblemType())) continue;
            applyTopicEvidence(problem);
        }
    }

    private void applyTopicEvidence(LearningProblem problem) {
        if (problem.getEvidenceJson() != null) {
            try {
                JsonNode evidence = objectMapper.readTree(problem.getEvidenceJson());
                if (evidence.hasNonNull("topicKnowledgePointId")) problem.setTopicKnowledgePointId(evidence.get("topicKnowledgePointId").asLong());
                if (evidence.hasNonNull("topicCode")) problem.setTopicCode(evidence.get("topicCode").asText());
                if (evidence.hasNonNull("topicName")) problem.setTopicName(evidence.get("topicName").asText());
            } catch (JsonProcessingException ignored) {
                // 解析失败时回退到问题自身知识点
            }
        }
        if (problem.getTopicKnowledgePointId() == null) problem.setTopicKnowledgePointId(problem.getKnowledgePointId());
    }

    private DailyPlan loadItems(DailyPlan plan) { plan.setItems(planMapper.findItems(plan.getId())); return plan; }
}
