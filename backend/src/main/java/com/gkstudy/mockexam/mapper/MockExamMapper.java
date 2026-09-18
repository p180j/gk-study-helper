package com.gkstudy.mockexam.mapper;

import com.gkstudy.mockexam.model.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MockExamMapper {
 @Select("SELECT * FROM mock_paper WHERE status='ACTIVE' ORDER BY source_year DESC,id DESC") List<MockPaper> listActivePapers();
 @Select("SELECT * FROM mock_paper ORDER BY id DESC") List<MockPaper> listAllPapers();
 @Select("SELECT * FROM mock_paper WHERE id=#{id}") MockPaper findPaper(Long id);
 @Select("SELECT * FROM mock_paper_section WHERE paper_id=#{paperId} ORDER BY sort_no,id") List<MockPaperSection> findSections(Long paperId);
 @Select("SELECT i.id,i.paper_id,i.section_id,i.item_type,i.question_id,i.essay_question_id,i.sort_no,i.score,s.section_code,s.name AS section_name,COALESCE(q.stem,eq.prompt) AS stem,eq.material,eq.prompt,COALESCE(q.standard_time_seconds,eq.standard_time_seconds) AS standard_time_seconds,q.answer AS correct_answer,q.analysis FROM mock_paper_item i JOIN mock_paper_section s ON s.id=i.section_id LEFT JOIN question q ON q.id=i.question_id LEFT JOIN essay_question eq ON eq.id=i.essay_question_id WHERE i.paper_id=#{paperId} ORDER BY i.sort_no,i.id") List<MockPaperItem> findItems(Long paperId);
 @Select("SELECT i.id,i.paper_id,i.section_id,i.item_type,i.question_id,i.essay_question_id,i.sort_no,i.score,s.section_code,s.name AS section_name,COALESCE(q.stem,eq.prompt) AS stem,eq.material,eq.prompt,COALESCE(q.standard_time_seconds,eq.standard_time_seconds) AS standard_time_seconds,q.answer AS correct_answer,q.analysis FROM mock_paper_item i JOIN mock_paper_section s ON s.id=i.section_id LEFT JOIN question q ON q.id=i.question_id LEFT JOIN essay_question eq ON eq.id=i.essay_question_id WHERE i.id=#{id}") MockPaperItem findItem(Long id);
 @Insert("INSERT INTO mock_paper(name,exam_type,source_year,source,usage_type,duration_minutes,total_score,status) VALUES(#{name},#{examType},#{sourceYear},#{source},#{usageType},#{durationMinutes},#{totalScore},#{status})") @Options(useGeneratedKeys=true,keyProperty="id") int insertPaper(MockPaper paper);
 @Update("UPDATE mock_paper SET status=#{status} WHERE id=#{id}") int updatePaperStatus(@Param("id") Long id,@Param("status") String status);
 @Insert("INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) VALUES(#{paperId},#{name},#{sectionCode},#{knowledgePointId},#{sortNo},#{score})") @Options(useGeneratedKeys=true,keyProperty="id") int insertSection(MockPaperSection section);
 @Insert("INSERT INTO mock_paper_item(paper_id,section_id,item_type,question_id,essay_question_id,sort_no,score) VALUES(#{paperId},#{sectionId},#{itemType},#{questionId},#{essayQuestionId},#{sortNo},#{score})") @Options(useGeneratedKeys=true,keyProperty="id") int insertItem(MockPaperItem item);
 @Select("SELECT COUNT(*) FROM mock_paper_item i JOIN question q ON q.id=i.question_id WHERE i.paper_id=#{paperId} AND q.usage_type<>'MOCK_RESERVED'") int countNonReservedQuestions(Long paperId);

 @Insert("INSERT INTO mock_session(user_id,paper_id,start_time,duration_ms,status) VALUES(#{userId},#{paperId},#{startTime},0,#{status})") @Options(useGeneratedKeys=true,keyProperty="id") int insertSession(MockSession session);
 @Select("SELECT * FROM mock_session WHERE id=#{id}") MockSession findSession(Long id);
 @Select("SELECT * FROM mock_session WHERE id=#{id} FOR UPDATE") MockSession findSessionForUpdate(Long id);
 @Select("SELECT * FROM mock_session WHERE user_id=#{userId} ORDER BY id DESC") List<MockSession> findSessionsByUser(Long userId);
 @Select("SELECT COUNT(*) FROM mock_session WHERE user_id=#{userId} AND status='SUBMITTED'") int countSubmittedSessions(Long userId);
 @Update("UPDATE mock_session SET submit_time=#{submitTime},duration_ms=#{durationMs},status=#{status},total_score=#{totalScore},completion_rate=#{completionRate},submit_type=#{submitType} WHERE id=#{id}") int updateSession(MockSession session);

 @Insert("INSERT INTO mock_answer(session_id,paper_item_id,question_id,essay_question_id,section_code,answer_order,user_answer,correct_answer_snapshot,result_status,duration_ms,start_time,submit_time,word_count,score,evaluation_json) VALUES(#{sessionId},#{paperItemId},#{questionId},#{essayQuestionId},#{sectionCode},#{answerOrder},#{userAnswer},#{correctAnswerSnapshot},#{resultStatus},#{durationMs},#{startTime},#{submitTime},#{wordCount},#{score},CAST(#{evaluationJson} AS JSON)) ON DUPLICATE KEY UPDATE answer_order=COALESCE(answer_order,VALUES(answer_order)),user_answer=VALUES(user_answer),correct_answer_snapshot=VALUES(correct_answer_snapshot),result_status=VALUES(result_status),duration_ms=VALUES(duration_ms),start_time=COALESCE(start_time,VALUES(start_time)),submit_time=VALUES(submit_time),word_count=VALUES(word_count),score=VALUES(score),evaluation_json=VALUES(evaluation_json)") int upsertAnswer(MockAnswer answer);
 @Select("SELECT id,session_id,paper_item_id,question_id,essay_question_id,section_code,answer_order,user_answer,correct_answer_snapshot,result_status,duration_ms,start_time,submit_time,word_count,score,evaluation_json AS evaluation_json FROM mock_answer WHERE session_id=#{sessionId} ORDER BY answer_order,id") List<MockAnswer> findAnswers(Long sessionId);
 @Select("SELECT id,session_id,paper_item_id,question_id,essay_question_id,section_code,answer_order,user_answer,correct_answer_snapshot,result_status,duration_ms,start_time,submit_time,word_count,score,evaluation_json AS evaluation_json FROM mock_answer WHERE session_id=#{sessionId} AND paper_item_id=#{itemId}") MockAnswer findAnswer(@Param("sessionId") Long sessionId,@Param("itemId") Long itemId);
 @Select("SELECT COALESCE(MAX(answer_order),0)+1 FROM mock_answer WHERE session_id=#{sessionId}") int nextAnswerOrder(Long sessionId);

 @Insert("INSERT INTO mock_result(session_id,total_score,accuracy_rate,completion_rate,total_duration_ms,section_stats_json,calibration_json) VALUES(#{sessionId},#{totalScore},#{accuracyRate},#{completionRate},#{totalDurationMs},CAST(#{sectionStatsJson} AS JSON),CAST(#{calibrationJson} AS JSON))") @Options(useGeneratedKeys=true,keyProperty="id") int insertResult(MockResult result);
 @Select("SELECT id,session_id,total_score,accuracy_rate,completion_rate,total_duration_ms,section_stats_json,calibration_json FROM mock_result WHERE session_id=#{sessionId}") MockResult findResult(Long sessionId);
 @Select("SELECT mr.id,mr.session_id,mr.total_score,mr.accuracy_rate,mr.completion_rate,mr.total_duration_ms,mr.section_stats_json,mr.calibration_json FROM mock_result mr JOIN mock_session ms ON ms.id=mr.session_id WHERE ms.user_id=#{userId} ORDER BY mr.id DESC") List<MockResult> findResultsByUser(Long userId);
}
