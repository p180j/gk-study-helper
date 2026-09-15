package com.gkstudy.learningproblem.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.learningproblem.service.LearningProblemService;
import com.gkstudy.priority.service.ProblemPriorityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning-problems")
public class LearningProblemController {
    private final LearningProblemService learningProblemService;
    private final ProblemPriorityService priorityService;

    public LearningProblemController(LearningProblemService learningProblemService, ProblemPriorityService priorityService) {
        this.learningProblemService = learningProblemService; this.priorityService = priorityService;
    }

    @GetMapping
    public ApiResponse<List<LearningProblem>> problems(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(learningProblemService.problems(userId));
    }

    @GetMapping("/core")
    public ApiResponse<List<LearningProblem>> core(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(priorityService.core(userId));
    }
}
