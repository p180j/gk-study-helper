package com.gkstudy.reading.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class SaveMaterialRequest {
    @NotNull private Long topicId;
    @NotBlank private String title;
    @NotBlank private String content;
    private String source;
    private LocalDate publishDate;
    private String coreView;
    private String problem;
    private String cause;
    private String solution;
    private String policyLogic;
    private List<String> standardExpressions;
    private List<String> cases;
    private List<String> applicableEssayThemes;
    private String status;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }
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
    public List<String> getStandardExpressions() { return standardExpressions; }
    public void setStandardExpressions(List<String> standardExpressions) { this.standardExpressions = standardExpressions; }
    public List<String> getCases() { return cases; }
    public void setCases(List<String> cases) { this.cases = cases; }
    public List<String> getApplicableEssayThemes() { return applicableEssayThemes; }
    public void setApplicableEssayThemes(List<String> applicableEssayThemes) { this.applicableEssayThemes = applicableEssayThemes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
