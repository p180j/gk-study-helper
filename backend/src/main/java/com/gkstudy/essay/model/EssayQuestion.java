package com.gkstudy.essay.model;

import java.time.LocalDateTime;

public class EssayQuestion {
    private Long id;
    private Long topicKnowledgePointId;
    private String questionType;
    private String material;
    private String prompt;
    private Integer wordLimitMin;
    private Integer wordLimitMax;
    private Integer standardTimeSeconds;
    private String referenceAnswer;
    private String referencePointsJson;
    private String sourceType;
    private Integer sourceYear;
    private String sourceExam;
    private String sourceName;
    private String status;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String topicCode;
    private String topicName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTopicKnowledgePointId() { return topicKnowledgePointId; }
    public void setTopicKnowledgePointId(Long topicKnowledgePointId) { this.topicKnowledgePointId = topicKnowledgePointId; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public Integer getWordLimitMin() { return wordLimitMin; }
    public void setWordLimitMin(Integer wordLimitMin) { this.wordLimitMin = wordLimitMin; }
    public Integer getWordLimitMax() { return wordLimitMax; }
    public void setWordLimitMax(Integer wordLimitMax) { this.wordLimitMax = wordLimitMax; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public void setStandardTimeSeconds(Integer standardTimeSeconds) { this.standardTimeSeconds = standardTimeSeconds; }
    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
    public String getReferencePointsJson() { return referencePointsJson; }
    public void setReferencePointsJson(String referencePointsJson) { this.referencePointsJson = referencePointsJson; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Integer getSourceYear() { return sourceYear; }
    public void setSourceYear(Integer sourceYear) { this.sourceYear = sourceYear; }
    public String getSourceExam() { return sourceExam; }
    public void setSourceExam(String sourceExam) { this.sourceExam = sourceExam; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
}
