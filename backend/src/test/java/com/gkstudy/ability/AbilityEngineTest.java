package com.gkstudy.ability;

import com.gkstudy.ability.engine.AbilityEngine;
import com.gkstudy.ability.engine.AbilityEvent;
import com.gkstudy.ability.model.AbilityProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AbilityEngineTest {
    private final AbilityEngine engine = new AbilityEngine();

    @Test void easyCorrectRaisesMastery() { assertTrue(engine.calculate(null, event(true, 20, "SURE", 60000, 1)).getMasteryScore().doubleValue() > 50); }
    @Test void easyWrongLowersMastery() { assertTrue(engine.calculate(null, event(false, 20, "SURE", 60000, 1)).getMasteryScore().doubleValue() < 50); }
    @Test void hardCorrectRaisesMoreThanEasyCorrect() {
        double easy = engine.calculate(null, event(true, 20, "SURE", 60000, 1)).getMasteryScore().doubleValue();
        double hard = engine.calculate(null, event(true, 90, "SURE", 60000, 1)).getMasteryScore().doubleValue();
        assertTrue(hard > easy);
    }
    @Test void guessCorrectRaisesLessThanSureCorrect() {
        double guess = engine.calculate(null, event(true, 60, "GUESS", 60000, 1)).getMasteryScore().doubleValue();
        double sure = engine.calculate(null, event(true, 60, "SURE", 60000, 1)).getMasteryScore().doubleValue();
        assertTrue(guess < sure);
    }
    @Test void severeTimeoutLowersSpeed() { assertTrue(engine.calculate(null, event(true, 50, "SURE", 180000, 1)).getSpeedScore().doubleValue() < 50); }
    @Test void stableRecentPerformanceRaisesStability() {
        AbilityEvent event = event(true, 50, "SURE", 60000, 1); event.setRecentCorrectness(Arrays.asList(true, true, true, true));
        assertTrue(engine.calculate(null, event).getStabilityScore().doubleValue() > 50);
    }
    @Test void alternatingPerformanceHasLowerStability() {
        AbilityEvent stable = event(true, 50, "SURE", 60000, 1); stable.setRecentCorrectness(Arrays.asList(true, true, true, true));
        AbilityEvent alternating = event(true, 50, "SURE", 60000, 1); alternating.setRecentCorrectness(Arrays.asList(true, false, true, false));
        assertTrue(engine.calculate(null, alternating).getStabilityScore().compareTo(engine.calculate(null, stable).getStabilityScore()) < 0);
    }
    @Test void fewSamplesHaveLowConfidence() { assertTrue(engine.calculate(null, event(true, 50, "SURE", 60000, 1)).getConfidenceScore().doubleValue() < 20); }
    @Test void confidenceRisesWithSamples() {
        AbilityProfile profile = profile(5, 55); AbilityProfile next = engine.calculate(profile, event(true, 70, "SURE", 60000, 1));
        assertTrue(next.getConfidenceScore().doubleValue() > engine.calculate(null, event(true, 50, "SURE", 60000, 1)).getConfidenceScore().doubleValue());
    }
    @Test void primaryWeightChangesMoreThanSecondary() {
        double primary = engine.calculate(null, event(true, 70, "SURE", 60000, 0.7)).getMasteryScore().doubleValue() - 50;
        double secondary = engine.calculate(null, event(true, 70, "SURE", 60000, 0.3)).getMasteryScore().doubleValue() - 50;
        assertTrue(primary > secondary);
    }
    @Test void matureAbilityDoesNotSwingSharply() {
        AbilityProfile mature = profile(50, 75); double change = Math.abs(engine.calculate(mature, event(false, 20, "SURE", 60000, 1)).getMasteryScore().doubleValue() - 75);
        assertTrue(change < 5);
    }

    private AbilityEvent event(boolean correct, int difficulty, String confidence, long duration, double weight) {
        AbilityEvent event = new AbilityEvent(); event.setCorrect(correct); event.setDifficulty(BigDecimal.valueOf(difficulty));
        event.setDurationMs(duration); event.setStandardTimeSeconds(60); event.setAnswerConfidenceType(confidence); event.setPracticeType("DAILY");
        event.setKnowledgeWeight(BigDecimal.valueOf(weight)); event.setAnswerTime(LocalDateTime.of(2026, 1, 1, 12, 0));
        event.setRecentCorrectness(Collections.singletonList(correct)); event.setRecentDifficulties(Collections.singletonList(BigDecimal.valueOf(difficulty)));
        event.setRecentPracticeTypes(Collections.singletonList("DAILY")); return event;
    }

    private AbilityProfile profile(int samples, double mastery) {
        AbilityProfile profile = new AbilityProfile(); profile.setSampleCount(samples); profile.setMasteryScore(BigDecimal.valueOf(mastery));
        profile.setSpeedScore(BigDecimal.valueOf(60)); profile.setStabilityScore(BigDecimal.valueOf(60)); profile.setConfidenceScore(BigDecimal.valueOf(30));
        profile.setStatus("LEARNING"); profile.setLastPracticeTime(LocalDateTime.of(2026, 1, 1, 11, 0)); return profile;
    }
}
