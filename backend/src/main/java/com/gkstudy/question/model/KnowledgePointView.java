package com.gkstudy.question.model;

import java.math.BigDecimal;

public class KnowledgePointView {
    private Long id;
    private Long parentId;
    private String parentName;
    private String code;
    private String name;
    private Integer level;
    private Integer sortNo;
    private String status;
    private BigDecimal importance;
    private BigDecimal improvementPotential;
    private BigDecimal transferValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getImportance() { return importance; }
    public void setImportance(BigDecimal importance) { this.importance = importance; }
    public BigDecimal getImprovementPotential() { return improvementPotential; }
    public void setImprovementPotential(BigDecimal improvementPotential) { this.improvementPotential = improvementPotential; }
    public BigDecimal getTransferValue() { return transferValue; }
    public void setTransferValue(BigDecimal transferValue) { this.transferValue = transferValue; }
}
