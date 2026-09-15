package com.gkstudy.ability.engine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AbilityEvent {
    private boolean correct;
    private BigDecimal difficulty;
    private long durationMs;
    private int standardTimeSeconds;
    private String answerConfidenceType;
    private String practiceType;
    private BigDecimal knowledgeWeight;
    private LocalDateTime answerTime;
    private List<Boolean> recentCorrectness = new ArrayList<>();
    private List<BigDecimal> recentDifficulties = new ArrayList<>();
    private List<String> recentPracticeTypes = new ArrayList<>();

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public BigDecimal getDifficulty() { return difficulty; }
    public void setDifficulty(BigDecimal difficulty) { this.difficulty = difficulty; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public int getStandardTimeSeconds() { return standardTimeSeconds; }
    public void setStandardTimeSeconds(int standardTimeSeconds) { this.standardTimeSeconds = standardTimeSeconds; }
    public String getAnswerConfidenceType() { return answerConfidenceType; }
    public void setAnswerConfidenceType(String answerConfidenceType) { this.answerConfidenceType = answerConfidenceType; }
    public String getPracticeType() { return practiceType; }
    public void setPracticeType(String practiceType) { this.practiceType = practiceType; }
    public BigDecimal getKnowledgeWeight() { return knowledgeWeight; }
    public void setKnowledgeWeight(BigDecimal knowledgeWeight) { this.knowledgeWeight = knowledgeWeight; }
    public LocalDateTime getAnswerTime() { return answerTime; }
    public void setAnswerTime(LocalDateTime answerTime) { this.answerTime = answerTime; }
    public List<Boolean> getRecentCorrectness() { return recentCorrectness; }
    public void setRecentCorrectness(List<Boolean> recentCorrectness) { this.recentCorrectness = recentCorrectness; }
    public List<BigDecimal> getRecentDifficulties() { return recentDifficulties; }
    public void setRecentDifficulties(List<BigDecimal> recentDifficulties) { this.recentDifficulties = recentDifficulties; }
    public List<String> getRecentPracticeTypes() { return recentPracticeTypes; }
    public void setRecentPracticeTypes(List<String> recentPracticeTypes) { this.recentPracticeTypes = recentPracticeTypes; }
}
