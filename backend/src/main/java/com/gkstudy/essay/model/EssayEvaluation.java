package com.gkstudy.essay.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EssayEvaluation {
    private Long id;
    private Long essayAnswerId;
    private String evaluator;
    private String provider;
    private String model;
    private String promptVersion;
    private LocalDateTime requestTime;
    private Long latencyMs;
    private String status;
    private BigDecimal confidence;
    private String rawResponse;
    private String failureMessage;
    private BigDecimal totalScore;
    private String dimensionScoresJson;
    private String strengthsJson;
    private String problemsJson;
    private String missingPointsJson;
    private String evidenceJson;
    private String suggestionsJson;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEssayAnswerId() { return essayAnswerId; }
    public void setEssayAnswerId(Long essayAnswerId) { this.essayAnswerId = essayAnswerId; }
    public String getEvaluator() { return evaluator; }
    public void setEvaluator(String evaluator) { this.evaluator = evaluator; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getDimensionScoresJson() { return dimensionScoresJson; }
    public void setDimensionScoresJson(String dimensionScoresJson) { this.dimensionScoresJson = dimensionScoresJson; }
    public String getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(String strengthsJson) { this.strengthsJson = strengthsJson; }
    public String getProblemsJson() { return problemsJson; }
    public void setProblemsJson(String problemsJson) { this.problemsJson = problemsJson; }
    public String getMissingPointsJson() { return missingPointsJson; }
    public void setMissingPointsJson(String missingPointsJson) { this.missingPointsJson = missingPointsJson; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getSuggestionsJson() { return suggestionsJson; }
    public void setSuggestionsJson(String suggestionsJson) { this.suggestionsJson = suggestionsJson; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
