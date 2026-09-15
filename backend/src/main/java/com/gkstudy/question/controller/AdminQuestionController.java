package com.gkstudy.question.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.question.dto.AdminQuestionResponse;
import com.gkstudy.question.service.QuestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {
    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) { this.questionService = questionService; }

    @GetMapping("/{id}")
    public ApiResponse<AdminQuestionResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(AdminQuestionResponse.from(questionService.detail(id)));
    }
}
