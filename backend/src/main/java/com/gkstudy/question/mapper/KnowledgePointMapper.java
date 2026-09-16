package com.gkstudy.question.mapper;

import com.gkstudy.question.model.KnowledgePointView;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface KnowledgePointMapper {
    @Select("SELECT kp.id,kp.parent_id,parent.name AS parent_name,kp.code,kp.name,kp.level,kp.sort_no,kp.status,kp.importance,kp.improvement_potential,kp.transfer_value FROM knowledge_point kp LEFT JOIN knowledge_point parent ON parent.id=kp.parent_id ORDER BY kp.level,COALESCE(kp.parent_id,0),kp.sort_no,kp.id")
    List<KnowledgePointView> findAll();

    @Select("SELECT id FROM knowledge_point WHERE code=#{code}")
    Long findIdByCode(String code);

    @Select("SELECT COALESCE(MAX(sort_no),0)+1 FROM knowledge_point WHERE parent_id=#{parentId}")
    int findNextSortNo(Long parentId);

    @Insert("INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES(#{parentId},#{code},#{name},#{examType},#{level},#{sortNo},#{importance})")
    int insert(@Param("parentId") Long parentId, @Param("code") String code, @Param("name") String name,
               @Param("examType") String examType, @Param("level") int level, @Param("sortNo") int sortNo,
               @Param("importance") BigDecimal importance);
}
