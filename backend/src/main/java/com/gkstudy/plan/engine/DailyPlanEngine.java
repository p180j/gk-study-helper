package com.gkstudy.plan.engine;

import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.model.MaintenanceCandidate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class DailyPlanEngine {
    private static final Set<Integer> ALLOWED_MINUTES = new HashSet<>(Arrays.asList(20, 45, 60, 90));
    private static final Map<Integer, Integer> ITEM_COUNTS;
    private static final String ITEM_TYPE_ESSAY = "ESSAY";
    private static final String ITEM_TYPE_READING = "READING";
    private static final int ESSAY_PLANNED_MINUTES = 15;
    private static final int READING_PLANNED_MINUTES = 10;
    private static final int FLEX_ITEM_MIN_MINUTES = 5;

    static {
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(20, 1); counts.put(45, 3); counts.put(60, 3); counts.put(90, 4);
        ITEM_COUNTS = Collections.unmodifiableMap(counts);
    }

    public DailyPlan generate(Long userId, LocalDate planDate, int plannedMinutes, List<LearningProblem> coreProblems,
                              List<MaintenanceCandidate> maintenanceCandidates) {
        if (!ALLOWED_MINUTES.contains(plannedMinutes)) throw new IllegalArgumentException("计划时长仅支持 20、45、60、90 分钟");
        DailyPlan plan = new DailyPlan(); plan.setUserId(userId); plan.setPlanDate(planDate); plan.setPlannedMinutes(plannedMinutes);
        plan.setActualMinutes(0); plan.setStatus("PENDING");
        List<DailyPlanItem> items = selectItems(coreProblems, maintenanceCandidates, ITEM_COUNTS.get(plannedMinutes));
        allocate(items, plannedMinutes); plan.setItems(items); plan.setGenerationReason(generationReason(items));
        return plan;
    }

    private List<DailyPlanItem> selectItems(List<LearningProblem> problems, List<MaintenanceCandidate> maintenance, int limit) {
        List<DailyPlanItem> items = new ArrayList<>(); Set<String> usedKeys = new HashSet<>();
        for (LearningProblem problem : orderedProblems(problems)) {
            // CONTENT_GAP 配套阅读任务允许超出常规任务数上限（最多 1 个），保证申论-阅读联动
            if (items.size() >= limit && !LearningProblemEngine.CONTENT_GAP.equals(problem.getProblemType())) continue;
            if (LearningProblemEngine.CONTENT_GAP.equals(problem.getProblemType())
                    && items.stream().anyMatch(item -> ITEM_TYPE_READING.equals(item.getItemType()))) continue;
            DailyPlanItem item = problemItem(problem);
            // 去重维度为「知识点 + 任务类型」：同一主题可同时出现 ESSAY 与 READING 两个任务
            if (!usedKeys.add(itemKey(item))) continue;
            items.add(item);
        }
        for (MaintenanceCandidate candidate : maintenance == null ? Collections.<MaintenanceCandidate>emptyList() : maintenance) {
            if (items.size() >= limit) continue;
            DailyPlanItem item = maintenanceItem(candidate);
            if (!usedKeys.add(itemKey(item))) continue;
            items.add(item);
        }
        return items;
    }

    private String itemKey(DailyPlanItem item) { return item.getKnowledgePointId() + ":" + item.getItemType(); }

    private List<LearningProblem> orderedProblems(List<LearningProblem> problems) {
        List<LearningProblem> ordered = new ArrayList<>();
        if (problems == null) return ordered;
        for (LearningProblem problem : problems) if ("VERIFYING".equals(problem.getStatus())) ordered.add(problem);
        for (LearningProblem problem : problems) if (!"VERIFYING".equals(problem.getStatus())) ordered.add(problem);
        return ordered;
    }

    private DailyPlanItem problemItem(LearningProblem problem) {
        if (LearningProblemEngine.CONTENT_GAP.equals(problem.getProblemType())) return readingItem(problem);
        if (LearningProblemEngine.isEssayDimensionProblem(problem.getProblemType())) return essayItem(problem);
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("QUESTION_SET"); item.setTargetType("LEARNING_PROBLEM");
        item.setTargetId(problem.getId()); item.setLearningProblemId(problem.getId()); item.setKnowledgePointId(problem.getKnowledgePointId());
        item.setKnowledgePointCode(problem.getKnowledgePointCode()); item.setKnowledgePointName(problem.getKnowledgePointName()); item.setProblemType(problem.getProblemType());
        item.setPurpose("VERIFYING".equals(problem.getStatus()) ? "VALIDATION" : "TRAINING"); item.setStatus("PENDING");
        item.setReason(problemReason(problem)); return item;
    }

    private DailyPlanItem essayItem(LearningProblem problem) {
        DailyPlanItem item = new DailyPlanItem(); item.setItemType(ITEM_TYPE_ESSAY); item.setTargetType("LEARNING_PROBLEM");
        item.setTargetId(problem.getId()); item.setLearningProblemId(problem.getId());
        item.setKnowledgePointId(problem.getTopicKnowledgePointId() != null ? problem.getTopicKnowledgePointId() : problem.getKnowledgePointId());
        item.setKnowledgePointCode(problem.getTopicCode() != null ? problem.getTopicCode() : problem.getKnowledgePointCode());
        item.setKnowledgePointName(problem.getTopicName() != null ? problem.getTopicName() : problem.getKnowledgePointName());
        item.setProblemType(problem.getProblemType());
        item.setPurpose("VERIFYING".equals(problem.getStatus()) ? "VALIDATION" : "TRAINING"); item.setStatus("PENDING");
        item.setReason(essayReason(problem)); return item;
    }

    private DailyPlanItem readingItem(LearningProblem problem) {
        DailyPlanItem item = new DailyPlanItem(); item.setItemType(ITEM_TYPE_READING); item.setTargetType("LEARNING_PROBLEM");
        item.setTargetId(problem.getId()); item.setLearningProblemId(problem.getId()); item.setKnowledgePointId(problem.getKnowledgePointId());
        item.setKnowledgePointCode(problem.getKnowledgePointCode()); item.setKnowledgePointName(problem.getKnowledgePointName());
        item.setProblemType(problem.getProblemType());
        item.setPurpose("TRAINING"); item.setStatus("PENDING");
        item.setReason("《" + problem.getKnowledgePointName() + "》主题申论表现弱且政治阅读覆盖不足，安排主题阅读补充素材");
        return item;
    }

    private DailyPlanItem maintenanceItem(MaintenanceCandidate candidate) {
        boolean exploration = "TRAINING".equals(candidate.getPurpose());
        DailyPlanItem item = new DailyPlanItem(); item.setItemType(exploration ? "QUESTION_SET" : "REVIEW"); item.setTargetType("KNOWLEDGE_POINT");
        item.setTargetId(candidate.getKnowledgePointId()); item.setKnowledgePointId(candidate.getKnowledgePointId());
        item.setKnowledgePointCode(candidate.getKnowledgePointCode()); item.setKnowledgePointName(candidate.getKnowledgePointName());
        item.setPurpose(exploration ? "TRAINING" : "REVIEW"); item.setStatus("PENDING");
        item.setReason(exploration ? "完成" + candidate.getKnowledgePointName() + "初始训练以积累真实能力样本" : "保持" + candidate.getKnowledgePointName() + "已掌握能力");
        return item;
    }

    private String problemReason(LearningProblem problem) {
        if ("VERIFYING".equals(problem.getStatus())) return "验证" + problem.getKnowledgePointName() + problemName(problem.getProblemType()) + "是否稳定改善";
        if ("PROCESSING".equals(problem.getStatus())) return "继续改善" + problem.getKnowledgePointName() + problemName(problem.getProblemType());
        if ("OBSERVING".equals(problem.getStatus())) return "补充样本确认" + problem.getKnowledgePointName() + problemName(problem.getProblemType());
        return "优先处理" + problem.getKnowledgePointName() + problemName(problem.getProblemType());
    }

    private String problemName(String type) {
        if ("MASTERY".equals(type)) return "掌握问题";
        if ("SPEED".equals(type)) return "速度问题";
        return "稳定性问题";
    }

    private String essayReason(LearningProblem problem) {
        String topic = problem.getTopicName();
        String prefix = topic == null || topic.isEmpty() ? "申论" : topic + "申论";
        return prefix + "'" + essayProblemName(problem) + "'问题，安排申论专项训练";
    }

    private String essayProblemName(LearningProblem problem) {
        String name = problem.getKnowledgePointName() == null ? "" : problem.getKnowledgePointName();
        return LearningProblemEngine.ESSAY_STRUCTURE.equals(problem.getProblemType()) ? name + "结构" : name;
    }

    private void allocate(List<DailyPlanItem> items, int totalMinutes) {
        if (items.isEmpty()) return;
        List<DailyPlanItem> fixedItems = new ArrayList<>(); List<DailyPlanItem> flexItems = new ArrayList<>();
        for (DailyPlanItem item : items) {
            if (fixedMinutes(item.getItemType()) > 0) fixedItems.add(item); else flexItems.add(item);
        }
        if (fixedItems.isEmpty()) {
            splitEvenly(items, totalMinutes);
        } else {
            int fixedSum = 0; for (DailyPlanItem item : fixedItems) fixedSum += fixedMinutes(item.getItemType());
            if (fixedSum + flexItems.size() * FLEX_ITEM_MIN_MINUTES > totalMinutes) {
                int fixedBudget = totalMinutes - flexItems.size() * FLEX_ITEM_MIN_MINUTES;
                for (DailyPlanItem item : fixedItems) {
                    item.setPlannedMinutes(Math.max(FLEX_ITEM_MIN_MINUTES,
                            (int) ((long) fixedMinutes(item.getItemType()) * fixedBudget / fixedSum)));
                }
            } else {
                for (DailyPlanItem item : fixedItems) item.setPlannedMinutes(fixedMinutes(item.getItemType()));
            }
            int assigned = 0; for (DailyPlanItem item : fixedItems) assigned += item.getPlannedMinutes();
            if (flexItems.isEmpty()) {
                // 全部为 ESSAY/READING 时，剩余分钟按默认分钟占比分配
                int remaining = totalMinutes - assigned;
                int weightSum = 0; for (DailyPlanItem item : fixedItems) weightSum += fixedMinutes(item.getItemType());
                int distributed = 0;
                for (int index = 0; index < fixedItems.size(); index++) {
                    DailyPlanItem item = fixedItems.get(index);
                    int extra = index == fixedItems.size() - 1 ? remaining - distributed
                            : (int) ((long) fixedMinutes(item.getItemType()) * remaining / weightSum);
                    item.setPlannedMinutes(item.getPlannedMinutes() + extra);
                    distributed += extra;
                }
            } else {
                splitEvenly(flexItems, totalMinutes - assigned);
            }
        }
        for (int index = 0; index < items.size(); index++) items.get(index).setSortNo(index + 1);
    }

    private int fixedMinutes(String itemType) {
        if (ITEM_TYPE_ESSAY.equals(itemType)) return ESSAY_PLANNED_MINUTES;
        if (ITEM_TYPE_READING.equals(itemType)) return READING_PLANNED_MINUTES;
        return 0;
    }

    private void splitEvenly(List<DailyPlanItem> items, int totalMinutes) {
        int base = totalMinutes / items.size(); int remainder = totalMinutes % items.size();
        for (int index = 0; index < items.size(); index++) {
            items.get(index).setPlannedMinutes(base + (index < remainder ? 1 : 0));
        }
    }

    private String generationReason(List<DailyPlanItem> items) {
        if (items.isEmpty()) return "当前没有可生成计划的学习问题或保持项";
        StringBuilder reason = new StringBuilder();
        for (DailyPlanItem item : items) {
            if (reason.length() > 0) reason.append("；");
            reason.append(item.getReason());
        }
        return reason.append("。").toString();
    }
}
