package com.gkstudy.plan.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.essay.dto.EssayQuestionView;
import com.gkstudy.plan.dto.GeneratePlanRequest;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.service.DailyPlanService;
import com.gkstudy.plan.service.PlanQuestionService;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.reading.dto.MaterialDetailView;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/plan")
public class DailyPlanController {
    private final DailyPlanService planService;
    private final PlanQuestionService questionService;

    public DailyPlanController(DailyPlanService planService, PlanQuestionService questionService) {
        this.planService = planService; this.questionService = questionService;
    }

    @GetMapping("/today")
    public ApiResponse<DailyPlan> today(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(planService.today(userId));
    }

    @PostMapping("/generate")
    public ApiResponse<DailyPlan> generate(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                           @Valid @RequestBody GeneratePlanRequest request) {
        return ApiResponse.success(planService.generate(userId, request.getPlannedMinutes()));
    }

    @GetMapping("/items/{itemId}/questions")
    public ApiResponse<java.util.List<QuestionResponse>> questions(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long itemId, @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(questionService.questions(userId, itemId, limit));
    }

    @GetMapping("/items/{itemId}/essay-question")
    public ApiResponse<EssayQuestionView> essayQuestion(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long itemId) {
        return ApiResponse.success(questionService.essayQuestion(userId, itemId));
    }

    @GetMapping("/items/{itemId}/reading-material")
    public ApiResponse<MaterialDetailView> readingMaterial(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @PathVariable Long itemId) {
        return ApiResponse.success(questionService.readingMaterial(userId, itemId));
    }
}
