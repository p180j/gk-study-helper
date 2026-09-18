package com.gkstudy.practice.mapper;

import com.gkstudy.practice.model.AnswerRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AnswerRecordMapper {
    String RECORD_COLUMNS = "id,user_id,question_id,plan_item_id,question_version,practice_type,user_answer,correct_answer_snapshot,is_correct AS correct,duration_ms,standard_time_seconds_snapshot,difficulty_snapshot,knowledge_snapshot,confidence_type,error_type,answer_time";

    @Insert("INSERT INTO answer_record(user_id,question_id,plan_item_id,question_version,practice_type,user_answer,correct_answer_snapshot,is_correct,duration_ms,standard_time_seconds_snapshot,difficulty_snapshot,knowledge_snapshot,confidence_type,error_type,answer_time) VALUES(#{userId},#{questionId},#{planItemId},#{questionVersion},#{practiceType},#{userAnswer},#{correctAnswerSnapshot},#{correct},#{durationMs},#{standardTimeSecondsSnapshot},#{difficultySnapshot},CAST(#{knowledgeSnapshot} AS JSON),#{confidenceType},#{errorType},#{answerTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnswerRecord record);

    @Select("SELECT id FROM answer_record WHERE user_id=#{userId} AND plan_item_id=#{planItemId} AND question_id=#{questionId} LIMIT 1")
    Long findIdByUserPlanQuestion(@Param("userId") Long userId, @Param("planItemId") Long planItemId, @Param("questionId") Long questionId);

    @Select("SELECT " + RECORD_COLUMNS + " FROM answer_record WHERE id=#{id}")
    AnswerRecord findById(Long id);

    @Select("SELECT " + RECORD_COLUMNS + " FROM answer_record WHERE user_id=#{userId} ORDER BY answer_time DESC,id DESC LIMIT #{limit} OFFSET #{offset}")
    List<AnswerRecord> findByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT " + RECORD_COLUMNS + " FROM answer_record WHERE user_id=#{userId} ORDER BY answer_time,id")
    List<AnswerRecord> findAllByUserId(Long userId);

    @Update("UPDATE answer_record SET error_type=#{errorType} WHERE id=#{recordId} AND user_id=#{userId}")
    int updateErrorType(@Param("userId") Long userId, @Param("recordId") Long recordId, @Param("errorType") String errorType);
}
