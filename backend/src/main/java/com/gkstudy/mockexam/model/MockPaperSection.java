package com.gkstudy.mockexam.model;
import java.math.BigDecimal; import java.util.ArrayList; import java.util.List;
public class MockPaperSection {
 private Long id; private Long paperId; private String name; private String sectionCode; private Long knowledgePointId; private Integer sortNo; private BigDecimal score; private List<MockPaperItem> items=new ArrayList<>();
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getPaperId(){return paperId;} public void setPaperId(Long v){paperId=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getSectionCode(){return sectionCode;} public void setSectionCode(String v){sectionCode=v;} public Long getKnowledgePointId(){return knowledgePointId;} public void setKnowledgePointId(Long v){knowledgePointId=v;} public Integer getSortNo(){return sortNo;} public void setSortNo(Integer v){sortNo=v;} public BigDecimal getScore(){return score;} public void setScore(BigDecimal v){score=v;} public List<MockPaperItem> getItems(){return items;} public void setItems(List<MockPaperItem> v){items=v;}
}
