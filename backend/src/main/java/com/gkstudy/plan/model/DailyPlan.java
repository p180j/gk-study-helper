package com.gkstudy.plan.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DailyPlan {
    private Long id;
    private Long userId;
    private LocalDate planDate;
    private Integer plannedMinutes;
    private Integer actualMinutes;
    private String status;
    private String generationReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<DailyPlanItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
    public Integer getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(Integer plannedMinutes) { this.plannedMinutes = plannedMinutes; }
    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer actualMinutes) { this.actualMinutes = actualMinutes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGenerationReason() { return generationReason; }
    public void setGenerationReason(String generationReason) { this.generationReason = generationReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<DailyPlanItem> getItems() { return items; }
    public void setItems(List<DailyPlanItem> items) { this.items = items; }
}
