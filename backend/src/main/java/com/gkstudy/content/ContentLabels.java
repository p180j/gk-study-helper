package com.gkstudy.content;

import java.util.HashMap;
import java.util.Map;

/** 内容自动化内部枚举 → 用户可见中文，管理页面统一使用 */
public final class ContentLabels {
    private static final Map<String, String> STATUS = new HashMap<>();
    private static final Map<String, String> TRUST = new HashMap<>();
    private static final Map<String, String> SOURCE_TYPE = new HashMap<>();
    private static final Map<String, String> SITE_TYPE = new HashMap<>();

    static {
        STATUS.put("DISCOVERED", "已发现");
        STATUS.put("DOWNLOADED", "已下载");
        STATUS.put("PARSED", "已解析");
        STATUS.put("DEDUPED", "已去重");
        STATUS.put("READY", "待入库");
        STATUS.put("IMPORTED", "已入库");
        STATUS.put("NEEDS_REVIEW", "需要人工检查");
        STATUS.put("FAILED", "处理失败");

        TRUST.put("S", "S：官方真题 / 官方样题");
        TRUST.put("A", "A：官方附件 / 官方材料");
        TRUST.put("B", "B：多来源交叉验证");
        TRUST.put("C", "C：单来源回忆");
        TRUST.put("D", "D：未经验证");

        SOURCE_TYPE.put("ANNOUNCEMENT", "官方公告");
        SOURCE_TYPE.put("SYLLABUS", "考试大纲");
        SOURCE_TYPE.put("SAMPLE_QUESTION", "公开样题");
        SOURCE_TYPE.put("ATTACHMENT", "附件");
        SOURCE_TYPE.put("ARTICLE", "文章资料");

        SITE_TYPE.put("GOVERNMENT", "政府网站");
        SITE_TYPE.put("ORGANIZATION", "组织部门");
        SITE_TYPE.put("HR_DEPARTMENT", "人社部门");
        SITE_TYPE.put("EXAM_AUTHORITY", "公务员主管部门");
        SITE_TYPE.put("OTHER", "其他");
    }

    private ContentLabels() { }

    public static String status(String status) { return STATUS.getOrDefault(status, status); }
    public static String trust(String trustLevel) { return trustLevel == null ? "" : TRUST.getOrDefault(trustLevel, trustLevel); }
    public static String sourceType(String sourceType) { return SOURCE_TYPE.getOrDefault(sourceType, sourceType); }
    public static String siteType(String siteType) { return SITE_TYPE.getOrDefault(siteType, siteType); }

    public static boolean validTrust(String trustLevel) {
        return trustLevel != null && "SABCD".indexOf(trustLevel) >= 0 && trustLevel.length() == 1;
    }
}
