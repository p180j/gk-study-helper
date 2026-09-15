package com.gkstudy.learningproblem.engine;

import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.learningproblem.model.LearningProblem;
import com.gkstudy.practice.model.AnswerRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class LearningProblemEngine {
    public static final String MASTERY = "MASTERY";
    public static final String SPEED = "SPEED";
    public static final String STABILITY = "STABILITY";
    private static final int RECENT_SIZE = 6;
    private static final int PERSISTENCE_SIZE = 4;
    private static final int MIN_CONFIRM_SAMPLES = 4;
    private static final double MIN_CONFIRM_CONFIDENCE = 30.0;
    private static final double WEAK_SCORE = 45.0;
    private static final double VERIFY_SCORE = 50.0;
    private static final double RESOLVED_SCORE = 60.0;
    private static final int REQUIRED_VALIDATION_PASSES = 3;

    public Evaluation evaluate(String problemType, LearningProblem current, AbilityProfile profile, List<AnswerRecord> records) {
        List<AnswerRecord> recent = tail(records, RECENT_SIZE);
        List<AnswerRecord> persistence = tail(recent, PERSISTENCE_SIZE);
        double metric = metric(problemType, profile);
        int incorrect = countIncorrect(recent);
        int slow = countSlow(recent);
        int transitions = countTransitions(recent);
        int abnormal = abnormalCount(problemType, persistence);
        int consecutivePasses = consecutivePasses(problemType, recent);
        boolean persistent = MASTERY.equals(problemType) ? abnormal >= 3
                : SPEED.equals(problemType) ? abnormal >= 3 : countTransitions(persistence) >= 2;
        if (current == null && metric >= WEAK_SCORE && !persistent) return null;

        int validationCount = current == null || current.getValidationCount() == null ? 0 : current.getValidationCount();
        int validationPassCount = current == null || current.getValidationPassCount() == null ? 0 : current.getValidationPassCount();
        String status = current == null ? initialStatus(profile, metric, persistent) : nextStatus(current.getStatus(), profile, metric, persistent, consecutivePasses);
        AnswerRecord latest = recent.isEmpty() ? null : recent.get(recent.size() - 1);
        if ("VERIFYING".equals(status) && latest != null && "VALIDATION".equals(latest.getPracticeType())) {
            if (passes(problemType, latest)) {
                validationCount++;
                validationPassCount++;
            } else {
                validationCount = 0;
                validationPassCount = 0;
            }
            if (validationPassCount >= REQUIRED_VALIDATION_PASSES && resolved(profile, metric, consecutivePasses)) status = "RESOLVED";
        }
        if (!"VERIFYING".equals(status) && !"RESOLVED".equals(status)) {
            validationCount = 0;
            validationPassCount = 0;
        }
        BigDecimal severity = decimal(Math.max(0.0, 100.0 - metric));
        double confidenceFactor = 0.5 + value(profile.getConfidenceScore()) / 200.0;
        return new Evaluation(status, severity, decimal(severity.doubleValue() * confidenceFactor), metric, recent.size(), incorrect, slow,
                transitions, abnormal, consecutivePasses, validationCount, validationPassCount);
    }

    private String initialStatus(AbilityProfile profile, double metric, boolean persistent) {
        return canConfirm(profile, metric, persistent) ? "CONFIRMED" : "OBSERVING";
    }

    private String nextStatus(String current, AbilityProfile profile, double metric, boolean persistent, int consecutivePasses) {
        if ("RESOLVED".equals(current)) return canConfirm(profile, metric, persistent) ? "REOPENED" : current;
        if ("OBSERVING".equals(current)) return canConfirm(profile, metric, persistent) ? "CONFIRMED" : current;
        if (("CONFIRMED".equals(current) || "REOPENED".equals(current)) && consecutivePasses >= 2) return "PROCESSING";
        if ("PROCESSING".equals(current) && consecutivePasses >= 4 && metric >= VERIFY_SCORE && value(profile.getConfidenceScore()) >= MIN_CONFIRM_CONFIDENCE) return "VERIFYING";
        return current;
    }

    private boolean canConfirm(AbilityProfile profile, double metric, boolean persistent) {
        return profile.getSampleCount() >= MIN_CONFIRM_SAMPLES && value(profile.getConfidenceScore()) >= MIN_CONFIRM_CONFIDENCE
                && metric < WEAK_SCORE && persistent;
    }

    private boolean resolved(AbilityProfile profile, double metric, int consecutivePasses) {
        return metric >= RESOLVED_SCORE && value(profile.getMasteryScore()) >= 55.0 && value(profile.getSpeedScore()) >= 55.0
                && value(profile.getStabilityScore()) >= 55.0 && value(profile.getConfidenceScore()) >= 45.0 && consecutivePasses >= 4;
    }

    private double metric(String type, AbilityProfile profile) {
        if (MASTERY.equals(type)) return value(profile.getMasteryScore());
        if (SPEED.equals(type)) return value(profile.getSpeedScore());
        if (STABILITY.equals(type)) return value(profile.getStabilityScore());
        throw new IllegalArgumentException("不支持的问题类型: " + type);
    }

    private int abnormalCount(String type, List<AnswerRecord> records) {
        int count = 0;
        for (AnswerRecord record : records) {
            if (MASTERY.equals(type) && !Boolean.TRUE.equals(record.getCorrect())) count++;
            if (SPEED.equals(type) && Boolean.TRUE.equals(record.getCorrect()) && isSlow(record)) count++;
        }
        return count;
    }

    private int consecutivePasses(String type, List<AnswerRecord> records) {
        int count = 0;
        for (int index = records.size() - 1; index >= 0; index--) {
            if (!passes(type, records.get(index))) break;
            count++;
        }
        return count;
    }

    private boolean passes(String type, AnswerRecord record) {
        if (MASTERY.equals(type) || STABILITY.equals(type)) return Boolean.TRUE.equals(record.getCorrect());
        return Boolean.TRUE.equals(record.getCorrect()) && !isSlow(record);
    }

    private int countIncorrect(List<AnswerRecord> records) {
        int count = 0;
        for (AnswerRecord record : records) if (!Boolean.TRUE.equals(record.getCorrect())) count++;
        return count;
    }

    private int countSlow(List<AnswerRecord> records) {
        int count = 0;
        for (AnswerRecord record : records) if (isSlow(record)) count++;
        return count;
    }

    private int countTransitions(List<AnswerRecord> records) {
        int count = 0;
        for (int index = 1; index < records.size(); index++) if (!records.get(index).getCorrect().equals(records.get(index - 1).getCorrect())) count++;
        return count;
    }

    private boolean isSlow(AnswerRecord record) {
        return record.getDurationMs() != null && record.getStandardTimeSecondsSnapshot() != null
                && record.getDurationMs() > record.getStandardTimeSecondsSnapshot() * 1500L;
    }

    private List<AnswerRecord> tail(List<AnswerRecord> records, int size) {
        if (records == null || records.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(records.subList(Math.max(0, records.size() - size), records.size()));
    }

    private BigDecimal decimal(double value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP); }
    private double value(BigDecimal value) { return value == null ? 0.0 : value.doubleValue(); }

    public static class Evaluation {
        private final String status;
        private final BigDecimal severity;
        private final BigDecimal priorityScore;
        private final double metric;
        private final int recentCount;
        private final int incorrectCount;
        private final int slowCount;
        private final int transitionCount;
        private final int abnormalCount;
        private final int consecutivePasses;
        private final int validationCount;
        private final int validationPassCount;

        public Evaluation(String status, BigDecimal severity, BigDecimal priorityScore, double metric, int recentCount, int incorrectCount,
                          int slowCount, int transitionCount, int abnormalCount, int consecutivePasses, int validationCount, int validationPassCount) {
            this.status = status; this.severity = severity; this.priorityScore = priorityScore; this.metric = metric; this.recentCount = recentCount;
            this.incorrectCount = incorrectCount; this.slowCount = slowCount; this.transitionCount = transitionCount; this.abnormalCount = abnormalCount;
            this.consecutivePasses = consecutivePasses; this.validationCount = validationCount; this.validationPassCount = validationPassCount;
        }

        public String getStatus() { return status; }
        public BigDecimal getSeverity() { return severity; }
        public BigDecimal getPriorityScore() { return priorityScore; }
        public double getMetric() { return metric; }
        public int getRecentCount() { return recentCount; }
        public int getIncorrectCount() { return incorrectCount; }
        public int getSlowCount() { return slowCount; }
        public int getTransitionCount() { return transitionCount; }
        public int getAbnormalCount() { return abnormalCount; }
        public int getConsecutivePasses() { return consecutivePasses; }
        public int getValidationCount() { return validationCount; }
        public int getValidationPassCount() { return validationPassCount; }
    }
}
