package com.gkstudy.content.mapper;

import com.gkstudy.content.dto.InventoryItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContentInventoryMapper {
    /** 各知识点题库库存：总量 / 高质量题 / 未使用高质量题（从未出现在任何答题记录） */
    @Select("SELECT kp.id, kp.code, kp.name, kp.exam_type AS examType, kp.parent_id AS parentId, "
            + "COUNT(DISTINCT q.id) AS totalQuestions, "
            + "COUNT(DISTINCT CASE WHEN q.source_type IN ('HISTORICAL','IMPORTED') THEN q.id END) AS qualityQuestions, "
            + "COUNT(DISTINCT CASE WHEN q.source_type IN ('HISTORICAL','IMPORTED') AND ar.question_id IS NULL THEN q.id END) AS unusedQualityQuestions "
            + "FROM knowledge_point kp "
            + "LEFT JOIN question_knowledge qk ON qk.knowledge_point_id = kp.id "
            + "LEFT JOIN question q ON q.id = qk.question_id AND q.status='ACTIVE' "
            + "LEFT JOIN (SELECT DISTINCT question_id FROM answer_record) ar ON ar.question_id = q.id "
            + "WHERE kp.status='ACTIVE' "
            + "GROUP BY kp.id, kp.code, kp.name, kp.exam_type, kp.parent_id, kp.sort_no "
            + "ORDER BY kp.sort_no, kp.id")
    List<InventoryItem> inventory();
}
