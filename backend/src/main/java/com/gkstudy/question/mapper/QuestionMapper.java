package com.gkstudy.question.mapper;

import com.gkstudy.question.model.KnowledgePointRef;
import com.gkstudy.question.model.Question;
import com.gkstudy.question.model.QuestionOption;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionMapper {
    @Select("<script>SELECT * FROM question <where><if test='status != null and status != \"\"'>status=#{status}</if></where> ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<Question> findAll(@Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM question WHERE id=#{id}")
    Question findById(Long id);

    @Select("SELECT id,question_id,option_key,option_text,sort_no FROM question_option WHERE question_id=#{questionId} ORDER BY sort_no")
    List<QuestionOption> findOptions(Long questionId);

    @Select("SELECT kp.id,kp.code,kp.name,qk.relation_type,qk.weight FROM question_knowledge qk JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id WHERE qk.question_id=#{questionId} ORDER BY qk.weight DESC,kp.id")
    List<KnowledgePointRef> findKnowledgePoints(Long questionId);

    @Select("SELECT id,code,name FROM knowledge_point WHERE code=#{code} AND status='ACTIVE'")
    KnowledgePointRef findKnowledgePointByCode(String code);

    @Select("SELECT COUNT(*) FROM question WHERE content_hash=#{contentHash}")
    int countByContentHash(String contentHash);

    @Insert("INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_year,source_exam,source_name,usage_type,status,version,content_hash) VALUES(#{questionType},#{stem},#{answer},#{analysis},#{difficultyExpected},#{standardTimeSeconds},#{sourceType},#{sourceYear},#{sourceExam},#{sourceName},#{usageType},#{status},#{version},#{contentHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertQuestion(Question question);

    @Insert("INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(#{questionId},#{optionKey},#{optionText},#{sortNo})")
    int insertOption(QuestionOption option);

    @Insert("INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) VALUES(#{questionId},#{knowledgePointId},'PRIMARY',1)")
    int insertKnowledge(@Param("questionId") Long questionId, @Param("knowledgePointId") Long knowledgePointId);
}
