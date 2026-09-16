package com.gkstudy.essay.engine;

import com.gkstudy.learningproblem.model.LearningProblem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 申论学习问题引擎：评分驱动的 OBSERVING/CONFIRMED/VERIFYING/RESOLVED/REOPENED 状态机。
 * 与行测不同，申论单维度样本量少：2 个低分样本即确认问题，
 * 连续 2 次达标（>=60）即解决，55-60 之间进入验证观察。
 */
@Component
public class EssayProblemEngine {
    private static final double WEAK_SCORE = 45.0;
    private static final double VERIFY_SCORE = 55.0;
    private static final double RESOLVED_SCORE = 60.0;
    private static final int REQUIRED_PASSES = 2;

    public Evaluation evaluate(String problemType, LearningProblem current, double dimensionScore, int samples, String evidenceJsonBase) {
        if (current == null && dimensionScore >= WEAK_SCORE) return null;
        BigDecimal severity = decimal(clamp((50.0 - dimensionScore) / 50.0 * 100.0, 20.0, 95.0));
        BigDecimal priorityScore = decimal(clamp(severity.doubleValue() * 0.6 + 40.0 * (1.0 - dimensionScore / 100.0), 0.0, 100.0));
        String status;
        int validationCount = 0;
        int validationPassCount = 0;
        if (current == null) {
            status = samples <= 1 ? "OBSERVING" : "CONFIRMED";
        } else {
            int oldValidationCount = current.getValidationCount() == null ? 0 : current.getValidationCount();
            int oldPassCount = current.getValidationPassCount() == null ? 0 : current.getValidationPassCount();
            validationCount = oldValidationCount + 1;
            if (dimensionScore >= RESOLVED_SCORE) {
                validationPassCount = oldPassCount + 1;
                status = validationPassCount >= REQUIRED_PASSES ? "RESOLVED" : "VERIFYING";
            } else if (dimensionScore >= VERIFY_SCORE) {
                status = "VERIFYING";
            } else if (dimensionScore < WEAK_SCORE) {
                status = relapse(current.getStatus(), samples);
            } else {
                status = current.getStatus();
            }
        }
        return new Evaluation(status, severity, priorityScore, validationCount, validationPassCount, evidenceJsonBase);
    }

    private String relapse(String currentStatus, int samples) {
        if ("RESOLVED".equals(currentStatus)) return "REOPENED";
        if ("OBSERVING".equals(currentStatus) && samples >= 2) return "CONFIRMED";
        return currentStatus;
    }

    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private BigDecimal decimal(double value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP); }

    public static class Evaluation {
        private final String status;
        private final BigDecimal severity;
        private final BigDecimal priorityScore;
        private final int validationCount;
        private final int validationPassCount;
        private final String evidenceJson;

        public Evaluation(String status, BigDecimal severity, BigDecimal priorityScore, int validationCount,
                          int validationPassCount, String evidenceJson) {
            this.status = status; this.severity = severity; this.priorityScore = priorityScore;
            this.validationCount = validationCount; this.validationPassCount = validationPassCount;
            this.evidenceJson = evidenceJson;
        }

        public String getStatus() { return status; }
        public BigDecimal getSeverity() { return severity; }
        public BigDecimal getPriorityScore() { return priorityScore; }
        public int getValidationCount() { return validationCount; }
        public int getValidationPassCount() { return validationPassCount; }
        public String getEvidenceJson() { return evidenceJson; }
    }
}
