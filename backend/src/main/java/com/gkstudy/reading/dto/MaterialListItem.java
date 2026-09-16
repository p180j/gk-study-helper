package com.gkstudy.reading.dto;

import java.time.LocalDate;

public class MaterialListItem {
    private Long id;
    private Long topicId;
    private String topicCode;
    private String topicName;
    private String title;
    private String source;
    private LocalDate publishDate;
    private String coreView;
    private String readStatus;
    private Boolean favorite;
    private String masteryLevel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }
    public String getCoreView() { return coreView; }
    public void setCoreView(String coreView) { this.coreView = coreView; }
    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }
    public Boolean getFavorite() { return favorite; }
    public void setFavorite(Boolean favorite) { this.favorite = favorite; }
    public String getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(String masteryLevel) { this.masteryLevel = masteryLevel; }
}
