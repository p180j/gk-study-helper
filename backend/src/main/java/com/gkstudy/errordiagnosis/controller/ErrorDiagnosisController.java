package com.gkstudy.errordiagnosis.controller;

import com.gkstudy.common.ApiResponse;
import com.gkstudy.errordiagnosis.dto.DiagnosisDecisionRequest;
import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import com.gkstudy.errordiagnosis.service.ErrorDiagnosisService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/error-diagnoses")
public class ErrorDiagnosisController {
    private final ErrorDiagnosisService diagnosisService;

    public ErrorDiagnosisController(ErrorDiagnosisService diagnosisService) { this.diagnosisService = diagnosisService; }

    @PostMapping("/{id}/decision")
    public ApiResponse<ErrorDiagnosis> decide(@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
                                              @PathVariable Long id,
                                              @Valid @RequestBody DiagnosisDecisionRequest request) {
        return ApiResponse.success(diagnosisService.decide(userId, id, request.getConfirmed()));
    }
}
