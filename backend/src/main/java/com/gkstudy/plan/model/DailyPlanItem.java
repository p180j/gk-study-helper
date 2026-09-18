package com.gkstudy.plan.model;

public class DailyPlanItem {
    private Long id;
    private Long planId;
    private String itemType;
    private String targetType;
    private Long targetId;
    private Long learningProblemId;
    private Long knowledgePointId;
    private String knowledgePointCode;
    private String knowledgePointName;
    private String problemType;
    private String purpose;
    private String reason;
    private Integer plannedMinutes;
    private Integer targetQuestionCount;
    private Integer completedQuestionCount;
    private Integer sortNo;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Long getLearningProblemId() { return learningProblemId; }
    public void setLearningProblemId(Long learningProblemId) { this.learningProblemId = learningProblemId; }
    public Long getKnowledgePointId() { return knowledgePointId; }
    public void setKnowledgePointId(Long knowledgePointId) { this.knowledgePointId = knowledgePointId; }
    public String getKnowledgePointCode() { return knowledgePointCode; }
    public void setKnowledgePointCode(String knowledgePointCode) { this.knowledgePointCode = knowledgePointCode; }
    public String getKnowledgePointName() { return knowledgePointName; }
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
    public String getProblemType() { return problemType; }
    public void setProblemType(String problemType) { this.problemType = problemType; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(Integer plannedMinutes) { this.plannedMinutes = plannedMinutes; }
    public Integer getTargetQuestionCount() { return targetQuestionCount; }
    public void setTargetQuestionCount(Integer targetQuestionCount) { this.targetQuestionCount = targetQuestionCount; }
    public Integer getCompletedQuestionCount() { return completedQuestionCount; }
    public void setCompletedQuestionCount(Integer completedQuestionCount) { this.completedQuestionCount = completedQuestionCount; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
