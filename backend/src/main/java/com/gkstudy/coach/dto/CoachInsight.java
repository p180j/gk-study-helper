package com.gkstudy.coach.dto;

import java.util.List;

/**
 * 首页 AI 今日洞察：只从真实数据（error_diagnosis 已确认根因 → learning_problem → 能力画像）中选取，
 * 不经过 AI 生成，source 仅用于前端判断展示形态，不直接展示。
 */
public class CoachInsight {
    private final String source;
    private final String knowledgePointName;
    private final String problem;
    private final String suggestion;
    private final List<String> evidence;

    public CoachInsight(String source, String knowledgePointName, String problem, String suggestion, List<String> evidence) {
        this.source = source; this.knowledgePointName = knowledgePointName; this.problem = problem;
        this.suggestion = suggestion; this.evidence = evidence;
    }

    public String getSource() { return source; }
    public String getKnowledgePointName() { return knowledgePointName; }
    public String getProblem() { return problem; }
    public String getSuggestion() { return suggestion; }
    public List<String> getEvidence() { return evidence; }
}
