package com.gkstudy.reading.dto;

public class TopicView {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer totalMaterials;
    private Integer completedMaterials;

    public TopicView() { }

    public TopicView(Long id, String code, String name, String description, Integer totalMaterials, Integer completedMaterials) {
        this.id = id; this.code = code; this.name = name; this.description = description;
        this.totalMaterials = totalMaterials; this.completedMaterials = completedMaterials;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getTotalMaterials() { return totalMaterials; }
    public void setTotalMaterials(Integer totalMaterials) { this.totalMaterials = totalMaterials; }
    public Integer getCompletedMaterials() { return completedMaterials; }
    public void setCompletedMaterials(Integer completedMaterials) { this.completedMaterials = completedMaterials; }
}
