package com.gkstudy.reading.mapper;

import com.gkstudy.reading.model.ReadingRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ReadingRecordMapper {
    @Select("SELECT id,user_id,material_id,read_status,favorite,mastery_level,duration_ms,first_read_time,last_read_time FROM reading_record WHERE user_id=#{userId} AND material_id=#{materialId} FOR UPDATE")
    ReadingRecord findForUpdate(@Param("userId") Long userId, @Param("materialId") Long materialId);

    @Select("SELECT id,user_id,material_id,read_status,favorite,mastery_level,duration_ms,first_read_time,last_read_time FROM reading_record WHERE user_id=#{userId} AND material_id=#{materialId}")
    ReadingRecord findByUserIdAndMaterialId(@Param("userId") Long userId, @Param("materialId") Long materialId);

    @Insert("INSERT INTO reading_record(user_id,material_id,read_status,favorite,mastery_level,duration_ms,first_read_time,last_read_time) VALUES(#{userId},#{materialId},#{readStatus},#{favorite},#{masteryLevel},#{durationMs},#{firstReadTime},#{lastReadTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReadingRecord record);

    @Update("UPDATE reading_record SET read_status=#{readStatus},favorite=#{favorite},mastery_level=#{masteryLevel},duration_ms=#{durationMs},last_read_time=#{lastReadTime} WHERE id=#{id}")
    int update(ReadingRecord record);

    @Select("SELECT COUNT(*) FROM reading_record r JOIN reading_material m ON m.id=r.material_id JOIN political_topic t ON t.id=m.topic_id WHERE r.user_id=#{userId} AND r.read_status='COMPLETED' AND m.status='PUBLISHED' AND t.knowledge_point_id=#{topicKnowledgePointId}")
    int countCompletedByUserAndTopicKpId(@Param("userId") Long userId, @Param("topicKnowledgePointId") Long topicKnowledgePointId);

    @Select("SELECT COUNT(*) FROM reading_record WHERE user_id=#{userId} AND read_status='COMPLETED'")
    int countCompletedByUser(Long userId);
}
