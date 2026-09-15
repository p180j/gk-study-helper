package com.gkstudy.priority.service;

import com.gkstudy.learningproblem.mapper.LearningProblemMapper;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.priority.engine.ProblemPriorityEngine;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        for (LearningProblem problem : ranked) problemMapper.updatePriority(problem.getId(), problem.getPriorityScore());
        return core(ranked);
    }

    public List<LearningProblem> core(Long userId) {
        return core(ranked(userId));
    }

    private List<LearningProblem> core(List<LearningProblem> ranked) {
        return ranked.subList(0, Math.min(3, ranked.size()));
    }
}
