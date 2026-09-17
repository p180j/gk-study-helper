package com.gkstudy.content.model;

import java.time.LocalDateTime;

public class ContentStaging {
    public static final String DISCOVERED = "DISCOVERED";
    public static final String DOWNLOADED = "DOWNLOADED";
    public static final String PARSED = "PARSED";
    public static final String DEDUPED = "DEDUPED";
    public static final String READY = "READY";
    public static final String IMPORTED = "IMPORTED";
    public static final String NEEDS_REVIEW = "NEEDS_REVIEW";
    public static final String FAILED = "FAILED";

    private Long id;
    private Long sourceId;
    private String sourceUrl;
    private String siteName;
    private String publishOrg;
    private LocalDateTime publishTime;
    private LocalDateTime crawlTime;
    private String title;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private String contentHash;
    private String examType;
    private Integer sourceYear;
    private String sourceType;
    private String trustLevel;
    private String filePath;
    private String parsedText;
    private String status;
    private String failReason;
    private String importedType;
    private Long importedId;
    private String reviewNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 非数据库字段：关联来源名称 */
    private String sourceName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getPublishOrg() { return publishOrg; }
    public void setPublishOrg(String publishOrg) { this.publishOrg = publishOrg; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public LocalDateTime getCrawlTime() { return crawlTime; }
    public void setCrawlTime(LocalDateTime crawlTime) { this.crawlTime = crawlTime; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public Integer getSourceYear() { return sourceYear; }
    public void setSourceYear(Integer sourceYear) { this.sourceYear = sourceYear; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getTrustLevel() { return trustLevel; }
    public void setTrustLevel(String trustLevel) { this.trustLevel = trustLevel; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getParsedText() { return parsedText; }
    public void setParsedText(String parsedText) { this.parsedText = parsedText; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getImportedType() { return importedType; }
    public void setImportedType(String importedType) { this.importedType = importedType; }
    public Long getImportedId() { return importedId; }
    public void setImportedId(Long importedId) { this.importedId = importedId; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
}
