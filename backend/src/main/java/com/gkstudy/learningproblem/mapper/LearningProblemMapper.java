package com.gkstudy.learningproblem.mapper;

import com.gkstudy.learningproblem.model.LearningProblem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearningProblemMapper {
    @Select("SELECT id,user_id,problem_type,knowledge_point_id,title,description,severity,priority_score,status,evidence_json,root_cause,discovered_time,resolved_time,validation_count,validation_pass_count FROM learning_problem WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgePointId} AND problem_type=#{problemType} FOR UPDATE")
    LearningProblem findForUpdate(@Param("userId") Long userId, @Param("knowledgePointId") Long knowledgePointId, @Param("problemType") String problemType);

    @Select("SELECT lp.id,lp.user_id,lp.problem_type,lp.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,lp.title,lp.description,lp.severity,lp.priority_score,lp.status,lp.evidence_json,lp.root_cause,lp.discovered_time,lp.resolved_time,lp.validation_count,lp.validation_pass_count FROM learning_problem lp JOIN knowledge_point kp ON kp.id=lp.knowledge_point_id WHERE lp.user_id=#{userId} ORDER BY FIELD(lp.status,'CONFIRMED','REOPENED','PROCESSING','VERIFYING','OBSERVING','RESOLVED'),lp.priority_score DESC,lp.id")
    List<LearningProblem> findByUserId(Long userId);

    @Select("SELECT lp.id,lp.user_id,lp.problem_type,lp.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,lp.title,lp.description,lp.severity,lp.priority_score,lp.status,lp.evidence_json,lp.root_cause,lp.discovered_time,lp.resolved_time,lp.validation_count,lp.validation_pass_count,kp.importance AS exam_importance,kp.improvement_potential,kp.transfer_value,ap.confidence_score AS ability_confidence FROM learning_problem lp JOIN knowledge_point kp ON kp.id=lp.knowledge_point_id LEFT JOIN ability_profile ap ON ap.user_id=lp.user_id AND ap.knowledge_point_id=lp.knowledge_point_id WHERE lp.user_id=#{userId}")
    List<LearningProblem> findPriorityCandidates(Long userId);

    @Update("UPDATE learning_problem SET priority_score=#{priorityScore} WHERE id=#{id}")
    int updatePriority(@Param("id") Long id, @Param("priorityScore") java.math.BigDecimal priorityScore);

    @Insert("INSERT INTO learning_problem(user_id,problem_type,knowledge_point_id,title,description,severity,priority_score,status,evidence_json,root_cause,discovered_time,resolved_time,validation_count,validation_pass_count) VALUES(#{userId},#{problemType},#{knowledgePointId},#{title},#{description},#{severity},#{priorityScore},#{status},CAST(#{evidenceJson} AS JSON),#{rootCause},#{discoveredTime},#{resolvedTime},#{validationCount},#{validationPassCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningProblem problem);

    @Update("UPDATE learning_problem SET title=#{title},description=#{description},severity=#{severity},priority_score=#{priorityScore},status=#{status},evidence_json=CAST(#{evidenceJson} AS JSON),root_cause=#{rootCause},resolved_time=#{resolvedTime},validation_count=#{validationCount},validation_pass_count=#{validationPassCount} WHERE id=#{id}")
    int update(LearningProblem problem);

    @Update("UPDATE learning_problem SET root_cause=#{rootCause} WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgePointId}")
    int updateRootCause(@Param("userId") Long userId, @Param("knowledgePointId") Long knowledgePointId,
                        @Param("rootCause") String rootCause);

    @Insert("INSERT INTO learning_problem_history(learning_problem_id,from_status,to_status,evidence_json,change_time) VALUES(#{problem.id},#{fromStatus},#{problem.status},CAST(#{problem.evidenceJson} AS JSON),#{changeTime})")
    int insertHistory(@Param("problem") LearningProblem problem, @Param("fromStatus") String fromStatus, @Param("changeTime") java.time.LocalDateTime changeTime);
}
