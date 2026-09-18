package com.gkstudy.mockexam.engine;

import com.gkstudy.ability.model.AbilityProfile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class MockCalibrationEngine {
 public List<Calibration> calibrate(List<SectionStat> stats,List<AbilityProfile> abilities,int historyCount){
  Map<Long,AbilityProfile> byKnowledge=new HashMap<>(); if(abilities!=null) for(AbilityProfile p:abilities) byKnowledge.put(p.getKnowledgePointId(),p);
  List<Calibration> result=new ArrayList<>();
  for(SectionStat stat:stats){
   AbilityProfile ability=byKnowledge.get(stat.getKnowledgePointId()); double daily=ability==null||ability.getMasteryScore()==null?0:ability.getMasteryScore().doubleValue();
   double mock=stat.getAccuracyRate().doubleValue(); String type=null; String conclusion;
   if(stat.getCompletionRate().doubleValue()<75){type="EXAM_COMPLETION"; conclusion="本模块未完成题目较多，优先改善考试完成率";}
   else if(daily>=65&&mock<=daily-15&&stat.getOrderFragmentation()>=3){type="EXAM_ORDER_STRATEGY"; conclusion="专项能力尚可，但做题顺序频繁穿插打乱节奏，建议按模块连续作答";}
   else if(daily>=65&&mock<=daily-15&&stat.getTimeoutCount()>0){type="EXAM_TIME_MANAGEMENT"; conclusion="专项能力尚可，但模考耗时过高，主要是时间分配问题";}
   else if(daily>=65&&mock<=daily-15&&stat.getSecondHalfDrop().doubleValue()>=15){type="EXAM_SECTION_STABILITY"; conclusion="专项能力尚可，但后半段正确率下降，需提升考试稳定性";}
   else if(daily>=65&&mock<=daily-15){type="EXAM_PERFORMANCE_GAP"; conclusion="专项训练能力未稳定迁移到考试环境";}
   else if(daily>0&&daily<50&&mock<50){type="MASTERY"; conclusion="日常训练与模考都偏弱，更可能是知识掌握不足";}
   else conclusion="专项能力与本次模考表现基本一致";
   double confidence=Math.min(90,35+Math.min(historyCount,3)*15+Math.min(stat.getTotalCount(),20));
   result.add(new Calibration(stat.getSectionCode(),stat.getSectionName(),stat.getKnowledgePointId(),decimal(daily),decimal(mock),decimal(mock-daily),type,conclusion,decimal(confidence)));
  }
  return result;
 }
 private BigDecimal decimal(double v){return BigDecimal.valueOf(v).setScale(2,RoundingMode.HALF_UP);}
 public String nextProblemStatus(String current,boolean abnormal){
  if(abnormal){if(current==null)return "OBSERVING";if("OBSERVING".equals(current))return "CONFIRMED";if("RESOLVED".equals(current))return "REOPENED";return current;}
  if("CONFIRMED".equals(current)||"REOPENED".equals(current))return "PROCESSING";if("PROCESSING".equals(current))return "VERIFYING";if("VERIFYING".equals(current))return "RESOLVED";return current;
 }

 public static class SectionStat {
  private String sectionCode; private String sectionName; private Long knowledgePointId; private int totalCount; private int correctCount; private int wrongCount; private int skippedCount; private int unansweredCount; private int timeoutCount; private int orderFragmentation; private BigDecimal score; private BigDecimal accuracyRate; private BigDecimal completionRate; private long averageDurationMs; private BigDecimal secondHalfDrop;
  public SectionStat(){}
  public SectionStat(String code,String name,Long kp){sectionCode=code;sectionName=name;knowledgePointId=kp;}
  public int getOrderFragmentation(){return orderFragmentation;} public void setOrderFragmentation(int v){orderFragmentation=v;}
  public String getSectionCode(){return sectionCode;} public void setSectionCode(String v){sectionCode=v;} public String getSectionName(){return sectionName;} public void setSectionName(String v){sectionName=v;} public Long getKnowledgePointId(){return knowledgePointId;} public void setKnowledgePointId(Long v){knowledgePointId=v;} public int getTotalCount(){return totalCount;} public void setTotalCount(int v){totalCount=v;} public int getCorrectCount(){return correctCount;} public void setCorrectCount(int v){correctCount=v;} public int getWrongCount(){return wrongCount;} public void setWrongCount(int v){wrongCount=v;} public int getSkippedCount(){return skippedCount;} public void setSkippedCount(int v){skippedCount=v;} public int getUnansweredCount(){return unansweredCount;} public void setUnansweredCount(int v){unansweredCount=v;} public int getTimeoutCount(){return timeoutCount;} public void setTimeoutCount(int v){timeoutCount=v;} public BigDecimal getScore(){return score;} public void setScore(BigDecimal v){score=v;} public BigDecimal getAccuracyRate(){return accuracyRate;} public void setAccuracyRate(BigDecimal v){accuracyRate=v;} public BigDecimal getCompletionRate(){return completionRate;} public void setCompletionRate(BigDecimal v){completionRate=v;} public long getAverageDurationMs(){return averageDurationMs;} public void setAverageDurationMs(long v){averageDurationMs=v;} public BigDecimal getSecondHalfDrop(){return secondHalfDrop;} public void setSecondHalfDrop(BigDecimal v){secondHalfDrop=v;}
 }
 public static class Calibration {
  private final String sectionCode,sectionName; private final Long knowledgePointId; private final BigDecimal dailyAbility,mockPerformance,gap; private final String issueType,conclusion; private final BigDecimal confidence;
  public Calibration(String c,String n,Long k,BigDecimal d,BigDecimal m,BigDecimal g,String i,String text,BigDecimal conf){sectionCode=c;sectionName=n;knowledgePointId=k;dailyAbility=d;mockPerformance=m;gap=g;issueType=i;conclusion=text;confidence=conf;}
  public String getSectionCode(){return sectionCode;} public String getSectionName(){return sectionName;} public Long getKnowledgePointId(){return knowledgePointId;} public BigDecimal getDailyAbility(){return dailyAbility;} public BigDecimal getMockPerformance(){return mockPerformance;} public BigDecimal getGap(){return gap;} public String getIssueType(){return issueType;} public String getConclusion(){return conclusion;} public BigDecimal getConfidence(){return confidence;}
 }
}
