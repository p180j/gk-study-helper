package com.gkstudy.learningproblem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LearningProblem {
    private Long id;
    private Long userId;
    private String problemType;
    private Long knowledgePointId;
    private String knowledgePointCode;
    private String knowledgePointName;
    private String title;
    private String description;
    private BigDecimal severity;
    private BigDecimal priorityScore;
    private String status;
    private String evidenceJson;
    private String rootCause;
    private LocalDateTime discoveredTime;
    private LocalDateTime resolvedTime;
    private Integer validationCount;
    private Integer validationPassCount;
    private BigDecimal examImportance;
    private BigDecimal improvementPotential;
    private BigDecimal transferValue;
    private BigDecimal abilityConfidence;
    // 非表字段：申论类问题（ESSAY_*/CONTENT_GAP）的主题知识点信息，由 evidence_json 提取，
    // 供后续计划引擎按申论主题生成训练计划项时使用。
    private Long topicKnowledgePointId;
    private String topicCode;
    private String topicName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }
    public Long getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public String getKnowledgePointCode() { return knowledgePointCode; }
    public void setKnowledgePointCode(String knowledgePointCode) { this.knowledgePointCode = knowledgePointCode; }
    public String getKnowledgePointName() { return knowledgePointName; }
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getSeverity() { return severity; }
    public void setSeverity(BigDecimal severity) { this.severity = severity; }
    public BigDecimal getPriorityScore() { return priorityScore; }
    public void setPriorityScore(BigDecimal priorityScore) { this.priorityScore = priorityScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public LocalDateTime getDiscoveredTime() { return discoveredTime; }
    public void setDiscoveredTime(LocalDateTime discoveredTime) { this.discoveredTime = discoveredTime; }
    public LocalDateTime getResolvedTime() { return resolvedTime; }
    public void setResolvedTime(LocalDateTime resolvedTime) { this.resolvedTime = resolvedTime; }
    public Integer getValidationCount() { return validationCount; }
    public void setValidationCount(Integer validationCount) { this.validationCount = validationCount; }
    public Integer getValidationPassCount() { return validationPassCount; }
    public void setValidationPassCount(Integer validationPassCount) { this.validationPassCount = validationPassCount; }
    public BigDecimal getExamImportance() { return examImportance; }
    public void setExamImportance(BigDecimal examImportance) { this.examImportance = examImportance; }
    public BigDecimal getImprovementPotential() { return improvementPotential; }
    public void setImprovementPotential(BigDecimal improvementPotential) { this.improvementPotential = improvementPotential; }
    public BigDecimal getTransferValue() { return transferValue; }
    public void setTransferValue(BigDecimal transferValue) { this.transferValue = transferValue; }
    public BigDecimal getAbilityConfidence() { return abilityConfidence; }
    public void setAbilityConfidence(BigDecimal abilityConfidence) { this.abilityConfidence = abilityConfidence; }
    public Long getTopicKnowledgePointId() { return topicKnowledgePointId; }
    public void setTopicKnowledgePointId(Long topicKnowledgePointId) { this.topicKnowledgePointId = topicKnowledgePointId; }
    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
}
