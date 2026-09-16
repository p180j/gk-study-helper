package com.gkstudy.essay.mapper;

import com.gkstudy.essay.model.EssayAnswer;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EssayAnswerMapper {
    @Insert("INSERT INTO essay_answer(user_id,essay_question_id,question_version,practice_type,answer_text,duration_ms,word_count,submit_time) VALUES(#{userId},#{essayQuestionId},#{questionVersion},#{practiceType},#{answerText},#{durationMs},#{wordCount},#{submitTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EssayAnswer answer);

    @Select("SELECT a.id,a.user_id,a.essay_question_id,a.question_version,a.practice_type,a.answer_text,a.duration_ms,a.word_count,a.submit_time,LEFT(q.prompt,60) AS prompt_preview,kp.name AS topic_name,(CASE q.question_type WHEN 'SUMMARY' THEN '归纳概括' WHEN 'ANALYSIS' THEN '综合分析' WHEN 'COUNTERMEASURE' THEN '提出对策' WHEN 'IMPLEMENTATION' THEN '贯彻执行' ELSE q.question_type END) AS question_type_name,ev.total_score,ev.evaluator FROM essay_answer a JOIN essay_question q ON q.id=a.essay_question_id JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id LEFT JOIN essay_evaluation ev ON ev.id=(SELECT MAX(e2.id) FROM essay_evaluation e2 WHERE e2.essay_answer_id=a.id) WHERE a.id=#{id}")
    EssayAnswer findById(Long id);

    @Select("<script>SELECT a.id,a.user_id,a.essay_question_id,a.question_version,a.practice_type,a.answer_text,a.duration_ms,a.word_count,a.submit_time,LEFT(q.prompt,60) AS prompt_preview,kp.name AS topic_name,(CASE q.question_type WHEN 'SUMMARY' THEN '归纳概括' WHEN 'ANALYSIS' THEN '综合分析' WHEN 'COUNTERMEASURE' THEN '提出对策' WHEN 'IMPLEMENTATION' THEN '贯彻执行' ELSE q.question_type END) AS question_type_name,ev.total_score,ev.evaluator FROM essay_answer a JOIN essay_question q ON q.id=a.essay_question_id JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id LEFT JOIN essay_evaluation ev ON ev.id=(SELECT MAX(e2.id) FROM essay_evaluation e2 WHERE e2.essay_answer_id=a.id) <where>"
            + "<if test='userId != null'>AND a.user_id=#{userId}</if>"
            + "<if test='essayQuestionId != null'>AND a.essay_question_id=#{essayQuestionId}</if>"
            + "</where> ORDER BY a.id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<EssayAnswer> adminList(@Param("userId") Long userId, @Param("essayQuestionId") Long essayQuestionId,
                                 @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM essay_answer a <where>"
            + "<if test='userId != null'>AND a.user_id=#{userId}</if>"
            + "<if test='essayQuestionId != null'>AND a.essay_question_id=#{essayQuestionId}</if>"
            + "</where></script>")
    int adminCount(@Param("userId") Long userId, @Param("essayQuestionId") Long essayQuestionId);

    @Select("SELECT a.id,a.user_id,a.essay_question_id,a.question_version,a.practice_type,a.answer_text,a.duration_ms,a.word_count,a.submit_time FROM essay_answer a WHERE a.user_id=#{userId} AND a.essay_question_id=#{essayQuestionId} ORDER BY a.submit_time,a.id")
    List<EssayAnswer> findByUserAndQuestion(@Param("userId") Long userId, @Param("essayQuestionId") Long essayQuestionId);
}
