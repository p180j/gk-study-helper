package com.gkstudy.content.model;

import java.time.LocalDateTime;

/** 一次采集触发的历史记录：异步执行，结束后回写各阶段计数 */
public class ContentCrawlLog {
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private Long id;
    private Long sourceId;
    private String sourceName;
    private String status;
    private int discovered;
    private int downloaded;
    private int parsed;
    private int imported;
    private int duplicates;
    private int needsReview;
    private int failed;
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDiscovered() { return discovered; }
    public void setDiscovered(int discovered) { this.discovered = discovered; }
    public int getDownloaded() { return downloaded; }
    public void setDownloaded(int downloaded) { this.downloaded = downloaded; }
    public int getParsed() { return parsed; }
    public void setParsed(int parsed) { this.parsed = parsed; }
    public int getImported() { return imported; }
    public void setImported(int imported) { this.imported = imported; }
    public int getDuplicates() { return duplicates; }
    public void setDuplicates(int duplicates) { this.duplicates = duplicates; }
    public int getNeedsReview() { return needsReview; }
    public void setNeedsReview(int needsReview) { this.needsReview = needsReview; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
