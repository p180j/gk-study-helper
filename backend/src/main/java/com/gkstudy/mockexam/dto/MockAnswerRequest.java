package com.gkstudy.mockexam.dto;
import javax.validation.constraints.*;
public class MockAnswerRequest {
 @NotNull private Long paperItemId; private String userAnswer; @NotNull @Min(0) private Long durationMs; private Long startedAtEpochMs;
 public Long getPaperItemId(){return paperItemId;} public void setPaperItemId(Long v){paperItemId=v;} public String getUserAnswer(){return userAnswer;} public void setUserAnswer(String v){userAnswer=v;} public Long getDurationMs(){return durationMs;} public void setDurationMs(Long v){durationMs=v;} public Long getStartedAtEpochMs(){return startedAtEpochMs;} public void setStartedAtEpochMs(Long v){startedAtEpochMs=v;}
}
