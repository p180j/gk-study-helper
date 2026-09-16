package com.gkstudy.essay.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;

public class CreateEssayQuestionRequest {
    @NotBlank private String topicCode;
    @NotBlank @Pattern(regexp = "SUMMARY|ANALYSIS|COUNTERMEASURE|IMPLEMENTATION") private String questionType;
    @NotBlank private String material;
    @NotBlank private String prompt;
    @Min(1) private Integer wordLimitMin;
    @Min(1) private Integer wordLimitMax;
    @Min(1) private Integer standardTimeSeconds;
    private String referenceAnswer;
    private List<ReferencePoint> referencePoints;
    private Integer sourceYear;
    private String sourceExam;
    private String sourceName;
    private String status;

    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public Integer getWordLimitMin() { return wordLimitMin; }
    public void setWordLimitMin(Integer wordLimitMin) { this.wordLimitMin = wordLimitMin; }
    public Integer getWordLimitMax() { return wordLimitMax; }
    public void setWordLimitMax(Integer wordLimitMax) { this.wordLimitMax = wordLimitMax; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public void setStandardTimeSeconds(Integer standardTimeSeconds) { this.standardTimeSeconds = standardTimeSeconds; }
    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
    public List<ReferencePoint> getReferencePoints() { return referencePoints; }
    public void setReferencePoints(List<ReferencePoint> referencePoints) { this.referencePoints = referencePoints; }
    public Integer getSourceYear() { return sourceYear; }
    public void setSourceYear(Integer sourceYear) { this.sourceYear = sourceYear; }
    public String getSourceExam() { return sourceExam; }
    public void setSourceExam(String sourceExam) { this.sourceExam = sourceExam; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getStatus() { return status == null || status.trim().isEmpty() ? "ACTIVE" : status; }
    public void setStatus(String status) { this.status = status; }
}
