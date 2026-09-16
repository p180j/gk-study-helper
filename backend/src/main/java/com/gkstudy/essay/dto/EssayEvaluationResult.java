package com.gkstudy.essay.dto;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public class EssayEvaluationResult {
    private final String evaluator;
    private final double totalScore;
    private final Map<String, Double> dimensionScores;
    private final List<String> strengths;
    private final List<String> problems;
    private final List<String> missingPoints;
    private final List<String> suggestions;
    private final Map<String, Object> evidence;
    private final String provider;
    private final String model;
    private final String promptVersion;
    private final LocalDateTime requestTime;
    private final Long latencyMs;
    private final String status;
    private final Double confidence;
    private final String rawResponse;

    public EssayEvaluationResult(String evaluator, double totalScore, Map<String, Double> dimensionScores, List<String> strengths,
                                  List<String> problems, List<String> missingPoints, List<String> suggestions, Map<String, Object> evidence) {
        this.evaluator = evaluator; this.totalScore = totalScore; this.dimensionScores = dimensionScores;
        this.strengths = strengths; this.problems = problems; this.missingPoints = missingPoints;
        this.suggestions = suggestions; this.evidence = evidence;
        this.provider = "LOCAL_RULE"; this.model = null; this.promptVersion = "LOCAL_RULE_V1";
        this.requestTime = LocalDateTime.now(); this.latencyMs = 0L; this.status = "SUCCESS";
        this.confidence = 100.0; this.rawResponse = null;
    }

    public EssayEvaluationResult(String evaluator, double totalScore, Map<String, Double> dimensionScores, List<String> strengths,
                                 List<String> problems, List<String> missingPoints, List<String> suggestions, Map<String, Object> evidence,
                                 String provider, String model, String promptVersion, LocalDateTime requestTime, Long latencyMs,
                                 String status, Double confidence, String rawResponse) {
        this.evaluator = evaluator; this.totalScore = totalScore; this.dimensionScores = dimensionScores;
        this.strengths = strengths; this.problems = problems; this.missingPoints = missingPoints;
        this.suggestions = suggestions; this.evidence = evidence; this.provider = provider; this.model = model;
        this.promptVersion = promptVersion; this.requestTime = requestTime; this.latencyMs = latencyMs;
        this.status = status; this.confidence = confidence; this.rawResponse = rawResponse;
    }

    public String getEvaluator() { return evaluator; }
    public double getTotalScore() { return totalScore; }
    public Map<String, Double> getDimensionScores() { return dimensionScores; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getProblems() { return problems; }
    public List<String> getMissingPoints() { return missingPoints; }
    public List<String> getSuggestions() { return suggestions; }
    public Map<String, Object> getEvidence() { return evidence; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public Long getLatencyMs() { return latencyMs; }
    public String getStatus() { return status; }
    public Double getConfidence() { return confidence; }
    public String getRawResponse() { return rawResponse; }
}
