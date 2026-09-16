package com.gkstudy.essay.dto;

public class PlanEssayItem {
    private final Long itemId;
    private final Long knowledgePointId;
    private final String topicCode;
    private final String topicName;
    private final Integer plannedMinutes;
    private final String purpose;
    private final String reason;
    private final String status;

    public PlanEssayItem(Long itemId, Long knowledgePointId, String topicCode, String topicName, Integer plannedMinutes,
                         String purpose, String reason, String status) {
        this.itemId = itemId; this.knowledgePointId = knowledgePointId; this.topicCode = topicCode; this.topicName = topicName;
        this.plannedMinutes = plannedMinutes; this.purpose = purpose; this.reason = reason; this.status = status;
    }

    public Long getItemId() { return itemId; }
    public Long getKnowledgePointId() { return knowledgePointId; }
    public String getTopicCode() { return topicCode; }
    public String getTopicName() { return topicName; }
    public Integer getPlannedMinutes() { return plannedMinutes; }
    public String getPurpose() { return purpose; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
}
