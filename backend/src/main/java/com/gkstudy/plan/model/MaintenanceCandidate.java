package com.gkstudy.plan.model;

import java.math.BigDecimal;

public class MaintenanceCandidate {
    private Long knowledgePointId;
    private String knowledgePointCode;
    private String knowledgePointName;
    private BigDecimal examImportance;
    private BigDecimal masteryScore;
    private BigDecimal confidenceScore;
    private String purpose;

    public Long getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public String getKnowledgePointCode() { return knowledgePointCode; }
    public void setKnowledgePointCode(String knowledgePointCode) { this.knowledgePointCode = knowledgePointCode; }
    public String getKnowledgePointName() { return knowledgePointName; }
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
    public BigDecimal getExamImportance() { return examImportance; }
    public void setExamImportance(BigDecimal examImportance) { this.examImportance = examImportance; }
    public BigDecimal getMasteryScore() { return masteryScore; }
    public void setMasteryScore(BigDecimal masteryScore) { this.masteryScore = masteryScore; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
