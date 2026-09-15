package com.gkstudy.practice.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.practice.dto.AnswerResult;
import com.gkstudy.practice.dto.SubmitAnswerRequest;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.practice.service.PracticeService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {
    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) { this.practiceService = practiceService; }

    @PostMapping("/answer")
    public ApiResponse<AnswerResult> answer(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                            @Valid @RequestBody SubmitAnswerRequest request) {
        return ApiResponse.success(practiceService.submit(userId, request));
    }

    @GetMapping("/answers")
    public ApiResponse<List<AnswerRecord>> history(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(practiceService.history(userId, page, size));
    }
}
