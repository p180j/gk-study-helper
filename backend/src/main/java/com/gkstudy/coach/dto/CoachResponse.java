package com.gkstudy.coach.dto;

import java.util.List;

public class CoachResponse {
    private final String currentStatus;
    private final List<String> coreProblems;
    private final String todayReason;
    private final String answer;
    private final List<String> evidence;
    private final String provider;
    private final String model;

    public CoachResponse(String currentStatus, List<String> coreProblems, String todayReason, String answer,
                         List<String> evidence, String provider, String model) {
        this.currentStatus = currentStatus; this.coreProblems = coreProblems; this.todayReason = todayReason;
        this.answer = answer; this.evidence = evidence; this.provider = provider; this.model = model;
    }
    public String getCurrentStatus() { return currentStatus; }
    public List<String> getCoreProblems() { return coreProblems; }
    public String getTodayReason() { return todayReason; }
    public String getAnswer() { return answer; }
    public List<String> getEvidence() { return evidence; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
}
