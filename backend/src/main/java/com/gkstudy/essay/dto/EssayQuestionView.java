package com.gkstudy.essay.dto;

public class EssayQuestionView {
    private final Long id;
    private final String topicCode;
    private final String topicName;
    private final String questionType;
    private final String questionTypeName;
    private final String material;
    private final String prompt;
    private final Integer wordLimitMin;
    private final Integer wordLimitMax;
    private final Integer standardTimeSeconds;

    public EssayQuestionView(Long id, String topicCode, String topicName, String questionType, String questionTypeName,
                             String material, String prompt, Integer wordLimitMin, Integer wordLimitMax, Integer standardTimeSeconds) {
        this.id = id; this.topicCode = topicCode; this.topicName = topicName; this.questionType = questionType;
        this.questionTypeName = questionTypeName; this.material = material; this.prompt = prompt;
        this.wordLimitMin = wordLimitMin; this.wordLimitMax = wordLimitMax; this.standardTimeSeconds = standardTimeSeconds;
    }

    public Long getId() { return id; }
    public String getTopicCode() { return topicCode; }
    public String getTopicName() { return topicName; }
    public String getQuestionType() { return questionType; }
    public String getQuestionTypeName() { return questionTypeName; }
    public String getMaterial() { return material; }
    public String getPrompt() { return prompt; }
    public Integer getWordLimitMin() { return wordLimitMin; }
    public Integer getWordLimitMax() { return wordLimitMax; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
}
