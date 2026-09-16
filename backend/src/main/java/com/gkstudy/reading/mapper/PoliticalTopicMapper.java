package com.gkstudy.reading.mapper;

import com.gkstudy.reading.model.PoliticalTopic;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PoliticalTopicMapper {
    @Insert("INSERT INTO political_topic(code,name,knowledge_point_id,description,status,sort_no) VALUES(#{code},#{name},#{knowledgePointId},#{description},#{status},#{sortNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PoliticalTopic topic);

    @Select("SELECT t.*,(SELECT COUNT(*) FROM reading_material m WHERE m.topic_id=t.id) AS material_count FROM political_topic t ORDER BY t.sort_no,t.id")
    List<PoliticalTopic> findAll();

    @Select("SELECT * FROM political_topic WHERE status='ACTIVE' ORDER BY sort_no,id")
    List<PoliticalTopic> findAllActive();

    @Select("SELECT * FROM political_topic WHERE id=#{id}")
    PoliticalTopic findById(Long id);

    @Select("SELECT * FROM political_topic WHERE code=#{code}")
    PoliticalTopic findByCode(String code);

    @Select("SELECT COUNT(*) FROM reading_material WHERE topic_id=#{topicId} AND status=#{status}")
    int countMaterialsByTopicId(@Param("topicId") Long topicId, @Param("status") String status);

    @Select("SELECT COALESCE(MAX(sort_no),0)+1 FROM political_topic")
    int findNextSortNo();
}
