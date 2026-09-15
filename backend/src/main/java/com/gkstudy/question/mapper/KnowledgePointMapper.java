package com.gkstudy.question.mapper;

import com.gkstudy.question.model.KnowledgePointView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgePointMapper {
    @Select("SELECT kp.id,kp.parent_id,parent.name AS parent_name,kp.code,kp.name,kp.level,kp.sort_no,kp.status,kp.importance,kp.improvement_potential,kp.transfer_value FROM knowledge_point kp LEFT JOIN knowledge_point parent ON parent.id=kp.parent_id ORDER BY kp.level,COALESCE(kp.parent_id,0),kp.sort_no,kp.id")
    List<KnowledgePointView> findAll();
}
