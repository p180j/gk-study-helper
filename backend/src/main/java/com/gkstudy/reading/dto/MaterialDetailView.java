package com.gkstudy.reading.dto;

import java.time.LocalDate;
import java.util.List;

public class MaterialDetailView {
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
    private String content;
    private String problem;
    private String cause;
    private String solution;
    private String policyLogic;
    private List<String> standardExpressions;
    private List<String> cases;
    private List<String> applicableEssayThemes;

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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getPolicyLogic() { return policyLogic; }
    public void setPolicyLogic(String policyLogic) { this.policyLogic = policyLogic; }
    public List<String> getStandardExpressions() { return standardExpressions; }
    public void setStandardExpressions(List<String> standardExpressions) { this.standardExpressions = standardExpressions; }
    public List<String> getCases() { return cases; }
    public void setCases(List<String> cases) { this.cases = cases; }
    public List<String> getApplicableEssayThemes() { return applicableEssayThemes; }
    public void setApplicableEssayThemes(List<String> applicableEssayThemes) { this.applicableEssayThemes = applicableEssayThemes; }
}
