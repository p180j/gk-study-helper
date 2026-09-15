package com.gkstudy.ability.mapper;

import com.gkstudy.ability.model.AbilityProfile;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AbilityMapper {
    @Select("SELECT id,user_id,knowledge_point_id,mastery_score,speed_score,stability_score,confidence_score,sample_count,status,last_practice_time FROM ability_profile WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgePointId} FOR UPDATE")
    AbilityProfile findForUpdate(@Param("userId") Long userId, @Param("knowledgePointId") Long knowledgePointId);

    @Select("SELECT id,user_id,knowledge_point_id,mastery_score,speed_score,stability_score,confidence_score,sample_count,status,last_practice_time FROM ability_profile WHERE user_id=#{userId} ORDER BY knowledge_point_id")
    List<AbilityProfile> findByUserId(Long userId);

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
