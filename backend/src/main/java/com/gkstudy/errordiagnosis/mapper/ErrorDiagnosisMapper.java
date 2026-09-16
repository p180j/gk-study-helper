package com.gkstudy.errordiagnosis.mapper;

import com.gkstudy.errordiagnosis.model.ErrorDiagnosis;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ErrorDiagnosisMapper {
    @Select("SELECT * FROM error_diagnosis WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgePointId} AND suspected_cause=#{suspectedCause} FOR UPDATE")
    ErrorDiagnosis findForUpdate(@Param("userId") Long userId, @Param("knowledgePointId") Long knowledgePointId,
                                 @Param("suspectedCause") String suspectedCause);

    @Select("SELECT * FROM error_diagnosis WHERE id=#{id} AND user_id=#{userId} FOR UPDATE")
    ErrorDiagnosis findByIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM error_diagnosis WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgePointId} AND status<>'REJECTED' ORDER BY FIELD(status,'CONFIRMED','PENDING_CONFIRMATION'),confidence DESC,id LIMIT 1")
    ErrorDiagnosis findMain(@Param("userId") Long userId, @Param("knowledgePointId") Long knowledgePointId);

    @Insert("INSERT INTO error_diagnosis(user_id,answer_record_id,question_id,knowledge_point_id,suspected_cause,evidence_json,confidence,confirmed_by_user,status,occurrence_count,selected_option_key) VALUES(#{userId},#{answerRecordId},#{questionId},#{knowledgePointId},#{suspectedCause},CAST(#{evidenceJson} AS JSON),#{confidence},#{confirmedByUser},#{status},#{occurrenceCount},#{selectedOptionKey})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ErrorDiagnosis diagnosis);

    @Update("UPDATE error_diagnosis SET answer_record_id=#{answerRecordId},question_id=#{questionId},evidence_json=CAST(#{evidenceJson} AS JSON),confidence=#{confidence},occurrence_count=#{occurrenceCount},selected_option_key=#{selectedOptionKey} WHERE id=#{id}")
    int updateEvidence(ErrorDiagnosis diagnosis);

    @Update("UPDATE error_diagnosis SET confirmed_by_user=#{confirmedByUser},status=#{status},confidence=#{confidence} WHERE id=#{id}")
    int updateDecision(ErrorDiagnosis diagnosis);

    @Update("UPDATE error_diagnosis SET ai_explanation=#{aiExplanation},ai_provider=#{aiProvider},ai_model=#{aiModel},ai_prompt_version=#{aiPromptVersion},ai_confidence=#{aiConfidence},ai_status=#{aiStatus},ai_raw_response=#{aiRawResponse},ai_request_time=#{aiRequestTime},ai_latency_ms=#{aiLatencyMs} WHERE id=#{id}")
    int updateAiAnalysis(ErrorDiagnosis diagnosis);

    @Select("SELECT * FROM error_diagnosis WHERE user_id=#{userId} AND status<>'REJECTED' ORDER BY update_time DESC,id DESC LIMIT #{limit}")
    java.util.List<ErrorDiagnosis> findRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
