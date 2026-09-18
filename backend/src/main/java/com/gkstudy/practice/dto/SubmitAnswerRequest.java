package com.gkstudy.practice.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class SubmitAnswerRequest {
    @NotNull(message = "questionId不能为空") private Long questionId;
    private Long planItemId;
    @NotBlank(message = "userAnswer不能为空") private String userAnswer;
    @NotNull(message = "durationMs不能为空") @Min(value = 0, message = "durationMs不能小于0") private Long durationMs;
    @NotBlank(message = "practiceType不能为空") @Pattern(regexp = "DAILY|EXTRA|SPECIAL|REVIEW|VALIDATION|MOCK", message = "practiceType不合法") private String practiceType;
    @NotBlank(message = "confidenceType不能为空") @Pattern(regexp = "SURE|HESITANT|GUESS|UNKNOWN", message = "confidenceType不合法") private String confidenceType;
    /** 错因只在错误作答时由用户补充；缺省时服务端保存 UNKNOWN。 */
    @Pattern(regexp = "UNKNOWN|NOT_KNOW|FORMULA|CONDITION|CALCULATION|TIMEOUT|CARELESS", message = "errorType不合法") private String errorType;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getPlanItemId() { return planItemId; }
    public void setPlanItemId(Long planItemId) { this.planItemId = planItemId; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getPracticeType() { return practiceType; }
    public void setPracticeType(String practiceType) { this.practiceType = practiceType; }
    public String getConfidenceType() { return confidenceType; }
    public void setConfidenceType(String confidenceType) { this.confidenceType = confidenceType; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
}
