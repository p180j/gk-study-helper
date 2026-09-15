package com.gkstudy.question.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Question {
    private Long id;
    private String questionType;
    private String stem;
    private String answer;
    private String analysis;
    private BigDecimal difficultyExpected;
    private Integer standardTimeSeconds;
    private String sourceType;
    private Integer sourceYear;
    private String sourceExam;
    private String sourceName;
    private String usageType;
    private String status;
    private Integer version;
    private String contentHash;
    private LocalDateTime createTime;
    private List<QuestionOption> options = new ArrayList<>();
    private List<KnowledgePointRef> knowledgePoints = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public BigDecimal getDifficultyExpected() { return difficultyExpected; }
    public void setDifficultyExpected(BigDecimal difficultyExpected) { this.difficultyExpected = difficultyExpected; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public void setStandardTimeSeconds(Integer standardTimeSeconds) { this.standardTimeSeconds = standardTimeSeconds; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Integer getSourceYear() { return sourceYear; }
    public void setSourceYear(Integer sourceYear) { this.sourceYear = sourceYear; }
    public String getSourceExam() { return sourceExam; }
    public void setSourceExam(String sourceExam) { this.sourceExam = sourceExam; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<QuestionOption> getOptions() { return options; }
    public void setOptions(List<QuestionOption> options) { this.options = options; }
    public List<KnowledgePointRef> getKnowledgePoints() { return knowledgePoints; }
    public void setKnowledgePoints(List<KnowledgePointRef> knowledgePoints) { this.knowledgePoints = knowledgePoints; }
}
