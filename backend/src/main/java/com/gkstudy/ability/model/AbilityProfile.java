package com.gkstudy.ability.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AbilityProfile {
    private Long id;
    private Long userId;
    private Long knowledgePointId;
    private BigDecimal masteryScore;
    private BigDecimal speedScore;
    private BigDecimal stabilityScore;
    private BigDecimal confidenceScore;
    private Integer sampleCount;
    private String status;
    private LocalDateTime lastPracticeTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public BigDecimal getMasteryScore() { return masteryScore; }
    public void setMasteryScore(BigDecimal masteryScore) { this.masteryScore = masteryScore; }
    public BigDecimal getSpeedScore() { return speedScore; }
    public void setSpeedScore(BigDecimal speedScore) { this.speedScore = speedScore; }
    public BigDecimal getStabilityScore() { return stabilityScore; }
    public void setStabilityScore(BigDecimal stabilityScore) { this.stabilityScore = stabilityScore; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastPracticeTime() { return lastPracticeTime; }
    public void setLastPracticeTime(LocalDateTime lastPracticeTime) { this.lastPracticeTime = lastPracticeTime; }
}
