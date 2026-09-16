package com.gkstudy.essay.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.essay.dto.AdminEssayAnswerView;
import com.gkstudy.essay.dto.CreateEssayQuestionRequest;
import com.gkstudy.essay.model.EssayAnswer;
import com.gkstudy.essay.model.EssayQuestion;
import com.gkstudy.essay.service.AdminEssayService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminEssayController {
    private final AdminEssayService adminEssayService;

    public AdminEssayController(AdminEssayService adminEssayService) { this.adminEssayService = adminEssayService; }

    @GetMapping("/essay-questions")
    public ApiResponse<Map<String, Object>> questions(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String questionType,
                                                      @RequestParam(required = false) String topicCode,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int pageSize) {
        List<EssayQuestion> list = adminEssayService.questions(keyword, questionType, topicCode, status, page, pageSize);
        int total = adminEssayService.countQuestions(keyword, questionType, topicCode, status);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("list", list);
        return ApiResponse.success(result);
    }

    @GetMapping("/essay-questions/{id}")
    public ApiResponse<EssayQuestion> questionDetail(@PathVariable Long id) {
        return ApiResponse.success(adminEssayService.questionDetail(id));
    }

    @PostMapping("/essay-questions")
    public ApiResponse<EssayQuestion> createQuestion(@Valid @RequestBody CreateEssayQuestionRequest request) {
        return ApiResponse.success(adminEssayService.createQuestion(request));
    }

    @GetMapping("/essay-answers")
    public ApiResponse<Map<String, Object>> answers(@RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) Long essayQuestionId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        List<EssayAnswer> list = adminEssayService.answers(userId, essayQuestionId, page, pageSize);
        int total = adminEssayService.countAnswers(userId, essayQuestionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("list", list);
        return ApiResponse.success(result);
    }

    @GetMapping("/essay-answers/{id}")
    public ApiResponse<AdminEssayAnswerView> answerDetail(@PathVariable Long id) {
        return ApiResponse.success(adminEssayService.answerDetail(id));
    }
}
