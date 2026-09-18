package com.gkstudy.question.mapper;

import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionMapper {
    @Select("<script>SELECT * FROM question <where>"
            + "<if test='status != null and status != \"\"'>AND status=#{status}</if>"
            + "<if test='questionType != null and questionType != \"\"'>AND question_type=#{questionType}</if>"
            + "<if test='usageType != null and usageType != \"\"'>AND usage_type=#{usageType}</if>"
            + "<if test='keyword != null and keyword != \"\"'>AND (stem LIKE CONCAT('%',#{keyword},'%') OR source_name LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "</where> ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<Question> findAll(@Param("status") String status, @Param("questionType") String questionType,
                           @Param("usageType") String usageType, @Param("keyword") String keyword,
                           @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM question WHERE id=#{id}")
    Question findById(Long id);

    @Select("SELECT id,question_id,option_key,option_text,sort_no FROM question_option WHERE question_id=#{questionId} ORDER BY sort_no")
    List<QuestionOption> findOptions(Long questionId);

    @Select("SELECT kp.id,kp.code,kp.name,qk.relation_type,qk.weight FROM question_knowledge qk JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id WHERE qk.question_id=#{questionId} ORDER BY qk.weight DESC,kp.id")
    List<KnowledgePointRef> findKnowledgePoints(Long questionId);

    @Select("SELECT id,code,name FROM knowledge_point WHERE code=#{code} AND status='ACTIVE'")
    KnowledgePointRef findKnowledgePointByCode(String code);

    @Select("<script>SELECT DISTINCT q.* FROM question q JOIN question_knowledge qk ON qk.question_id=q.id "
            + "WHERE qk.knowledge_point_id=#{knowledgePointId} AND q.status='ACTIVE' "
            + "AND q.usage_type IN ('TRAINING','VALIDATION') "
            + "ORDER BY <choose><when test='purpose == \"VALIDATION\"'>FIELD(q.usage_type,'VALIDATION','TRAINING')</when>"
            + "<otherwise>FIELD(q.usage_type,'TRAINING','VALIDATION')</otherwise></choose>,q.id LIMIT #{limit}</script>")
    List<Question> findForPlanItem(@Param("knowledgePointId") Long knowledgePointId,
                                   @Param("purpose") String purpose, @Param("limit") int limit);

    /** 取题扩展：当前知识点未做 → 当前知识点已做 → 同父模块兄弟知识点未做 → 兄弟知识点已做，按层级排序去重取题。 */
    @Select("<script>SELECT q.* FROM question q JOIN question_knowledge qk ON qk.question_id=q.id "
            + "JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id "
            + "WHERE (kp.id=#{knowledgePointId} OR kp.parent_id=#{knowledgePointId} "
            + "OR kp.id=(SELECT self.parent_id FROM knowledge_point self WHERE self.id=#{knowledgePointId}) "
            + "OR (kp.parent_id=(SELECT self2.parent_id FROM knowledge_point self2 WHERE self2.id=#{knowledgePointId}) "
            + "AND (SELECT p.parent_id FROM knowledge_point p WHERE p.id=(SELECT self3.parent_id FROM knowledge_point self3 WHERE self3.id=#{knowledgePointId})) IS NOT NULL)) "
            + "AND q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') "
            + "<if test='purpose == \"VALIDATION\"'>AND (q.source_level IS NULL OR q.source_level NOT IN ('C','D'))</if> "
            + "GROUP BY q.id "
            + "ORDER BY MIN(CASE WHEN kp.id=#{knowledgePointId} AND NOT EXISTS (SELECT 1 FROM answer_record ar WHERE ar.user_id=#{userId} AND ar.question_id=q.id) THEN 0 "
            + "WHEN kp.id=#{knowledgePointId} THEN 1 "
            + "WHEN NOT EXISTS (SELECT 1 FROM answer_record ar WHERE ar.user_id=#{userId} AND ar.question_id=q.id) THEN 2 ELSE 3 END),q.id LIMIT #{limit}</script>")
    List<Question> findForPlanItemExpanded(@Param("knowledgePointId") Long knowledgePointId, @Param("purpose") String purpose,
                                           @Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT AVG(q.standard_time_seconds) FROM question q JOIN question_knowledge qk ON qk.question_id=q.id WHERE qk.knowledge_point_id=#{knowledgePointId} AND q.status='ACTIVE'")
    Integer avgStandardSeconds(Long knowledgePointId);

    @Select("SELECT COUNT(*) FROM question WHERE content_hash=#{contentHash}")
    int countByContentHash(String contentHash);

    @Insert("INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_year,source_exam,source_name,source_level,usage_type,status,version,content_hash,quality_score) VALUES(#{questionType},#{stem},#{answer},#{analysis},#{difficultyExpected},#{standardTimeSeconds},#{sourceType},#{sourceYear},#{sourceExam},#{sourceName},#{sourceLevel},#{usageType},#{status},#{version},#{contentHash},#{qualityScore})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertQuestion(Question question);

    @Insert("INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(#{questionId},#{optionKey},#{optionText},#{sortNo})")
    int insertOption(QuestionOption option);

    @Insert("INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) VALUES(#{questionId},#{knowledgePointId},'PRIMARY',1)")
    int insertKnowledge(@Param("questionId") Long questionId, @Param("knowledgePointId") Long knowledgePointId);
}
