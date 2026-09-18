package com.gkstudy.ability.mapper;

import com.gkstudy.ability.model.AbilityProfile;
import com.gkstudy.ability.engine.AbilityConstants;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AbilityMapper {
    @Select("SELECT id,user_id,knowledge_point_id,mastery_score,speed_score,stability_score,confidence_score,sample_count,status,last_practice_time FROM ability_profile WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgePointId} FOR UPDATE")
    AbilityProfile findForUpdate(@Param("userId") Long userId, @Param("knowledgePointId") Long knowledgePointId);

    @Select("SELECT ap.id,ap.user_id,ap.knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,COALESCE(root.code,kp.code) AS exam_section,ap.mastery_score,ap.speed_score,ap.stability_score,ap.confidence_score,ap.sample_count,ap.status,ap.last_practice_time FROM ability_profile ap JOIN knowledge_point kp ON kp.id=ap.knowledge_point_id LEFT JOIN knowledge_point root ON root.id=kp.parent_id AND root.level=1 WHERE ap.user_id=#{userId} ORDER BY kp.sort_no,ap.knowledge_point_id")
    List<AbilityProfile> findByUserId(Long userId);

    @Select("SELECT MAX(a.id) AS id,#{userId} AS user_id,kp.id AS knowledge_point_id,kp.code AS knowledge_point_code,kp.name AS knowledge_point_name,root.code AS exam_section," +
            "COALESCE(ROUND(SUM(a.mastery_score*a.sample_count)/NULLIF(SUM(a.sample_count),0),2),0) AS mastery_score," +
            "COALESCE(ROUND(SUM(a.speed_score*a.sample_count)/NULLIF(SUM(a.sample_count),0),2),0) AS speed_score," +
            "COALESCE(ROUND(SUM(a.stability_score*a.sample_count)/NULLIF(SUM(a.sample_count),0),2),0) AS stability_score," +
            "COALESCE(ROUND(SUM(a.confidence_score*a.sample_count)/NULLIF(SUM(a.sample_count),0),2),0) AS confidence_score," +
            "COALESCE(SUM(a.sample_count),0) AS sample_count,CASE WHEN COALESCE(SUM(a.sample_count),0)=0 THEN 'UNASSESSED' WHEN COALESCE(SUM(a.sample_count),0)<" + AbilityConstants.ASSESSMENT_MIN_SAMPLE_COUNT + " THEN 'ASSESSING' ELSE 'ASSESSED' END AS status,MAX(a.last_practice_time) AS last_practice_time " +
            "FROM knowledge_point kp JOIN knowledge_point root ON root.id=kp.parent_id AND root.code IN ('XINGCE','SHENLUN') " +
            "LEFT JOIN (SELECT ap.*,CASE WHEN leaf.level=2 THEN leaf.id ELSE leaf.parent_id END AS core_knowledge_point_id FROM ability_profile ap JOIN knowledge_point leaf ON leaf.id=ap.knowledge_point_id WHERE ap.user_id=#{userId} AND leaf.level IN (2,3)) a ON a.core_knowledge_point_id=kp.id " +
            "WHERE kp.status='ACTIVE' AND kp.level=2 AND kp.code<>'ESSAY_THEMES' GROUP BY kp.id,kp.code,kp.name,root.code,root.sort_no,kp.sort_no ORDER BY root.sort_no,kp.sort_no,kp.id")
    List<AbilityProfile> findCompleteMap(Long userId);

    @Select("SELECT t.core_id AS knowledge_point_id," +
            "ROUND(SUM(cur.mastery_score*cur.sample_count)/NULLIF(SUM(cur.sample_count),0),2)" +
            "-ROUND(SUM(t.mastery_score*t.sample_count)/NULLIF(SUM(t.sample_count),0),2) AS mastery_trend " +
            "FROM (SELECT CASE WHEN leaf.level=2 THEN leaf.id ELSE leaf.parent_id END AS core_id,prev.knowledge_point_id,prev.mastery_score,prev.sample_count " +
            "FROM (SELECT h.knowledge_point_id,h.mastery_score,h.sample_count FROM ability_history h " +
            "JOIN (SELECT knowledge_point_id,MAX(id) AS latest_id FROM ability_history WHERE user_id=#{userId} GROUP BY knowledge_point_id) latest " +
            "ON latest.knowledge_point_id=h.knowledge_point_id WHERE h.user_id=#{userId} " +
            "AND h.id=(SELECT MAX(h2.id) FROM ability_history h2 WHERE h2.user_id=#{userId} AND h2.knowledge_point_id=h.knowledge_point_id AND h2.id<latest.latest_id)) prev " +
            "JOIN knowledge_point leaf ON leaf.id=prev.knowledge_point_id) t " +
            "JOIN ability_profile cur ON cur.user_id=#{userId} AND cur.knowledge_point_id=t.knowledge_point_id " +
            "GROUP BY t.core_id")
    List<AbilityProfile> findMasteryTrends(Long userId);

    @Insert("INSERT INTO ability_profile(user_id,knowledge_point_id,mastery_score,speed_score,stability_score,confidence_score,sample_count,status,last_practice_time) VALUES(#{userId},#{knowledgePointId},#{masteryScore},#{speedScore},#{stabilityScore},#{confidenceScore},#{sampleCount},#{status},#{lastPracticeTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProfile(AbilityProfile profile);

    @Update("UPDATE ability_profile SET mastery_score=#{masteryScore},speed_score=#{speedScore},stability_score=#{stabilityScore},confidence_score=#{confidenceScore},sample_count=#{sampleCount},status=#{status},last_practice_time=#{lastPracticeTime} WHERE id=#{id}")
    int updateProfile(AbilityProfile profile);

    @Insert("INSERT INTO ability_history(user_id,knowledge_point_id,mastery_score,speed_score,stability_score,confidence_score,sample_count,snapshot_time) VALUES(#{userId},#{knowledgePointId},#{masteryScore},#{speedScore},#{stabilityScore},#{confidenceScore},#{sampleCount},#{lastPracticeTime})")
    int insertHistory(AbilityProfile profile);

    @Delete("DELETE FROM ability_profile WHERE user_id=#{userId}")
    int deleteProfilesByUserId(Long userId);
}
