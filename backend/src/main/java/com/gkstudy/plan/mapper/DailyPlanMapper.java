package com.gkstudy.plan.mapper;

import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.model.MaintenanceCandidate;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyPlanMapper {
    @Select("SELECT id,user_id,plan_date,planned_minutes,actual_minutes,status,generation_reason,create_time,update_time FROM daily_plan WHERE user_id=#{userId} AND plan_date=#{planDate} FOR UPDATE")
    DailyPlan findForUpdate(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);

    @Select("SELECT dpi.id,dpi.plan_id,dpi.item_type,dpi.target_type,dpi.target_id,dpi.learning_problem_id,dpi.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,lp.problem_type,dpi.purpose,dpi.reason,dpi.planned_minutes,dpi.sort_no,dpi.status FROM daily_plan_item dpi JOIN knowledge_point kp ON kp.id=dpi.knowledge_point_id LEFT JOIN learning_problem lp ON lp.id=dpi.learning_problem_id WHERE dpi.plan_id=#{planId} ORDER BY dpi.sort_no,dpi.id")
    List<DailyPlanItem> findItems(Long planId);

    @Select("SELECT dpi.id,dpi.plan_id,dpi.item_type,dpi.target_type,dpi.target_id,dpi.learning_problem_id,dpi.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,lp.problem_type,dpi.purpose,dpi.reason,dpi.planned_minutes,dpi.sort_no,dpi.status FROM daily_plan_item dpi JOIN daily_plan dp ON dp.id=dpi.plan_id JOIN knowledge_point kp ON kp.id=dpi.knowledge_point_id LEFT JOIN learning_problem lp ON lp.id=dpi.learning_problem_id WHERE dpi.id=#{itemId} AND dp.user_id=#{userId}")
    DailyPlanItem findItemForUser(@Param("itemId") Long itemId, @Param("userId") Long userId);

    @Insert("INSERT INTO daily_plan(user_id,plan_date,planned_minutes,actual_minutes,status,generation_reason) VALUES(#{userId},#{planDate},#{plannedMinutes},#{actualMinutes},#{status},#{generationReason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlan(DailyPlan plan);

    @Update("UPDATE daily_plan SET planned_minutes=#{plannedMinutes},status=#{status},generation_reason=#{generationReason} WHERE id=#{id}")
    int updatePlan(DailyPlan plan);

    @Delete("DELETE FROM daily_plan_item WHERE plan_id=#{planId}")
    int deleteItems(Long planId);

    @Insert("INSERT INTO daily_plan_item(plan_id,item_type,target_type,target_id,learning_problem_id,knowledge_point_id,purpose,reason,planned_minutes,sort_no,status) VALUES(#{planId},#{itemType},#{targetType},#{targetId},#{learningProblemId},#{knowledgePointId},#{purpose},#{reason},#{plannedMinutes},#{sortNo},#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(DailyPlanItem item);

    @Select("SELECT ap.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,kp.importance AS exam_importance,ap.mastery_score,ap.confidence_score,'REVIEW' AS purpose FROM ability_profile ap JOIN knowledge_point kp ON kp.id=ap.knowledge_point_id WHERE ap.user_id=#{userId} AND ap.status IN ('MASTERED','PROFICIENT') ORDER BY kp.importance DESC,ap.last_practice_time,kp.id")
    List<MaintenanceCandidate> findMaintenanceCandidates(Long userId);

    @Select("SELECT kp.id AS knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,kp.importance AS exam_importance,'TRAINING' AS purpose FROM knowledge_point kp JOIN question_knowledge qk ON qk.knowledge_point_id=kp.id JOIN question q ON q.id=qk.question_id WHERE q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') AND NOT EXISTS (SELECT 1 FROM ability_profile ap WHERE ap.user_id=#{userId} AND ap.knowledge_point_id=kp.id) GROUP BY kp.id,kp.code,kp.name,kp.importance ORDER BY kp.importance DESC,kp.id LIMIT 4")
    List<MaintenanceCandidate> findExplorationCandidates(Long userId);
}
