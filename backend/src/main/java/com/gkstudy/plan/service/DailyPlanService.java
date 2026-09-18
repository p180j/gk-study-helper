package com.gkstudy.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.engine.DailyPlanEngine;
import com.gkstudy.plan.engine.QuestionTaskPolicy;
import com.gkstudy.plan.mapper.DailyPlanMapper;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.model.MaintenanceCandidate;
import com.gkstudy.priority.service.ProblemPriorityService;
import com.gkstudy.question.mapper.QuestionMapper;
import com.gkstudy.question.service.QuestionInventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailyPlanService {
    private static final String ITEM_TYPE_QUESTION_SET = "QUESTION_SET";
    private static final String ITEM_TYPE_REVIEW = "REVIEW";
    private static final String STATUS_INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    private static final String INSUFFICIENT_SUFFIX = "（当前可用题目不足，暂无法完成本组训练）";

    private final DailyPlanEngine engine;
    private final DailyPlanMapper planMapper;
    private final ProblemPriorityService priorityService;
    private final ObjectMapper objectMapper;
    private final QuestionMapper questionMapper;
    private final QuestionInventoryService inventoryService;
    private final QuestionTaskPolicy policy;

    public DailyPlanService(DailyPlanEngine engine, DailyPlanMapper planMapper, ProblemPriorityService priorityService,
                            ObjectMapper objectMapper, QuestionMapper questionMapper,
                            QuestionInventoryService inventoryService, QuestionTaskPolicy policy) {
        this.engine = engine; this.planMapper = planMapper; this.priorityService = priorityService; this.objectMapper = objectMapper;
        this.questionMapper = questionMapper; this.inventoryService = inventoryService; this.policy = policy;
    }

    @Transactional
    public DailyPlan today(Long userId) {
        LocalDate today = LocalDate.now(); DailyPlan existing = planMapper.findForUpdate(userId, today);
        if (existing != null) {
            DailyPlan loaded = loadItems(existing);
            if (!loaded.getItems().isEmpty()) return loaded;
            int plannedMinutes = existing.getPlannedMinutes() > 0 ? existing.getPlannedMinutes() : 45;
            return generate(userId, today, plannedMinutes, true);
        }
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
        candidates.addAll(filterInsufficientAssessmentCandidates(userId, planMapper.findExplorationCandidates(userId)));
        DailyPlan generated = engine.generate(userId, planDate, plannedMinutes, coreProblems, candidates);
        applyQuestionTargets(userId, generated);
        if (existing == null) {
            planMapper.insertPlan(generated);
        } else {
            generated.setId(existing.getId()); generated.setActualMinutes(existing.getActualMinutes());
            planMapper.deleteItems(existing.getId()); planMapper.updatePlan(generated);
        }
        for (DailyPlanItem item : generated.getItems()) { item.setPlanId(generated.getId()); planMapper.insertItem(item); }
        return generated;
    }

    /** 摸底候选库存不足（< ASSESSMENT_MIN）时过滤，MAINTENANCE 候选不受影响；若全部摸底候选库存不足则保持原样，让引擎生成 INSUFFICIENT_STOCK 任务供用户感知。 */
    private List<MaintenanceCandidate> filterInsufficientAssessmentCandidates(Long userId, List<MaintenanceCandidate> exploration) {
        List<MaintenanceCandidate> sufficient = new ArrayList<>();
        for (MaintenanceCandidate candidate : exploration) {
            if (!"ASSESSMENT".equals(candidate.getPurpose())) { sufficient.add(candidate); continue; }
            if (inventoryService.countAvailable(candidate.getKnowledgePointId(), "ASSESSMENT", userId) >= policy.minCount("ASSESSMENT")) sufficient.add(candidate);
        }
        return sufficient.isEmpty() && !exploration.isEmpty() ? exploration : sufficient;
    }

    /** 为题目类任务写入目标题量与初始进度：库存充足时目标=min(策略值,可用库存)；库存不足时目标不缩水并标记 INSUFFICIENT_STOCK。 */
    private void applyQuestionTargets(Long userId, DailyPlan plan) {
        for (DailyPlanItem item : plan.getItems()) {
            item.setCompletedQuestionCount(0);
            if (!ITEM_TYPE_QUESTION_SET.equals(item.getItemType()) && !ITEM_TYPE_REVIEW.equals(item.getItemType())) continue;
            Integer avgSec = questionMapper.avgStandardSeconds(item.getKnowledgePointId());
            int target = policy.targetCount(item.getPurpose(), item.getPlannedMinutes(),
                    avgSec == null ? QuestionTaskPolicy.DEFAULT_STANDARD_SECONDS : avgSec);
            item.setTargetQuestionCount(target);
            int available = inventoryService.countAvailable(item.getKnowledgePointId(), item.getPurpose(), userId);
            if (available < policy.minCount(item.getPurpose())) {
                item.setStatus(STATUS_INSUFFICIENT_STOCK);
                item.setReason((item.getReason() == null ? "" : item.getReason()) + INSUFFICIENT_SUFFIX);
            } else if (available < target) {
                item.setTargetQuestionCount(available);
            }
        }
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
