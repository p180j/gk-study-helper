package com.gkstudy.question.dto;

import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionResponse {
    private final Long id;
    private final String questionType;
    private final String stem;
    private final BigDecimal difficulty;
    private final Integer standardTimeSeconds;
    private final String sourceType;
    private final Integer sourceYear;
    private final String sourceExam;
    private final String sourceName;
    private final String usageType;
    private final String status;
    private final Integer version;
    private final List<QuestionOption> options;
    private final List<KnowledgePointRef> knowledgePoints;

    private QuestionResponse(Question question) {
        id = question.getId(); questionType = question.getQuestionType(); stem = question.getStem();
        difficulty = question.getDifficultyExpected(); standardTimeSeconds = question.getStandardTimeSeconds();
        sourceType = question.getSourceType(); sourceYear = question.getSourceYear(); sourceExam = question.getSourceExam(); sourceName = question.getSourceName();
        usageType = question.getUsageType(); status = question.getStatus(); version = question.getVersion();
        options = question.getOptions(); knowledgePoints = question.getKnowledgePoints();
    }

    public static QuestionResponse from(Question question) { return new QuestionResponse(question); }
    public static List<QuestionResponse> from(List<Question> questions) { return questions.stream().map(QuestionResponse::from).collect(Collectors.toList()); }
    public Long getId() { return id; }
    public String getQuestionType() { return questionType; }
    public String getStem() { return stem; }
    public BigDecimal getDifficulty() { return difficulty; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public String getSourceType() { return sourceType; }
    public Integer getSourceYear() { return sourceYear; }
    public String getSourceExam() { return sourceExam; }
    public String getSourceName() { return sourceName; }
    public String getUsageType() { return usageType; }
    public String getStatus() { return status; }
    public Integer getVersion() { return version; }
    public List<QuestionOption> getOptions() { return options; }
    public List<KnowledgePointRef> getKnowledgePoints() { return knowledgePoints; }
}
