package com.gkstudy.question.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.question.dto.ImportResult;
import com.gkstudy.question.dto.QuestionResponse;
import com.gkstudy.question.service.QuestionImportService;
import com.gkstudy.question.service.QuestionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService questionService;
    private final QuestionImportService importService;

    public QuestionController(QuestionService questionService, QuestionImportService importService) {
        this.questionService = questionService;
        this.importService = importService;
    }

    @GetMapping
    public ApiResponse<java.util.List<QuestionResponse>> list(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String questionType,
                                            @RequestParam(required = false) String usageType,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(QuestionResponse.from(questionService.list(status, questionType, usageType, keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuestionResponse> detail(@PathVariable Long id) { return ApiResponse.success(QuestionResponse.from(questionService.detail(id))); }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ApiResponse<ImportResult> importCsv(@RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.success(importService.importCsv(file));
    }
}
