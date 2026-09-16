package com.gkstudy.reading.mapper;

import com.gkstudy.reading.model.ReadingMaterial;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReadingMaterialMapper {
    @Insert("INSERT INTO reading_material(topic_id,title,source,publish_date,content,core_view,problem,cause,solution,policy_logic,standard_expressions,cases,applicable_essay_themes,status) VALUES(#{topicId},#{title},#{source},#{publishDate},#{content},#{coreView},#{problem},#{cause},#{solution},#{policyLogic},CAST(#{standardExpressionsJson} AS JSON),CAST(#{casesJson} AS JSON),CAST(#{applicableEssayThemesJson} AS JSON),#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReadingMaterial material);

    @Update("UPDATE reading_material SET topic_id=#{topicId},title=#{title},source=#{source},publish_date=#{publishDate},content=#{content},core_view=#{coreView},problem=#{problem},cause=#{cause},solution=#{solution},policy_logic=#{policyLogic},standard_expressions=CAST(#{standardExpressionsJson} AS JSON),cases=CAST(#{casesJson} AS JSON),applicable_essay_themes=CAST(#{applicableEssayThemesJson} AS JSON),status=#{status} WHERE id=#{id}")
    int update(ReadingMaterial material);

    @Update("UPDATE reading_material SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE reading_material SET core_view=#{coreView},problem=#{problem},cause=#{cause},solution=#{solution},policy_logic=#{policyLogic},standard_expressions=CAST(#{standardExpressionsJson} AS JSON),cases=CAST(#{casesJson} AS JSON),applicable_essay_themes=CAST(#{applicableEssayThemesJson} AS JSON),topic_candidates=CAST(#{topicCandidatesJson} AS JSON),ai_confidence=#{aiConfidence},ai_provider=#{aiProvider},ai_model=#{aiModel},ai_prompt_version=#{aiPromptVersion},ai_status=#{aiStatus},ai_raw_response=#{aiRawResponse},ai_request_time=#{aiRequestTime},ai_latency_ms=#{aiLatencyMs},status='DRAFT' WHERE id=#{id}")
    int updateAiStructure(ReadingMaterial material);

    @Update("UPDATE reading_material SET ai_status='FAILED',ai_prompt_version=#{aiPromptVersion},ai_request_time=#{aiRequestTime} WHERE id=#{id}")
    int updateAiFailure(ReadingMaterial material);

    @Select("SELECT m.id,m.topic_id,m.title,m.source,m.publish_date,m.content,m.core_view,m.problem,m.cause,m.solution,m.policy_logic,m.standard_expressions AS standard_expressions_json,m.cases AS cases_json,m.applicable_essay_themes AS applicable_essay_themes_json,m.topic_candidates AS topic_candidates_json,m.ai_confidence,m.ai_provider,m.ai_model,m.ai_prompt_version,m.ai_status,m.ai_raw_response,m.ai_request_time,m.ai_latency_ms,m.status,m.create_time,m.update_time,t.code AS topic_code,t.name AS topic_name FROM reading_material m JOIN political_topic t ON t.id=m.topic_id WHERE m.id=#{id}")
    ReadingMaterial findById(Long id);

    @Select("<script>SELECT m.id,m.topic_id,m.title,m.source,m.publish_date,m.core_view,m.status,m.create_time,m.update_time,t.code AS topic_code,t.name AS topic_name FROM reading_material m JOIN political_topic t ON t.id=m.topic_id <where>"
            + "<if test='keyword != null and keyword != \"\"'>AND (m.title LIKE CONCAT('%',#{keyword},'%') OR m.source LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='topicId != null'>AND m.topic_id=#{topicId}</if>"
            + "<if test='status != null and status != \"\"'>AND m.status=#{status}</if>"
            + "</where> ORDER BY m.id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<ReadingMaterial> adminList(@Param("keyword") String keyword, @Param("topicId") Long topicId,
                                    @Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM reading_material m <where>"
            + "<if test='keyword != null and keyword != \"\"'>AND (m.title LIKE CONCAT('%',#{keyword},'%') OR m.source LIKE CONCAT('%',#{keyword},'%'))</if>"
            + "<if test='topicId != null'>AND m.topic_id=#{topicId}</if>"
            + "<if test='status != null and status != \"\"'>AND m.status=#{status}</if>"
            + "</where></script>")
    int adminCount(@Param("keyword") String keyword, @Param("topicId") Long topicId, @Param("status") String status);

    @Select("SELECT m.id,m.topic_id,m.title,m.source,m.publish_date,m.core_view,t.code AS topic_code,t.name AS topic_name FROM reading_material m JOIN political_topic t ON t.id=m.topic_id WHERE m.topic_id=#{topicId} AND m.status='PUBLISHED' AND t.status='ACTIVE' ORDER BY m.publish_date DESC,m.id DESC LIMIT #{limit}")
    List<ReadingMaterial> listPublishedByTopicId(@Param("topicId") Long topicId, @Param("limit") int limit);

    @Select("SELECT m.id,m.topic_id,m.title,m.source,m.publish_date,m.content,m.core_view,m.problem,m.cause,m.solution,m.policy_logic,m.standard_expressions AS standard_expressions_json,m.cases AS cases_json,m.applicable_essay_themes AS applicable_essay_themes_json,m.status,m.create_time,m.update_time,t.code AS topic_code,t.name AS topic_name FROM reading_material m JOIN political_topic t ON t.id=m.topic_id WHERE t.knowledge_point_id=#{topicKnowledgePointId} AND m.status=#{status} AND t.status='ACTIVE' ORDER BY m.publish_date DESC,m.id DESC LIMIT #{limit}")
    List<ReadingMaterial> findByTopicKpIdAndStatus(@Param("topicKnowledgePointId") Long topicKnowledgePointId,
                                                   @Param("status") String status, @Param("limit") int limit);

    @Select("SELECT m.id,m.topic_id,m.title,m.source,m.publish_date,m.content,m.core_view,t.code AS topic_code,t.name AS topic_name,r.read_status,r.favorite,r.mastery_level FROM reading_material m JOIN political_topic t ON t.id=m.topic_id AND t.knowledge_point_id=#{topicKnowledgePointId} AND t.status='ACTIVE' LEFT JOIN reading_record r ON r.material_id=m.id AND r.user_id=#{userId} WHERE m.status=#{status} ORDER BY (CASE WHEN r.read_status IS NULL OR r.read_status<>'COMPLETED' THEN 0 ELSE 1 END),m.publish_date DESC,m.id DESC LIMIT #{limit}")
    List<ReadingMaterial> findFirstUnreadByTopicKpId(@Param("userId") Long userId,
                                                     @Param("topicKnowledgePointId") Long topicKnowledgePointId,
                                                     @Param("status") String status, @Param("limit") int limit);
}
