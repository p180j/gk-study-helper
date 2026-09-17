package com.gkstudy.ai.controller;

import com.gkstudy.ai.dto.AiProviderView;
import com.gkstudy.ai.dto.AiTestResult;
import com.gkstudy.ai.dto.SaveAiProviderRequest;
import com.gkstudy.ai.service.AdminAiProviderService;
import com.gkstudy.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-providers")
public class AdminAiProviderController {
    private final AdminAiProviderService adminService;

    public AdminAiProviderController(AdminAiProviderService adminService) { this.adminService = adminService; }

    @GetMapping
    public ApiResponse<List<AiProviderView>> list() {
        return ApiResponse.success(adminService.list());
    }

    @PutMapping("/{code}")
    public ApiResponse<AiProviderView> save(@PathVariable String code, @Valid @RequestBody SaveAiProviderRequest request) {
        return ApiResponse.success(adminService.save(code, request));
    }

    @PostMapping("/{code}/test")
    public ApiResponse<AiTestResult> test(@PathVariable String code) {
        return ApiResponse.success(adminService.test(code));
    }

    /** 保存前测试：使用页面当前输入（含未保存的新 Key）直接测试连接 */
    @PostMapping("/{code}/test-request")
    public ApiResponse<AiTestResult> testWithRequest(@PathVariable String code, @RequestBody SaveAiProviderRequest request) {
        return ApiResponse.success(adminService.testWithRequest(code, request));
    }

    @PostMapping("/{code}/default")
    public ApiResponse<AiProviderView> setDefault(@PathVariable String code) {
        return ApiResponse.success(adminService.setDefault(code));
    }
}
