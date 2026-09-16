package com.gkstudy.essay;

import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.essay.engine.EssayAbilityEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EssayAbilityEngineTest {
    private final EssayAbilityEngine engine = new EssayAbilityEngine();

    @Test
    void firstSampleUsesScoreAsMastery() {
        EssayAbilityEngine.AbilityValues values = engine.update(null, 72.0);
        assertEquals(0, values.getMastery().compareTo(BigDecimal.valueOf(72)));
        assertEquals(0, values.getSpeed().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, values.getStability().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, values.getConfidence().compareTo(BigDecimal.valueOf(32)));
        assertEquals(1, values.getSampleCount());
    }

    @Test
    void secondSampleMovesMasteryTowardNewScore() {
        EssayAbilityEngine.AbilityValues values = engine.update(profile(1, 60), 80.0);
        assertTrue(values.getMastery().doubleValue() > 60 && values.getMastery().doubleValue() < 80,
                "平滑后的 mastery 应介于旧值 60 与新分数 80 之间，实际 " + values.getMastery());
        assertEquals(2, values.getSampleCount());
    }

    @Test
    void maturityNeverDropsBelowFloor() {
        EssayAbilityEngine.AbilityValues values = engine.update(profile(100, 20), 100.0);
        // 成熟度下限 0.3：20 + (100-20)*0.3 = 44，若无限下降将远小于 44
        assertEquals(0, values.getMastery().compareTo(BigDecimal.valueOf(44)));
        assertEquals(101, values.getSampleCount());
    }

    @Test
    void weakStatusRequiresTwoSamples() {
        assertEquals("LEARNING", engine.update(null, 40.0).getStatus());
        assertEquals("WEAK", engine.update(profile(1, 40), 40.0).getStatus());
    }

    @Test
    void proficientStatusAtEighty() {
        assertEquals("PROFICIENT", engine.update(null, 85.0).getStatus());
        assertEquals("PROFICIENT", engine.update(profile(3, 75), 82.0).getStatus());
    }

    private AbilityProfile profile(int samples, double mastery) {
        AbilityProfile profile = new AbilityProfile();
        profile.setUserId(7L); profile.setKnowledgePointId(12L); profile.setSampleCount(samples);
        profile.setMasteryScore(BigDecimal.valueOf(mastery));
        profile.setSpeedScore(BigDecimal.valueOf(55)); profile.setStabilityScore(BigDecimal.valueOf(55));
        profile.setConfidenceScore(BigDecimal.valueOf(30)); profile.setStatus("LEARNING");
        return profile;
    }
}
