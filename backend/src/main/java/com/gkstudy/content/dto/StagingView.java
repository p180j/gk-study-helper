package com.gkstudy.content.dto;

import com.gkstudy.content.ContentLabels;
import com.gkstudy.content.model.ContentStaging;

import java.time.LocalDateTime;

/** 管理端展示视图：内部状态统一映射为中文 */
public class StagingView {
    private Long id;
    private Long sourceId;
    private String sourceName;
    private String sourceUrl;
    private String title;
    private String status;
    private String statusText;
    private String sourceTypeText;
    private String trustText;
    private String failReason;
    private String importedType;
    private Long importedId;
    private String reviewNote;
    private Integer sourceYear;
    private LocalDateTime crawlTime;
    private LocalDateTime publishTime;

    public static StagingView of(ContentStaging s) {
        StagingView view = new StagingView();
        view.id = s.getId(); view.sourceId = s.getSourceId(); view.sourceName = s.getSourceName();
        view.sourceUrl = s.getSourceUrl(); view.title = s.getTitle(); view.status = s.getStatus();
        view.statusText = ContentLabels.status(s.getStatus());
        view.sourceTypeText = ContentLabels.sourceType(s.getSourceType());
        view.trustText = ContentLabels.trust(s.getTrustLevel());
        view.failReason = s.getFailReason(); view.importedType = s.getImportedType(); view.importedId = s.getImportedId();
        view.reviewNote = s.getReviewNote(); view.sourceYear = s.getSourceYear();
        view.crawlTime = s.getCrawlTime(); view.publishTime = s.getPublishTime();
        return view;
    }

    public Long getId() { return id; }
    public Long getSourceId() { return sourceId; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public String getSourceTypeText() { return sourceTypeText; }
    public String getTrustText() { return trustText; }
    public String getFailReason() { return failReason; }
    public String getImportedType() { return importedType; }
    public Long getImportedId() { return importedId; }
    public String getReviewNote() { return reviewNote; }
    public Integer getSourceYear() { return sourceYear; }
    public LocalDateTime getCrawlTime() { return crawlTime; }
    public LocalDateTime getPublishTime() { return publishTime; }
}
