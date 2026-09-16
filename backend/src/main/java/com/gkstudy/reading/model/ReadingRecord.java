package com.gkstudy.reading.model;

import java.time.LocalDateTime;

public class ReadingRecord {
    private Long id;
    private Long userId;
    private Long materialId;
    private String readStatus;
    private Boolean favorite;
    private String masteryLevel;
    private Long durationMs;
    private LocalDateTime firstReadTime;
    private LocalDateTime lastReadTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public LocalDateTime getFirstReadTime() { return firstReadTime; }
    public void setFirstReadTime(LocalDateTime firstReadTime) { this.firstReadTime = firstReadTime; }
    public LocalDateTime getLastReadTime() { return lastReadTime; }
    public void setLastReadTime(LocalDateTime lastReadTime) { this.lastReadTime = lastReadTime; }
}
