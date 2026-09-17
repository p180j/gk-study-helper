package com.gkstudy.content.dto;

public class InventoryItem {
    private Long id;
    private String code;
    private String name;
    private String examType;
    private Long parentId;
    private int totalQuestions;
    private int qualityQuestions;
    private int unusedQualityQuestions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getQualityQuestions() { return qualityQuestions; }
    public void setQualityQuestions(int qualityQuestions) { this.qualityQuestions = qualityQuestions; }
    public int getUnusedQualityQuestions() { return unusedQualityQuestions; }
    public void setUnusedQualityQuestions(int unusedQualityQuestions) { this.unusedQualityQuestions = unusedQualityQuestions; }
}
