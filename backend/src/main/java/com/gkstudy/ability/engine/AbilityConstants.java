package com.gkstudy.ability.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AbilityConstants {
    public static final int RECENT_WINDOW_SIZE = 6;
    public static final int CONFIDENCE_FULL_SAMPLE_COUNT = 12;
    public static final int RECENT_PRACTICE_DAYS = 30;
    public static final double INITIAL_SCORE = 50.0;
    public static final double INITIAL_CONFIDENCE = 0.0;
    public static final double CORRECT_MASTERY_DELTA = 10.0;
    public static final double WRONG_MASTERY_DELTA = -12.0;
    public static final double MIN_MATURITY_FACTOR = 0.25;
    public static final double MATURITY_SAMPLE_DIVISOR = 10.0;
    public static final double SPEED_BLEND = 0.35;
    public static final Map<String, Double> PRACTICE_WEIGHTS;
    public static final Map<String, Double> CORRECT_CONFIDENCE_WEIGHTS;

    static {
        Map<String, Double> practice = new HashMap<>();
        practice.put("DAILY", 1.0); practice.put("EXTRA", 0.9); practice.put("SPECIAL", 1.05);
        practice.put("REVIEW", 0.65); practice.put("VALIDATION", 1.15); practice.put("MOCK", 1.1);
        PRACTICE_WEIGHTS = Collections.unmodifiableMap(practice);
        Map<String, Double> confidence = new HashMap<>();
        confidence.put("SURE", 1.0); confidence.put("HESITANT", 0.75); confidence.put("GUESS", 0.45); confidence.put("UNKNOWN", 0.85);
        CORRECT_CONFIDENCE_WEIGHTS = Collections.unmodifiableMap(confidence);
    }

    private AbilityConstants() { }
}
