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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
