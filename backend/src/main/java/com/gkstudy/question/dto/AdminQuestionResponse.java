package com.gkstudy.question.dto;

import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminQuestionResponse {
    private final Long id;
    private final String questionType;
    private final String stem;
    private final String answer;
    private final String analysis;
    private final BigDecimal difficulty;
    private final Integer standardTimeSeconds;
    private final String sourceType;
    private final Integer sourceYear;
    private final String sourceExam;
    private final String sourceName;
    private final String usageType;
    private final String status;
    private final Integer version;
    private final LocalDateTime createTime;
    private final List<QuestionOption> options;
    private final List<KnowledgePointRef> knowledgePoints;

    private AdminQuestionResponse(Question question) {
        id = question.getId(); questionType = question.getQuestionType(); stem = question.getStem();
        answer = question.getAnswer(); analysis = question.getAnalysis(); difficulty = question.getDifficultyExpected();
        standardTimeSeconds = question.getStandardTimeSeconds(); sourceType = question.getSourceType();
        sourceYear = question.getSourceYear(); sourceExam = question.getSourceExam(); sourceName = question.getSourceName();
        usageType = question.getUsageType(); status = question.getStatus(); version = question.getVersion();
        createTime = question.getCreateTime(); options = question.getOptions(); knowledgePoints = question.getKnowledgePoints();
    }

    public static AdminQuestionResponse from(Question question) { return new AdminQuestionResponse(question); }
    public Long getId() { return id; }
    public String getQuestionType() { return questionType; }
    public String getStem() { return stem; }
    public String getAnswer() { return answer; }
    public String getAnalysis() { return analysis; }
    public BigDecimal getDifficulty() { return difficulty; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public String getSourceType() { return sourceType; }
    public Integer getSourceYear() { return sourceYear; }
    public String getSourceExam() { return sourceExam; }
    public String getSourceName() { return sourceName; }
    public String getUsageType() { return usageType; }
    public String getStatus() { return status; }
    public Integer getVersion() { return version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public List<QuestionOption> getOptions() { return options; }
    public List<KnowledgePointRef> getKnowledgePoints() { return knowledgePoints; }
}
