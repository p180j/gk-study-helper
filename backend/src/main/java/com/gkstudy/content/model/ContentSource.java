package com.gkstudy.content.model;

import java.time.LocalDateTime;

public class ContentSource {
    private Long id;
    private String name;
    private String baseUrl;
    private String sourceType;
    private String examType;
    private String trustLevel;
    private boolean enabled;
    private String crawlStrategy;
    private LocalDateTime lastCrawlTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public String getTrustLevel() { return trustLevel; }
    public void setTrustLevel(String trustLevel) { this.trustLevel = trustLevel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCrawlStrategy() { return crawlStrategy; }
    public void setCrawlStrategy(String crawlStrategy) { this.crawlStrategy = crawlStrategy; }
    public LocalDateTime getLastCrawlTime() { return lastCrawlTime; }
    public void setLastCrawlTime(LocalDateTime lastCrawlTime) { this.lastCrawlTime = lastCrawlTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
