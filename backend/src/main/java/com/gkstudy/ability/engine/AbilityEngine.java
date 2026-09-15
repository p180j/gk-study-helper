package com.gkstudy.ability.engine;

import com.gkstudy.ability.model.AbilityProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;

@Component
public class AbilityEngine {
    public AbilityProfile calculate(AbilityProfile current, AbilityEvent event) {
        AbilityProfile result = current == null ? initialProfile() : copy(current);
        int previousSamples = result.getSampleCount();
        double weight = clamp(value(event.getKnowledgeWeight(), 1.0), 0.0, 1.0);
        double maturity = Math.max(AbilityConstants.MIN_MATURITY_FACTOR, 1.0 / (1.0 + previousSamples / AbilityConstants.MATURITY_SAMPLE_DIVISOR));
        result.setMasteryScore(score(value(result.getMasteryScore(), AbilityConstants.INITIAL_SCORE) + masteryDelta(event) * weight * maturity));
        double speedTarget = speedTarget(event);
        double speed = value(result.getSpeedScore(), AbilityConstants.INITIAL_SCORE);
        result.setSpeedScore(score(speed + (speedTarget - speed) * AbilityConstants.SPEED_BLEND * weight * maturity));
        double stabilityTarget = stabilityTarget(event.getRecentCorrectness());
        double stability = value(result.getStabilityScore(), AbilityConstants.INITIAL_SCORE);
        result.setStabilityScore(score(stability + (stabilityTarget - stability) * weight));
        result.setSampleCount(previousSamples + 1);
        result.setConfidenceScore(score(confidence(event, result.getSampleCount(), current) * (0.5 + 0.5 * weight)));
        result.setLastPracticeTime(event.getAnswerTime());
        result.setStatus(status(result));
        return result;
    }

    private double masteryDelta(AbilityEvent event) {
        double difficulty = clamp(value(event.getDifficulty(), 50.0) / 100.0, 0.0, 1.0);
        double practiceWeight = AbilityConstants.PRACTICE_WEIGHTS.getOrDefault(event.getPracticeType(), 1.0);
        if (event.isCorrect()) {
            double difficultyFactor = 0.7 + difficulty * 0.6;
            double answerConfidence = AbilityConstants.CORRECT_CONFIDENCE_WEIGHTS.getOrDefault(event.getAnswerConfidenceType(), 0.85);
            return AbilityConstants.CORRECT_MASTERY_DELTA * difficultyFactor * answerConfidence * practiceWeight;
        }
        double difficultyFactor = 1.3 - difficulty * 0.6;
        double evidenceFactor = "GUESS".equals(event.getAnswerConfidenceType()) ? 0.8 : 1.0;
        return AbilityConstants.WRONG_MASTERY_DELTA * difficultyFactor * evidenceFactor * practiceWeight;
    }

    private double speedTarget(AbilityEvent event) {
        if (!event.isCorrect()) return 35.0;
        double ratio = event.getDurationMs() / (event.getStandardTimeSeconds() * 1000.0);
        if (ratio <= 0.75) return 90.0;
        if (ratio <= 1.1) return 72.0;
        if (ratio <= 1.5) return 52.0;
        return 28.0;
    }

    private double stabilityTarget(List<Boolean> results) {
        if (results == null || results.isEmpty()) return AbilityConstants.INITIAL_SCORE;
        int correct = 0;
        int transitions = 0;
        for (int i = 0; i < results.size(); i++) {
            if (Boolean.TRUE.equals(results.get(i))) correct++;
            if (i > 0 && !results.get(i).equals(results.get(i - 1))) transitions++;
        }
        double consistency = results.size() == 1 ? 0.5 : 1.0 - transitions / (double) (results.size() - 1);
        double correctness = correct / (double) results.size();
        return consistency * 60.0 + correctness * 40.0;
    }

    private double confidence(AbilityEvent event, int samples, AbilityProfile previous) {
        double sampleScore = Math.min(70.0, samples * 70.0 / AbilityConstants.CONFIDENCE_FULL_SAMPLE_COUNT);
        double coverage = difficultyCoverage(event.getRecentDifficulties()) * 15.0;
        double sceneScore = Math.min(10.0, new HashSet<>(event.getRecentPracticeTypes()).size() * (10.0 / 3.0));
        double recencyScore = 5.0;
        if (previous != null && previous.getLastPracticeTime() != null && event.getAnswerTime() != null
                && Duration.between(previous.getLastPracticeTime(), event.getAnswerTime()).toDays() > AbilityConstants.RECENT_PRACTICE_DAYS) recencyScore = 0.0;
        return clamp(sampleScore + coverage + sceneScore + recencyScore, 0.0, 100.0);
    }

    private double difficultyCoverage(List<BigDecimal> difficulties) {
        if (difficulties == null || difficulties.size() < 2) return 0.0;
        double min = 100.0;
        double max = 0.0;
        for (BigDecimal difficulty : difficulties) { double value = value(difficulty, 50.0); min = Math.min(min, value); max = Math.max(max, value); }
        return clamp((max - min) / 60.0, 0.0, 1.0);
    }

    private String status(AbilityProfile profile) {
        double confidence = profile.getConfidenceScore().doubleValue();
        double mastery = profile.getMasteryScore().doubleValue();
        if (confidence < 20.0) return "UNASSESSED";
        if (confidence < 45.0) return "LEARNING";
        if (mastery < 45.0) return "WEAK";
        if (mastery >= 80.0) return "PROFICIENT";
        if (mastery >= 65.0) return "MASTERED";
        return "LEARNING";
    }

    private AbilityProfile initialProfile() {
        AbilityProfile profile = new AbilityProfile();
        profile.setMasteryScore(score(AbilityConstants.INITIAL_SCORE)); profile.setSpeedScore(score(AbilityConstants.INITIAL_SCORE));
        profile.setStabilityScore(score(AbilityConstants.INITIAL_SCORE)); profile.setConfidenceScore(score(AbilityConstants.INITIAL_CONFIDENCE));
        profile.setSampleCount(0); profile.setStatus("UNASSESSED");
        return profile;
    }

    private AbilityProfile copy(AbilityProfile source) {
        AbilityProfile target = new AbilityProfile();
        target.setId(source.getId()); target.setUserId(source.getUserId()); target.setKnowledgePointId(source.getKnowledgePointId());
        target.setMasteryScore(source.getMasteryScore()); target.setSpeedScore(source.getSpeedScore()); target.setStabilityScore(source.getStabilityScore());
        target.setConfidenceScore(source.getConfidenceScore()); target.setSampleCount(source.getSampleCount()); target.setStatus(source.getStatus()); target.setLastPracticeTime(source.getLastPracticeTime());
        return target;
    }

    private BigDecimal score(double value) { return BigDecimal.valueOf(clamp(value, 0.0, 100.0)).setScale(2, RoundingMode.HALF_UP); }
    private double value(BigDecimal value, double fallback) { return value == null ? fallback : value.doubleValue(); }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
