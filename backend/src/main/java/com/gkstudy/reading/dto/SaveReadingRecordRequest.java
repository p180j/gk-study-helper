package com.gkstudy.reading.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class SaveReadingRecordRequest {
    @NotNull private Long materialId;
    @Pattern(regexp = "READING|COMPLETED") private String readStatus;
    private Boolean favorite;
    @Pattern(regexp = "UNMASTERED|MASTERED") private String masteryLevel;
    @Min(0) private Long durationMs;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }
    public Boolean getFavorite() { return favorite; }
    public void setFavorite(Boolean favorite) { this.favorite = favorite; }
    public String getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(String masteryLevel) { this.masteryLevel = masteryLevel; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
