package com.gkstudy.plan.mapper;

import com.gkstudy.plan.model.DailyPlan;
import com.gkstudy.plan.model.DailyPlanItem;
import com.gkstudy.plan.model.MaintenanceCandidate;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyPlanMapper {
    String ITEM_COLUMNS = "dpi.id,dpi.plan_id,dpi.item_type,dpi.target_type,dpi.target_id,dpi.learning_problem_id,dpi.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,lp.problem_type,dpi.purpose,dpi.reason,dpi.planned_minutes,dpi.target_question_count,dpi.completed_question_count,dpi.sort_no,dpi.status";

    @Select("SELECT id,user_id,plan_date,planned_minutes,actual_minutes,status,generation_reason,create_time,update_time FROM daily_plan WHERE user_id=#{userId} AND plan_date=#{planDate} FOR UPDATE")
    DailyPlan findForUpdate(@Param("userId") Long userId, @Param("planDate") LocalDate planDate);

    @Select("SELECT " + ITEM_COLUMNS + " FROM daily_plan_item dpi JOIN knowledge_point kp ON kp.id=dpi.knowledge_point_id LEFT JOIN learning_problem lp ON lp.id=dpi.learning_problem_id WHERE dpi.plan_id=#{planId} ORDER BY dpi.sort_no,dpi.id")
    List<DailyPlanItem> findItems(Long planId);

    @Select("SELECT " + ITEM_COLUMNS + " FROM daily_plan_item dpi JOIN daily_plan dp ON dp.id=dpi.plan_id JOIN knowledge_point kp ON kp.id=dpi.knowledge_point_id LEFT JOIN learning_problem lp ON lp.id=dpi.learning_problem_id WHERE dpi.id=#{itemId} AND dp.user_id=#{userId}")
    DailyPlanItem findItemForUser(@Param("itemId") Long itemId, @Param("userId") Long userId);

    @Select("SELECT " + ITEM_COLUMNS + " FROM daily_plan_item dpi JOIN knowledge_point kp ON kp.id=dpi.knowledge_point_id LEFT JOIN learning_problem lp ON lp.id=dpi.learning_problem_id WHERE dpi.id=#{itemId}")
    DailyPlanItem findItemById(Long itemId);

    @Insert("INSERT INTO daily_plan(user_id,plan_date,planned_minutes,actual_minutes,status,generation_reason) VALUES(#{userId},#{planDate},#{plannedMinutes},#{actualMinutes},#{status},#{generationReason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPlan(DailyPlan plan);

    @Update("UPDATE daily_plan SET planned_minutes=#{plannedMinutes},status=#{status},generation_reason=#{generationReason} WHERE id=#{id}")
    int updatePlan(DailyPlan plan);

    @Delete("DELETE FROM daily_plan_item WHERE plan_id=#{planId}")
    int deleteItems(Long planId);

    @Insert("INSERT INTO daily_plan_item(plan_id,item_type,target_type,target_id,learning_problem_id,knowledge_point_id,purpose,reason,planned_minutes,target_question_count,completed_question_count,sort_no,status) VALUES(#{planId},#{itemType},#{targetType},#{targetId},#{learningProblemId},#{knowledgePointId},#{purpose},#{reason},#{plannedMinutes},#{targetQuestionCount},COALESCE(#{completedQuestionCount},0),#{sortNo},#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(DailyPlanItem item);

    /** 任务进度回写：完成数 +1，达到目标题量即置 COMPLETED，已 COMPLETED 的任务不再推进。
     * 注意：status 放在前面用旧值判断（MySQL SET 按列顺序求值，后列会读到前列的新值）。 */
    @Update("UPDATE daily_plan_item SET status=IF(completed_question_count+1>=target_question_count AND target_question_count>0,'COMPLETED','IN_PROGRESS'), completed_question_count=completed_question_count+1 WHERE id=#{itemId} AND status<>'COMPLETED'")
    int updateProgress(Long itemId);

    /** 所有题目类任务完成后，才把当天计划标记为完成。 */
    @Update("UPDATE daily_plan dp SET dp.status='COMPLETED' WHERE dp.id=(SELECT plan_id FROM daily_plan_item WHERE id=#{itemId}) "
            + "AND NOT EXISTS (SELECT 1 FROM daily_plan_item dpi WHERE dpi.plan_id=dp.id AND dpi.item_type IN ('QUESTION_SET','REVIEW') AND dpi.status<>'COMPLETED')")
    int completePlanIfAllItemsCompleted(Long itemId);

    @Select("SELECT ap.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,kp.importance AS exam_importance,ap.mastery_score,ap.confidence_score,'MAINTENANCE' AS purpose FROM ability_profile ap JOIN knowledge_point kp ON kp.id=ap.knowledge_point_id WHERE ap.user_id=#{userId} AND ap.status IN ('MASTERED','PROFICIENT') ORDER BY kp.importance DESC,ap.last_practice_time,kp.id")
    List<MaintenanceCandidate> findMaintenanceCandidates(Long userId);

    @Select("SELECT kp.id AS knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,kp.importance AS exam_importance,COALESCE(ap.mastery_score,0) AS mastery_score,COALESCE(ap.confidence_score,0) AS confidence_score,'ASSESSMENT' AS purpose FROM knowledge_point kp JOIN question_knowledge qk ON qk.knowledge_point_id=kp.id JOIN question q ON q.id=qk.question_id LEFT JOIN ability_profile ap ON ap.user_id=#{userId} AND ap.knowledge_point_id=kp.id WHERE q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') AND (ap.id IS NULL OR ap.sample_count=0) GROUP BY kp.id,kp.code,kp.name,kp.importance,ap.mastery_score,ap.confidence_score ORDER BY kp.importance DESC,COALESCE(ap.confidence_score,0),kp.id LIMIT 8")
    List<MaintenanceCandidate> findExplorationCandidates(Long userId);
}
