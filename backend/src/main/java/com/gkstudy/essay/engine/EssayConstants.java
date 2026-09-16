package com.gkstudy.essay.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EssayConstants {
    public static final String DIM_MATERIAL_READING = "ESSAY_MATERIAL_READING";
    public static final String DIM_INFO_EXTRACTION = "ESSAY_INFO_EXTRACTION";
    public static final String DIM_POINT_COMPLETENESS = "ESSAY_POINT_COMPLETENESS";
    public static final String DIM_SUMMARY = "ESSAY_SUMMARY";
    public static final String DIM_ANALYSIS = "ESSAY_ANALYSIS";
    public static final String DIM_COUNTERMEASURE = "ESSAY_COUNTERMEASURE";
    public static final String DIM_IMPLEMENTATION = "ESSAY_IMPLEMENTATION";
    public static final String DIM_EXPRESSION = "ESSAY_EXPRESSION";

    public static final String TYPE_SUMMARY = "SUMMARY";
    public static final String TYPE_ANALYSIS = "ANALYSIS";
    public static final String TYPE_COUNTERMEASURE = "COUNTERMEASURE";
    public static final String TYPE_IMPLEMENTATION = "IMPLEMENTATION";

    private static final Map<String, String> DIMENSION_NAMES;
    private static final Map<String, String> TYPE_NAMES;
    private static final Map<String, String> PRIMARY_DIMENSIONS;

    static {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(DIM_MATERIAL_READING, "材料阅读");
        dimensions.put(DIM_INFO_EXTRACTION, "信息提取");
        dimensions.put(DIM_POINT_COMPLETENESS, "要点完整性");
        dimensions.put(DIM_SUMMARY, "归纳概括");
        dimensions.put(DIM_ANALYSIS, "综合分析");
        dimensions.put(DIM_COUNTERMEASURE, "提出对策");
        dimensions.put(DIM_IMPLEMENTATION, "贯彻执行");
        dimensions.put(DIM_EXPRESSION, "文字表达");
        DIMENSION_NAMES = Collections.unmodifiableMap(dimensions);
        Map<String, String> types = new HashMap<>();
        types.put(TYPE_SUMMARY, "归纳概括");
        types.put(TYPE_ANALYSIS, "综合分析");
        types.put(TYPE_COUNTERMEASURE, "提出对策");
        types.put(TYPE_IMPLEMENTATION, "贯彻执行");
        TYPE_NAMES = Collections.unmodifiableMap(types);
        Map<String, String> primary = new HashMap<>();
        primary.put(TYPE_SUMMARY, DIM_SUMMARY);
        primary.put(TYPE_ANALYSIS, DIM_ANALYSIS);
        primary.put(TYPE_COUNTERMEASURE, DIM_COUNTERMEASURE);
        primary.put(TYPE_IMPLEMENTATION, DIM_IMPLEMENTATION);
        PRIMARY_DIMENSIONS = Collections.unmodifiableMap(primary);
    }

    public static String dimensionName(String code) { return DIMENSION_NAMES.getOrDefault(code, code); }
    public static String typeName(String questionType) { return TYPE_NAMES.getOrDefault(questionType, questionType); }
    public static String primaryDimension(String questionType) { return PRIMARY_DIMENSIONS.getOrDefault(questionType, DIM_SUMMARY); }

    private EssayConstants() { }
}
