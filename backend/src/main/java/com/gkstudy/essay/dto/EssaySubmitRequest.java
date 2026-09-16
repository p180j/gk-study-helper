package com.gkstudy.essay.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class EssaySubmitRequest {
    @NotNull private Long essayQuestionId;
    @NotBlank private String answerText;
    @NotNull @Min(0) private Long durationMs;
    @Pattern(regexp = "DAILY|EXTRA|SPECIAL|REVIEW|VALIDATION") private String practiceType;

    public Long getEssayQuestionId() { return essayQuestionId; }
    public void setEssayQuestionId(Long essayQuestionId) { this.essayQuestionId = essayQuestionId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getPracticeType() { return practiceType == null || practiceType.trim().isEmpty() ? "DAILY" : practiceType; }
    public void setPracticeType(String practiceType) { this.practiceType = practiceType; }
}
