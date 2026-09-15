package com.gkstudy.ability.dto;

import java.math.BigDecimal;

public class AbilityChange {
    private final String knowledgePointCode;
    private final BigDecimal oldMastery;
    private final BigDecimal newMastery;
    private final BigDecimal oldSpeed;
    private final BigDecimal newSpeed;
    private final BigDecimal stability;
    private final BigDecimal confidence;
    private final String status;

    public AbilityChange(String knowledgePointCode, BigDecimal oldMastery, BigDecimal newMastery, BigDecimal oldSpeed,
                         BigDecimal newSpeed, BigDecimal stability, BigDecimal confidence, String status) {
        this.knowledgePointCode = knowledgePointCode; this.oldMastery = oldMastery; this.newMastery = newMastery;
        this.oldSpeed = oldSpeed; this.newSpeed = newSpeed; this.stability = stability; this.confidence = confidence; this.status = status;
    }

    public String getKnowledgePointCode() { return knowledgePointCode; }
    public BigDecimal getOldMastery() { return oldMastery; }
    public BigDecimal getNewMastery() { return newMastery; }
    public BigDecimal getOldSpeed() { return oldSpeed; }
    public BigDecimal getNewSpeed() { return newSpeed; }
    public BigDecimal getStability() { return stability; }
    public BigDecimal getConfidence() { return confidence; }
    public String getStatus() { return status; }
}
