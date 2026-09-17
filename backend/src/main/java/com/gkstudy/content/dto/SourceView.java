package com.gkstudy.content.dto;

import com.gkstudy.content.ContentLabels;
import com.gkstudy.content.model.ContentSource;

import java.time.LocalDateTime;

public class SourceView {
    private Long id;
    private String name;
    private String baseUrl;
    private String sourceType;
    private String sourceTypeText;
    private String examType;
    private String trustLevel;
    private String trustText;
    private boolean enabled;
    private String crawlStrategy;
    private LocalDateTime lastCrawlTime;
    private String status;

    public static SourceView of(ContentSource s) {
        SourceView view = new SourceView();
        view.id = s.getId(); view.name = s.getName(); view.baseUrl = s.getBaseUrl();
        view.sourceType = s.getSourceType(); view.sourceTypeText = ContentLabels.siteType(s.getSourceType());
        view.examType = s.getExamType(); view.trustLevel = s.getTrustLevel();
        view.trustText = ContentLabels.trust(s.getTrustLevel());
        view.enabled = s.isEnabled(); view.crawlStrategy = s.getCrawlStrategy();
        view.lastCrawlTime = s.getLastCrawlTime(); view.status = s.getStatus();
        return view;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public String getSourceType() { return sourceType; }
    public String getSourceTypeText() { return sourceTypeText; }
    public String getExamType() { return examType; }
    public String getTrustLevel() { return trustLevel; }
    public String getTrustText() { return trustText; }
    public boolean isEnabled() { return enabled; }
    public String getCrawlStrategy() { return crawlStrategy; }
    public LocalDateTime getLastCrawlTime() { return lastCrawlTime; }
    public String getStatus() { return status; }
}
