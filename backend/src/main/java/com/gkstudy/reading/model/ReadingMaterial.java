package com.gkstudy.reading.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ReadingMaterial {
    private Long id;
    private Long topicId;
    private String title;
    private String source;
    private LocalDate publishDate;
    private String content;
    private String coreView;
    private String problem;
    private String cause;
    private String solution;
    private String policyLogic;
    private String standardExpressionsJson;
    private String casesJson;
    private String applicableEssayThemesJson;
    private String topicCandidatesJson;
    private BigDecimal aiConfidence;
    private String aiProvider;
    private String aiModel;
    private String aiPromptVersion;
    private String aiStatus;
    private String aiRawResponse;
    private LocalDateTime aiRequestTime;
    private Long aiLatencyMs;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String topicCode;
    private String topicName;
    private String readStatus;
    private Boolean favorite;
    private String masteryLevel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCoreView() { return coreView; }
    public void setCoreView(String coreView) { this.coreView = coreView; }
    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getPolicyLogic() { return policyLogic; }
    public void setPolicyLogic(String policyLogic) { this.policyLogic = policyLogic; }
    public String getStandardExpressionsJson() { return standardExpressionsJson; }
    public void setStandardExpressionsJson(String standardExpressionsJson) { this.standardExpressionsJson = standardExpressionsJson; }
    public String getCasesJson() { return casesJson; }
    public void setCasesJson(String casesJson) { this.casesJson = casesJson; }
    public String getApplicableEssayThemesJson() { return applicableEssayThemesJson; }
    public void setApplicableEssayThemesJson(String applicableEssayThemesJson) { this.applicableEssayThemesJson = applicableEssayThemesJson; }
    public String getTopicCandidatesJson() { return topicCandidatesJson; }
    public void setTopicCandidatesJson(String topicCandidatesJson) { this.topicCandidatesJson = topicCandidatesJson; }
    public BigDecimal getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(BigDecimal aiConfidence) { this.aiConfidence = aiConfidence; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getAiPromptVersion() { return aiPromptVersion; }
    public void setAiPromptVersion(String aiPromptVersion) { this.aiPromptVersion = aiPromptVersion; }
    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }
    public String getAiRawResponse() { return aiRawResponse; }
    public void setAiRawResponse(String aiRawResponse) { this.aiRawResponse = aiRawResponse; }
    public LocalDateTime getAiRequestTime() { return aiRequestTime; }
    public void setAiRequestTime(LocalDateTime aiRequestTime) { this.aiRequestTime = aiRequestTime; }
    public Long getAiLatencyMs() { return aiLatencyMs; }
    public void setAiLatencyMs(Long aiLatencyMs) { this.aiLatencyMs = aiLatencyMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }
    public Boolean getFavorite() { return favorite; }
    public void setFavorite(Boolean favorite) { this.favorite = favorite; }
    public String getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(String masteryLevel) { this.masteryLevel = masteryLevel; }
}
