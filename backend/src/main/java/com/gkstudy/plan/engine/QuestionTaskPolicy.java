package com.gkstudy.plan.engine;

import org.springframework.stereotype.Component;

/** 题量目标策略：按计划分钟数与题目平均标准秒数推算任务目标题量，并按训练目的夹取到合理区间。 */
@Component
public class QuestionTaskPolicy {
    public static final int ASSESSMENT_MIN = 5;
    /** 首轮摸底以一组五题为最小、固定的可完成单元，避免新用户第一次计划过长。 */
    public static final int ASSESSMENT_MAX = 5;
    public static final int TRAINING_MIN = 3;
    public static final int TRAINING_MAX = 12;
    public static final int VALIDATION_MIN = 3;
    public static final int VALIDATION_MAX = 8;
    public static final int MAINTENANCE_MIN = 2;
    public static final int MAINTENANCE_MAX = 6;
    public static final int DEFAULT_STANDARD_SECONDS = 60;

    public int targetCount(String purpose, int plannedMinutes, int avgStandardSeconds) {
        int base = (int) ((long) plannedMinutes * 60 / Math.max(avgStandardSeconds, 15));
        return Math.max(minCount(purpose), Math.min(maxCount(purpose), base));
    }

    public int minCount(String purpose) {
        if ("ASSESSMENT".equals(purpose)) return ASSESSMENT_MIN;
        if ("VALIDATION".equals(purpose)) return VALIDATION_MIN;
        if ("MAINTENANCE".equals(purpose)) return MAINTENANCE_MIN;
        return TRAINING_MIN;
    }

    public int maxCount(String purpose) {
        if ("ASSESSMENT".equals(purpose)) return ASSESSMENT_MAX;
        if ("VALIDATION".equals(purpose)) return VALIDATION_MAX;
        if ("MAINTENANCE".equals(purpose)) return MAINTENANCE_MAX;
        return TRAINING_MAX;
    }
}
