package com.gkstudy.mockexam.dto;
import com.gkstudy.mockexam.model.*; import java.time.LocalDateTime; import java.util.List;
public class MockSessionView {
 private final MockSession session; private final MockPaper paper; private final List<MockPaperItem> items; private final List<MockAnswer> answers; private final LocalDateTime deadline;
 public MockSessionView(MockSession s,MockPaper p,List<MockPaperItem> i,List<MockAnswer> a,LocalDateTime d){session=s;paper=p;items=i;answers=a;deadline=d;}
 public MockSession getSession(){return session;} public MockPaper getPaper(){return paper;} public List<MockPaperItem> getItems(){return items;} public List<MockAnswer> getAnswers(){return answers;} public LocalDateTime getDeadline(){return deadline;}
}
