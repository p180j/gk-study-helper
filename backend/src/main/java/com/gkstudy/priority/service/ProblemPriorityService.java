package com.gkstudy.priority.service;

import com.gkstudy.learningproblem.engine.LearningProblemEngine;
import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.priority.engine.ProblemPriorityEngine;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProblemPriorityService {
    private final ProblemPriorityEngine engine;
    private final LearningProblemMapper problemMapper;

    public ProblemPriorityService(ProblemPriorityEngine engine, LearningProblemMapper problemMapper) {
        this.engine = engine; this.problemMapper = problemMapper;
    }

    public List<LearningProblem> ranked(Long userId) {
        return engine.rank(problemMapper.findPriorityCandidates(userId), LocalDateTime.now());
    }

    public List<LearningProblem> coreForPlan(Long userId) {
        List<LearningProblem> ranked = ranked(userId);
        updatePriorities(ranked);
        return core(ranked);
    }

    /**
     * 计划生成专用：核心问题之外，追加优先级最高的未解决 CONTENT_GAP 作为配套阅读任务（最多 1 个）。
     * 申论维度问题优先级普遍高于素材缺口，若不追加，同一主题的「申论训练 + 政治阅读」无法在同一计划中出现。
     */
    public List<LearningProblem> coreForPlanWithReadingGap(Long userId) {
        List<LearningProblem> ranked = ranked(userId);
        for (LearningProblem problem : ranked) problemMapper.updatePriority(problem.getId(), problem.getPriorityScore());
        List<LearningProblem> active = new ArrayList<>();
        for (LearningProblem problem : ranked) if (!"RESOLVED".equals(problem.getStatus())) active.add(problem);
        List<LearningProblem> core = new ArrayList<>(active.subList(0, Math.min(3, active.size())));
        for (LearningProblem problem : active) {
            if (LearningProblemEngine.CONTENT_GAP.equals(problem.getProblemType()) && !core.contains(problem)) {
                core.add(problem);
                break;
            }
        }
        return core;
    }

    public List<LearningProblem> core(Long userId) {
        return core(ranked(userId));
    }

    private List<LearningProblem> core(List<LearningProblem> ranked) {
        return ranked.subList(0, Math.min(3, ranked.size()));
    }

    /** 优先级回写统一按主键升序加锁，与答题链路（LearningProblemService.evaluate）一致，避免并发死锁。 */
    private void updatePriorities(List<LearningProblem> ranked) {
        List<LearningProblem> ordered = new ArrayList<>(ranked);
        ordered.sort(Comparator.comparing(LearningProblem::getId));
        for (LearningProblem problem : ordered) problemMapper.updatePriority(problem.getId(), problem.getPriorityScore());
    }
}
