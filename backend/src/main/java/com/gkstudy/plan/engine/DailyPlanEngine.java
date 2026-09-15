package com.gkstudy.plan.engine;

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
        List<DailyPlanItem> items = new ArrayList<>(); Set<Long> knowledgeIds = new HashSet<>();
        for (LearningProblem problem : orderedProblems(problems)) {
            if (items.size() >= limit || !knowledgeIds.add(problem.getKnowledgePointId())) continue;
            items.add(problemItem(problem));
        }
        for (MaintenanceCandidate candidate : maintenance == null ? Collections.<MaintenanceCandidate>emptyList() : maintenance) {
            if (items.size() >= limit || !knowledgeIds.add(candidate.getKnowledgePointId())) continue;
            items.add(maintenanceItem(candidate));
        }
        return items;
    }

    private List<LearningProblem> orderedProblems(List<LearningProblem> problems) {
        List<LearningProblem> ordered = new ArrayList<>();
        if (problems == null) return ordered;
        for (LearningProblem problem : problems) if ("VERIFYING".equals(problem.getStatus())) ordered.add(problem);
        for (LearningProblem problem : problems) if (!"VERIFYING".equals(problem.getStatus())) ordered.add(problem);
        return ordered;
    }

    private DailyPlanItem problemItem(LearningProblem problem) {
        DailyPlanItem item = new DailyPlanItem(); item.setItemType("QUESTION_SET"); item.setTargetType("LEARNING_PROBLEM");
        item.setTargetId(problem.getId()); item.setLearningProblemId(problem.getId()); item.setKnowledgePointId(problem.getKnowledgePointId());
        item.setKnowledgePointCode(problem.getKnowledgePointCode()); item.setKnowledgePointName(problem.getKnowledgePointName()); item.setProblemType(problem.getProblemType());
        item.setPurpose("VERIFYING".equals(problem.getStatus()) ? "VALIDATION" : "TRAINING"); item.setStatus("PENDING");
        item.setReason(problemReason(problem)); return item;
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

    private void allocate(List<DailyPlanItem> items, int totalMinutes) {
        if (items.isEmpty()) return;
        int base = totalMinutes / items.size(); int remainder = totalMinutes % items.size();
        for (int index = 0; index < items.size(); index++) {
            items.get(index).setPlannedMinutes(base + (index < remainder ? 1 : 0)); items.get(index).setSortNo(index + 1);
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
