package com.gkstudy.practice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class UpdateErrorTypeRequest {
    @NotBlank @Pattern(regexp = "UNKNOWN|NOT_KNOW|FORMULA|CONDITION|CALCULATION|TIMEOUT|CARELESS") private String errorType;

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
}
