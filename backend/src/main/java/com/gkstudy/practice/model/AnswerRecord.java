package com.gkstudy.practice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AnswerRecord {
    private Long id;
    private Long userId;
    private Long questionId;
    private Long planItemId;
    private Integer questionVersion;
    private String practiceType;
    private String userAnswer;
    private String correctAnswerSnapshot;
    private Boolean correct;
    private Long durationMs;
    private Integer standardTimeSecondsSnapshot;
    private BigDecimal difficultySnapshot;
    private String knowledgeSnapshot;
    private String confidenceType;
    private String errorType;
    private LocalDateTime answerTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getPlanItemId() { return planItemId; }
    public void setPlanItemId(Long planItemId) { this.planItemId = planItemId; }
    public Integer getQuestionVersion() { return questionVersion; }
    public void setQuestionVersion(Integer questionVersion) { this.questionVersion = questionVersion; }
    public String getPracticeType() { return practiceType; }
    public void setPracticeType(String practiceType) { this.practiceType = practiceType; }
    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    public String getCorrectAnswerSnapshot() { return correctAnswerSnapshot; }
    public void setCorrectAnswerSnapshot(String correctAnswerSnapshot) { this.correctAnswerSnapshot = correctAnswerSnapshot; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Integer getStandardTimeSecondsSnapshot() { return standardTimeSecondsSnapshot; }
    public void setStandardTimeSecondsSnapshot(Integer standardTimeSecondsSnapshot) { this.standardTimeSecondsSnapshot = standardTimeSecondsSnapshot; }
    public BigDecimal getDifficultySnapshot() { return difficultySnapshot; }
    public void setDifficultySnapshot(BigDecimal difficultySnapshot) { this.difficultySnapshot = difficultySnapshot; }
    public String getKnowledgeSnapshot() { return knowledgeSnapshot; }
    public void setKnowledgeSnapshot(String knowledgeSnapshot) { this.knowledgeSnapshot = knowledgeSnapshot; }
    public String getConfidenceType() { return confidenceType; }
    public void setConfidenceType(String confidenceType) { this.confidenceType = confidenceType; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public LocalDateTime getAnswerTime() { return answerTime; }
    public void setAnswerTime(LocalDateTime answerTime) { this.answerTime = answerTime; }
}
