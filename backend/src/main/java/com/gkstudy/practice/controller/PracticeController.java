package com.gkstudy.practice.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.practice.dto.AnswerResult;
import com.gkstudy.practice.dto.SubmitAnswerRequest;
import com.gkstudy.practice.dto.UpdateErrorTypeRequest;
import com.gkstudy.practice.model.AnswerRecord;
import com.gkstudy.practice.service.PracticeService;
import com.gkstudy.question.service.QuestionInventoryService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {
    private final PracticeService practiceService;
    private final QuestionInventoryService inventoryService;

    public PracticeController(PracticeService practiceService, QuestionInventoryService inventoryService) {
        this.practiceService = practiceService; this.inventoryService = inventoryService;
    }

    @PostMapping("/answer")
    public ApiResponse<AnswerResult> answer(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                            @Valid @RequestBody SubmitAnswerRequest request) {
        return ApiResponse.success(practiceService.submit(userId, request));
    }

    @PostMapping("/answer/{recordId}/error-type")
    public ApiResponse<Void> updateErrorType(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                             @PathVariable Long recordId,
                                             @Valid @RequestBody UpdateErrorTypeRequest request) {
        practiceService.updateErrorType(userId, recordId, request.getErrorType());
        return ApiResponse.success(null);
    }

    @GetMapping("/inventory-summary")
    public ApiResponse<List<QuestionInventoryService.ModuleInventory>> inventorySummary(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success(inventoryService.moduleSummary(userId));
    }

    @GetMapping("/answers")
    public ApiResponse<List<AnswerRecord>> history(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(practiceService.history(userId, page, size));
    }
}
