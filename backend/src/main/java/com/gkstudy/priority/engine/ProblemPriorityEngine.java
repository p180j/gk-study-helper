package com.gkstudy.priority.engine;

import com.gkstudy.learningproblem.model.LearningProblem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class ProblemPriorityEngine {
    private static final double SEVERITY_WEIGHT = 0.22;
    private static final double EXAM_IMPORTANCE_WEIGHT = 0.18;
    private static final double PERSISTENCE_WEIGHT = 0.13;
    private static final double IMPROVEMENT_POTENTIAL_WEIGHT = 0.17;
    private static final double CONFIDENCE_WEIGHT = 0.12;
    private static final double TRANSFER_VALUE_WEIGHT = 0.10;
    private static final double STATUS_WEIGHT = 0.08;
    private static final double VERIFYING_BONUS = 8.0;
    private static final double OBSERVING_FACTOR = 0.55;
    private static final int CORE_LIMIT = 3;
    private static final Map<String, Double> STATUS_SCORES;

    static {
        Map<String, Double> scores = new HashMap<>();
        scores.put("VERIFYING", 100.0); scores.put("REOPENED", 95.0); scores.put("CONFIRMED", 90.0);
        scores.put("PROCESSING", 85.0); scores.put("OBSERVING", 30.0); scores.put("RESOLVED", 0.0);
        STATUS_SCORES = Collections.unmodifiableMap(scores);
    }

    public List<LearningProblem> rank(List<LearningProblem> problems, LocalDateTime now) {
        List<LearningProblem> ranked = new ArrayList<>();
        for (LearningProblem problem : problems) {
            if ("RESOLVED".equals(problem.getStatus())) continue;
            problem.setPriorityScore(calculate(problem, now));
            ranked.add(problem);
        }
        ranked.sort(Comparator.comparing(LearningProblem::getPriorityScore).reversed().thenComparing(LearningProblem::getId));
        return ranked;
    }

    public List<LearningProblem> core(List<LearningProblem> problems, LocalDateTime now) {
        List<LearningProblem> ranked = rank(problems, now);
        return new ArrayList<>(ranked.subList(0, Math.min(CORE_LIMIT, ranked.size())));
    }

    public BigDecimal calculate(LearningProblem problem, LocalDateTime now) {
        double score = value(problem.getSeverity(), 0.0) * SEVERITY_WEIGHT
                + percent(problem.getExamImportance()) * EXAM_IMPORTANCE_WEIGHT
                + persistence(problem, now) * PERSISTENCE_WEIGHT
                + percent(problem.getImprovementPotential()) * IMPROVEMENT_POTENTIAL_WEIGHT
                + value(problem.getAbilityConfidence(), 0.0) * CONFIDENCE_WEIGHT
                + percent(problem.getTransferValue()) * TRANSFER_VALUE_WEIGHT
                + STATUS_SCORES.getOrDefault(problem.getStatus(), 0.0) * STATUS_WEIGHT;
        if ("VERIFYING".equals(problem.getStatus())) score += VERIFYING_BONUS;
        if ("OBSERVING".equals(problem.getStatus())) score *= OBSERVING_FACTOR;
        return BigDecimal.valueOf(Math.min(100.0, Math.max(0.0, score))).setScale(2, RoundingMode.HALF_UP);
    }

    private double persistence(LearningProblem problem, LocalDateTime now) {
        if (problem.getDiscoveredTime() == null || now == null) return 40.0;
        long days = Math.max(0, Duration.between(problem.getDiscoveredTime(), now).toDays());
        return Math.min(100.0, 40.0 + days * 5.0);
    }

    private double percent(BigDecimal value) { return Math.min(100.0, Math.max(0.0, value(value, 0.5) * 100.0)); }
    private double value(BigDecimal value, double fallback) { return value == null ? fallback : value.doubleValue(); }
}
