package com.gkstudy.essay.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EssayAnswer {
    private Long id;
    private Long userId;
    private Long essayQuestionId;
    private Integer questionVersion;
    private String practiceType;
    private String answerText;
    private Long durationMs;
    private Integer wordCount;
    private LocalDateTime submitTime;
    private String promptPreview;
    private String topicName;
    private String questionTypeName;
    private BigDecimal totalScore;
    private String evaluator;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getEssayQuestionId() { return essayQuestionId; }
    public void setEssayQuestionId(Long essayQuestionId) { this.essayQuestionId = essayQuestionId; }
    public Integer getQuestionVersion() { return questionVersion; }
    public void setQuestionVersion(Integer questionVersion) { this.questionVersion = questionVersion; }
    public String getPracticeType() { return practiceType; }
    public void setPracticeType(String practiceType) { this.practiceType = practiceType; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public String getPromptPreview() { return promptPreview; }
    public void setPromptPreview(String promptPreview) { this.promptPreview = promptPreview; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getQuestionTypeName() { return questionTypeName; }
    public void setQuestionTypeName(String questionTypeName) { this.questionTypeName = questionTypeName; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getEvaluator() { return evaluator; }
    public void setEvaluator(String evaluator) { this.evaluator = evaluator; }
}
