package com.gkstudy.practice.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class SubmitAnswerRequest {
    @NotNull private Long questionId;
    @NotBlank private String userAnswer;
    @NotNull @Min(0) private Long durationMs;
    @NotBlank @Pattern(regexp = "DAILY|EXTRA|SPECIAL|REVIEW|VALIDATION|MOCK") private String practiceType;
    @NotBlank @Pattern(regexp = "SURE|HESITANT|GUESS|UNKNOWN") private String confidenceType;
    @NotBlank @Pattern(regexp = "UNKNOWN|NOT_KNOW|FORMULA|CONDITION|CALCULATION|TIMEOUT|CARELESS") private String errorType;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
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
