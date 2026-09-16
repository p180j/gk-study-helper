package com.gkstudy.essay.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.essay.dto.EssayQuestionView;
import com.gkstudy.essay.dto.EssaySubmitRequest;
import com.gkstudy.essay.dto.EssaySubmitResult;
import com.gkstudy.essay.dto.EssayTaskView;
import com.gkstudy.essay.service.EssayService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/essay")
public class EssayController {
    private final EssayService essayService;

    public EssayController(EssayService essayService) { this.essayService = essayService; }

    @GetMapping("/tasks")
    public ApiResponse<EssayTaskView> tasks(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(essayService.tasks(userId));
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<EssayQuestionView> question(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                   @PathVariable Long id) {
        return ApiResponse.success(essayService.questionView(userId, id));
    }

    @PostMapping("/submit")
    public ApiResponse<EssaySubmitResult> submit(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                 @Valid @RequestBody EssaySubmitRequest request) {
        return ApiResponse.success(essayService.submit(userId, request));
    }

    @PostMapping("/answers/{id}/retry")
    public ApiResponse<EssaySubmitResult> retry(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                @PathVariable Long id) {
        return ApiResponse.success(essayService.retry(userId, id));
    }
}
