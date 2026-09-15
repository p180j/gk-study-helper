package com.gkstudy.plan.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.plan.dto.GeneratePlanRequest;
import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.service.DailyPlanService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/plan")
public class DailyPlanController {
    private final DailyPlanService planService;

    public DailyPlanController(DailyPlanService planService) { this.planService = planService; }

    @GetMapping("/today")
    public ApiResponse<DailyPlan> today(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(planService.today(userId));
    }

    @PostMapping("/generate")
    public ApiResponse<DailyPlan> generate(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                           @Valid @RequestBody GeneratePlanRequest request) {
        return ApiResponse.success(planService.generate(userId, request.getPlannedMinutes()));
    }
}
