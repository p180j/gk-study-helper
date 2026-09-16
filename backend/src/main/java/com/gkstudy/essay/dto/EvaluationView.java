package com.gkstudy.essay.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class EvaluationView {
    private final String evaluator;
    private final BigDecimal totalScore;
    private final List<DimensionScore> dimensionScores;
    private final List<String> strengths;
    private final List<String> problems;
    private final List<String> missingPoints;
    private final List<String> suggestions;
    private final Map<String, Object> evidence;
    private final String provider;
    private final String model;
    private final String promptVersion;
    private final String status;
    private final BigDecimal confidence;

    public EvaluationView(String evaluator, BigDecimal totalScore, List<DimensionScore> dimensionScores, List<String> strengths,
                          List<String> problems, List<String> missingPoints, List<String> suggestions, Map<String, Object> evidence) {
        this.evaluator = evaluator; this.totalScore = totalScore; this.dimensionScores = dimensionScores;
        this.strengths = strengths; this.problems = problems; this.missingPoints = missingPoints;
        this.suggestions = suggestions; this.evidence = evidence;
        this.provider = null; this.model = null; this.promptVersion = null; this.status = "SUCCESS"; this.confidence = null;
    }

    public EvaluationView(String evaluator, BigDecimal totalScore, List<DimensionScore> dimensionScores, List<String> strengths,
                          List<String> problems, List<String> missingPoints, List<String> suggestions, Map<String, Object> evidence,
                          String provider, String model, String promptVersion, String status, BigDecimal confidence) {
        this.evaluator = evaluator; this.totalScore = totalScore; this.dimensionScores = dimensionScores;
        this.strengths = strengths; this.problems = problems; this.missingPoints = missingPoints;
        this.suggestions = suggestions; this.evidence = evidence; this.provider = provider; this.model = model;
        this.promptVersion = promptVersion; this.status = status; this.confidence = confidence;
    }

    public String getEvaluator() { return evaluator; }
    public BigDecimal getTotalScore() { return totalScore; }
    public List<DimensionScore> getDimensionScores() { return dimensionScores; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getProblems() { return problems; }
    public List<String> getMissingPoints() { return missingPoints; }
    public List<String> getSuggestions() { return suggestions; }
    public Map<String, Object> getEvidence() { return evidence; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public String getStatus() { return status; }
    public BigDecimal getConfidence() { return confidence; }
}
