package com.gkstudy.content.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/** 题库采集中心的统一题目候选：网页抽取 / 文件上传 / 人工修正共用 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionCandidate {
    private String stem;
    private List<Option> options = new ArrayList<>();
    private String answer;
    private String analysis;
    private String knowledgeCode;
    private String knowledgePointName;
    private String sourceType;
    private String usageType;
    private String difficultyExpected;
    private Integer standardTimeSeconds;

    public static class Option {
        private String key;
        private String text;

        public Option() { }
        public Option(String key, String text) { this.key = key; this.text = text; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options == null ? new ArrayList<>() : options; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public String getKnowledgeCode() { return knowledgeCode; }
    public void setKnowledgeCode(String knowledgeCode) { this.knowledgeCode = knowledgeCode; }
    public String getKnowledgePointName() { return knowledgePointName; }
    public void setKnowledgePointName(String knowledgePointName) { this.knowledgePointName = knowledgePointName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public String getDifficultyExpected() { return difficultyExpected; }
    public void setDifficultyExpected(String difficultyExpected) { this.difficultyExpected = difficultyExpected; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public void setStandardTimeSeconds(Integer standardTimeSeconds) { this.standardTimeSeconds = standardTimeSeconds; }
}
