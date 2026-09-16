package com.gkstudy.errordiagnosis.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ErrorDiagnosis {
    private Long id;
    private Long userId;
    private Long answerRecordId;
    private Long questionId;
    private Long knowledgePointId;
    private String suspectedCause;
    private String evidenceJson;
    private BigDecimal confidence;
    private Boolean confirmedByUser;
    private String status;
    private Integer occurrenceCount;
    private String selectedOptionKey;
    private String aiExplanation;
    private String aiProvider;
    private String aiModel;
    private String aiPromptVersion;
    private BigDecimal aiConfidence;
    private String aiStatus;
    private String aiRawResponse;
    private LocalDateTime aiRequestTime;
    private Long aiLatencyMs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAnswerRecordId() { return answerRecordId; }
    public void setAnswerRecordId(Long answerRecordId) { this.answerRecordId = answerRecordId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public String getSuspectedCause() { return suspectedCause; }
    public void setSuspectedCause(String suspectedCause) { this.suspectedCause = suspectedCause; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public Boolean getConfirmedByUser() { return confirmedByUser; }
    public void setConfirmedByUser(Boolean confirmedByUser) { this.confirmedByUser = confirmedByUser; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public String getSelectedOptionKey() { return selectedOptionKey; }
    public void setSelectedOptionKey(String selectedOptionKey) { this.selectedOptionKey = selectedOptionKey; }
    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getAiPromptVersion() { return aiPromptVersion; }
    public void setAiPromptVersion(String aiPromptVersion) { this.aiPromptVersion = aiPromptVersion; }
    public BigDecimal getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(BigDecimal aiConfidence) { this.aiConfidence = aiConfidence; }
    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }
    public String getAiRawResponse() { return aiRawResponse; }
    public void setAiRawResponse(String aiRawResponse) { this.aiRawResponse = aiRawResponse; }
    public LocalDateTime getAiRequestTime() { return aiRequestTime; }
    public void setAiRequestTime(LocalDateTime aiRequestTime) { this.aiRequestTime = aiRequestTime; }
    public Long getAiLatencyMs() { return aiLatencyMs; }
    public void setAiLatencyMs(Long aiLatencyMs) { this.aiLatencyMs = aiLatencyMs; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
