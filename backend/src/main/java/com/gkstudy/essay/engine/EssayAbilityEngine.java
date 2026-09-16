package com.gkstudy.essay.engine;

import com.gkstudy.ability.model.AbilityProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 申论能力引擎：把申论维度评分映射为能力画像更新值。
 * 与行测 AbilityEngine 不同，申论以整题评分为输入（无对错与速度事件），
 * speed/stability 维持初始值，confidence 随样本数线性增长。
 */
@Component
public class EssayAbilityEngine {
    private static final double MIN_MATURITY = 0.3;
    private static final double MATURITY_SAMPLE_DIVISOR = 8.0;

    public AbilityValues update(AbilityProfile current, double score) {
        int previousSamples = current == null || current.getSampleCount() == null ? 0 : current.getSampleCount();
        double mastery;
        if (current == null) {
            mastery = score;
        } else {
            double old = current.getMasteryScore() == null ? 50.0 : current.getMasteryScore().doubleValue();
            double maturity = Math.max(MIN_MATURITY, 1.0 / (1.0 + previousSamples / MATURITY_SAMPLE_DIVISOR));
            mastery = old + (score - old) * maturity;
        }
        double speed = current == null || current.getSpeedScore() == null ? 50.0 : current.getSpeedScore().doubleValue();
        double stability = current == null || current.getStabilityScore() == null ? 50.0 : current.getStabilityScore().doubleValue();
        double confidence = Math.min(100.0, 20.0 + (previousSamples + 1) * 12.0);
        int sampleCount = previousSamples + 1;
        return new AbilityValues(decimal(mastery), decimal(speed), decimal(stability), decimal(confidence), sampleCount,
                status(mastery, sampleCount));
    }

    private String status(double mastery, int samples) {
        if (mastery >= 80.0) return "PROFICIENT";
        if (mastery >= 65.0) return "MASTERED";
        if (mastery >= 45.0) return "LEARNING";
        if (samples >= 2) return "WEAK";
        if (mastery < 20.0) return "UNASSESSED";
        return "LEARNING";
    }

    private BigDecimal decimal(double value) { return BigDecimal.valueOf(Math.max(0.0, Math.min(100.0, value))).setScale(2, RoundingMode.HALF_UP); }

    public static class AbilityValues {
        private final BigDecimal mastery;
        private final BigDecimal speed;
        private final BigDecimal stability;
        private final BigDecimal confidence;
        private final int sampleCount;
        private final String status;

        public AbilityValues(BigDecimal mastery, BigDecimal speed, BigDecimal stability, BigDecimal confidence, int sampleCount, String status) {
            this.mastery = mastery; this.speed = speed; this.stability = stability; this.confidence = confidence;
            this.sampleCount = sampleCount; this.status = status;
        }

        public BigDecimal getMastery() { return mastery; }
        public BigDecimal getSpeed() { return speed; }
        public BigDecimal getStability() { return stability; }
        public BigDecimal getConfidence() { return confidence; }
        public int getSampleCount() { return sampleCount; }
        public String getStatus() { return status; }
    }
}
