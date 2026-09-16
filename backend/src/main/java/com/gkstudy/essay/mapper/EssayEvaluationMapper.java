package com.gkstudy.essay.mapper;

import com.gkstudy.essay.model.EssayEvaluation;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EssayEvaluationMapper {
    @Insert("INSERT INTO essay_evaluation(essay_answer_id,evaluator,provider,model,prompt_version,request_time,latency_ms,status,confidence,raw_response,failure_message,total_score,dimension_scores,strengths,problems,missing_points,evidence,suggestions) VALUES(#{essayAnswerId},#{evaluator},#{provider},#{model},#{promptVersion},#{requestTime},#{latencyMs},#{status},#{confidence},#{rawResponse},#{failureMessage},#{totalScore},CAST(#{dimensionScoresJson} AS JSON),CAST(#{strengthsJson} AS JSON),CAST(#{problemsJson} AS JSON),CAST(#{missingPointsJson} AS JSON),CAST(#{evidenceJson} AS JSON),CAST(#{suggestionsJson} AS JSON))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EssayEvaluation evaluation);

    @Select("SELECT id,essay_answer_id,evaluator,provider,model,prompt_version,request_time,latency_ms,status,confidence,raw_response,failure_message,total_score,dimension_scores AS dimension_scores_json,strengths AS strengths_json,problems AS problems_json,missing_points AS missing_points_json,evidence AS evidence_json,suggestions AS suggestions_json,create_time FROM essay_evaluation WHERE essay_answer_id=#{answerId} ORDER BY id DESC LIMIT 1")
    EssayEvaluation findByAnswerId(Long answerId);

    @Select("SELECT id,essay_answer_id,evaluator,provider,model,prompt_version,request_time,latency_ms,status,confidence,raw_response,failure_message,total_score,dimension_scores AS dimension_scores_json,strengths AS strengths_json,problems AS problems_json,missing_points AS missing_points_json,evidence AS evidence_json,suggestions AS suggestions_json,create_time FROM essay_evaluation WHERE essay_answer_id=#{answerId} ORDER BY id")
    java.util.List<EssayEvaluation> findAllByAnswerId(Long answerId);

    @Select("SELECT ev.id,ev.essay_answer_id,ev.evaluator,ev.provider,ev.model,ev.prompt_version,ev.request_time,ev.latency_ms,ev.status,ev.confidence,ev.total_score,ev.dimension_scores AS dimension_scores_json,ev.strengths AS strengths_json,ev.problems AS problems_json,ev.missing_points AS missing_points_json,ev.evidence AS evidence_json,ev.suggestions AS suggestions_json,ev.create_time FROM essay_evaluation ev JOIN essay_answer a ON a.id=ev.essay_answer_id WHERE a.user_id=#{userId} AND ev.status='SUCCESS' ORDER BY ev.id DESC LIMIT #{limit}")
    java.util.List<EssayEvaluation> findRecentSuccessfulByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
