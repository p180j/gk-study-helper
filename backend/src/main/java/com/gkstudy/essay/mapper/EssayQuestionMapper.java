package com.gkstudy.essay.mapper;

import com.gkstudy.essay.model.EssayQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EssayQuestionMapper {
    @Insert("INSERT INTO essay_question(topic_knowledge_point_id,question_type,material,prompt,word_limit_min,word_limit_max,standard_time_seconds,reference_answer,reference_points,source_type,source_year,source_exam,source_name,status,version) VALUES(#{topicKnowledgePointId},#{questionType},#{material},#{prompt},#{wordLimitMin},#{wordLimitMax},#{standardTimeSeconds},#{referenceAnswer},CAST(#{referencePointsJson} AS JSON),#{sourceType},#{sourceYear},#{sourceExam},#{sourceName},#{status},#{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EssayQuestion question);

    @Select("SELECT q.id,q.topic_knowledge_point_id,q.question_type,q.material,q.prompt,q.word_limit_min,q.word_limit_max,q.standard_time_seconds,q.reference_answer,q.reference_points AS reference_points_json,q.source_type,q.source_year,q.source_exam,q.source_name,q.status,q.version,q.create_time,q.update_time,kp.code AS topic_code,kp.name AS topic_name FROM essay_question q JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id WHERE q.id=#{id}")
    EssayQuestion findById(Long id);

    @Select("SELECT q.id,q.topic_knowledge_point_id,q.question_type,q.material,q.prompt,q.word_limit_min,q.word_limit_max,q.standard_time_seconds,q.reference_answer,q.reference_points AS reference_points_json,q.source_type,q.source_year,q.source_exam,q.source_name,q.status,q.version,q.create_time,q.update_time,kp.code AS topic_code,kp.name AS topic_name FROM essay_question q JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id WHERE q.id=#{id} AND q.status='ACTIVE'")
    EssayQuestion findActiveById(Long id);

    @Select("<script>SELECT q.id,q.topic_knowledge_point_id,q.question_type,q.material,q.prompt,q.word_limit_min,q.word_limit_max,q.standard_time_seconds,q.reference_answer,q.reference_points AS reference_points_json,q.source_type,q.source_year,q.source_exam,q.source_name,q.status,q.version,q.create_time,q.update_time,kp.code AS topic_code,kp.name AS topic_name FROM essay_question q JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id <where>"
            + "<if test='keyword != null and keyword != \"\"'>AND (q.prompt LIKE CONCAT('%',#{keyword},'%') OR q.material LIKE CONCAT('%',#{keyword},'%') OR q.source_name LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='questionType != null and questionType != \"\"'>AND q.question_type=#{questionType}</if>"
            + "<if test='topicKnowledgePointId != null'>AND q.topic_knowledge_point_id=#{topicKnowledgePointId}</if>"
            + "<if test='status != null and status != \"\"'>AND q.status=#{status}</if>"
            + "</where> ORDER BY q.id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<EssayQuestion> adminList(@Param("keyword") String keyword, @Param("questionType") String questionType,
                                   @Param("topicKnowledgePointId") Long topicKnowledgePointId, @Param("status") String status,
                                   @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM essay_question q <where>"
            + "<if test='keyword != null and keyword != \"\"'>AND (q.prompt LIKE CONCAT('%',#{keyword},'%') OR q.material LIKE CONCAT('%',#{keyword},'%') OR q.source_name LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='questionType != null and questionType != \"\"'>AND q.question_type=#{questionType}</if>"
            + "<if test='topicKnowledgePointId != null'>AND q.topic_knowledge_point_id=#{topicKnowledgePointId}</if>"
            + "<if test='status != null and status != \"\"'>AND q.status=#{status}</if>"
            + "</where></script>")
    int adminCount(@Param("keyword") String keyword, @Param("questionType") String questionType,
                   @Param("topicKnowledgePointId") Long topicKnowledgePointId, @Param("status") String status);

    @Select("SELECT q.id,q.topic_knowledge_point_id,q.question_type,q.material,q.prompt,q.word_limit_min,q.word_limit_max,q.standard_time_seconds,q.reference_answer,q.reference_points AS reference_points_json,q.source_type,q.source_year,q.source_exam,q.source_name,q.status,q.version,q.create_time,q.update_time,kp.code AS topic_code,kp.name AS topic_name FROM essay_question q JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id WHERE q.topic_knowledge_point_id=#{topicKpId} AND q.status='ACTIVE' ORDER BY q.id LIMIT #{size}")
    List<EssayQuestion> findActiveByTopicKpId(@Param("topicKpId") Long topicKpId, @Param("size") int size);

    @Select("SELECT q.id,q.topic_knowledge_point_id,q.question_type,q.material,q.prompt,q.word_limit_min,q.word_limit_max,q.standard_time_seconds,q.reference_answer,q.reference_points AS reference_points_json,q.source_type,q.source_year,q.source_exam,q.source_name,q.status,q.version,q.create_time,q.update_time,kp.code AS topic_code,kp.name AS topic_name FROM essay_question q JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id LEFT JOIN essay_answer a ON a.essay_question_id=q.id AND a.user_id=#{userId} WHERE q.topic_knowledge_point_id=#{topicKpId} AND q.status='ACTIVE' ORDER BY (CASE WHEN a.id IS NULL THEN 0 ELSE 1 END),q.id LIMIT #{size}")
    List<EssayQuestion> findFirstUnansweredByTopicKpId(@Param("userId") Long userId, @Param("topicKpId") Long topicKpId,
                                                      @Param("size") int size);

    @Select("SELECT q.id,q.topic_knowledge_point_id,q.question_type,q.material,q.prompt,q.word_limit_min,q.word_limit_max,q.standard_time_seconds,q.reference_answer,q.reference_points AS reference_points_json,q.source_type,q.source_year,q.source_exam,q.source_name,q.status,q.version,q.create_time,q.update_time,kp.code AS topic_code,kp.name AS topic_name FROM essay_question q JOIN knowledge_point kp ON kp.id=q.topic_knowledge_point_id WHERE q.status='ACTIVE' ORDER BY q.id LIMIT #{size}")
    List<EssayQuestion> listActiveAll(@Param("size") int size);
}
