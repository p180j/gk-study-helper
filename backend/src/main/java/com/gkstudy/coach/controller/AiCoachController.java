package com.gkstudy.coach.controller;

import com.gkstudy.coach.dto.CoachRequest;
import com.gkstudy.coach.dto.CoachResponse;
import com.gkstudy.coach.service.AiCoachService;
import com.gkstudy.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/coach")
public class AiCoachController {
    private final AiCoachService coachService;
    public AiCoachController(AiCoachService coachService) { this.coachService = coachService; }

    @PostMapping
    public ApiResponse<CoachResponse> ask(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                         @RequestBody(required = false) CoachRequest request) {
        return ApiResponse.success(coachService.ask(userId, request == null ? null : request.getQuestion()));
    }
}
