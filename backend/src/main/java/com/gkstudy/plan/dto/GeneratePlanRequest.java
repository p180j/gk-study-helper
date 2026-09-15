package com.gkstudy.plan.dto;

import javax.validation.constraints.NotNull;

public class GeneratePlanRequest {
    @NotNull
    private Integer plannedMinutes;

    public Integer getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(Integer plannedMinutes) { this.plannedMinutes = plannedMinutes; }
}
