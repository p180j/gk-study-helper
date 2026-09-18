package com.gkstudy.content.mapper;

import com.gkstudy.content.dto.InventoryItem;
import com.gkstudy.content.dto.InventoryOverviewView.Overview;
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

    /** 库存总览：可训练 / 模考专用 / 申论 / AI 生成 / 待人工检查 / 采集失败来源 */
    @Select("SELECT "
            + "(SELECT COUNT(*) FROM question WHERE status='ACTIVE' AND usage_type IN ('TRAINING','VALIDATION')) AS trainableTotal, "
            + "(SELECT COUNT(*) FROM question WHERE status='ACTIVE' AND usage_type='MOCK_RESERVED') AS mockReservedTotal, "
            + "(SELECT COUNT(*) FROM essay_question WHERE status='ACTIVE') AS essayTotal, "
            + "(SELECT COUNT(*) FROM question WHERE status='ACTIVE' AND source_type IN ('AI_GENERATED','AI_VARIANT')) AS aiTotal, "
            + "(SELECT COUNT(*) FROM content_staging WHERE status='NEEDS_REVIEW') AS needsReviewCount, "
            + "(SELECT COUNT(*) FROM content_source WHERE status='FAILED') AS crawlFailedCount")
    Overview overviewCounts();

    /** 知识点级库存：总量 / 可训练 / 未使用（从未出现在答题记录），用于模块子项 */
    @Select("SELECT kp.id, kp.code, kp.name, kp.parent_id AS parentId, "
            + "COUNT(DISTINCT q.id) AS totalQuestions, "
            + "COUNT(DISTINCT CASE WHEN q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') THEN q.id END) AS trainable, "
            + "COUNT(DISTINCT CASE WHEN q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') "
            + "AND ar.question_id IS NULL THEN q.id END) AS unused "
            + "FROM knowledge_point kp "
            + "LEFT JOIN question_knowledge qk ON qk.knowledge_point_id = kp.id "
            + "LEFT JOIN question q ON q.id = qk.question_id "
            + "LEFT JOIN (SELECT DISTINCT question_id FROM answer_record) ar ON ar.question_id = q.id "
            + "WHERE kp.status='ACTIVE' "
            + "GROUP BY kp.id, kp.code, kp.name, kp.parent_id, kp.sort_no "
            + "ORDER BY kp.sort_no, kp.id")
    List<InventoryItem> pointStats();

    /** 一级模块库存：模块自身 + 子知识点范围去重统计 */
    @Select("SELECT m.id, m.code, m.name, "
            + "COUNT(DISTINCT q.id) AS totalQuestions, "
            + "COUNT(DISTINCT CASE WHEN q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') THEN q.id END) AS trainable, "
            + "COUNT(DISTINCT CASE WHEN q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') "
            + "AND ar.question_id IS NULL THEN q.id END) AS unused "
            + "FROM knowledge_point m "
            + "LEFT JOIN knowledge_point c ON c.parent_id = m.id "
            + "LEFT JOIN question_knowledge qk ON qk.knowledge_point_id IN (m.id, c.id) "
            + "LEFT JOIN question q ON q.id = qk.question_id "
            + "LEFT JOIN (SELECT DISTINCT question_id FROM answer_record) ar ON ar.question_id = q.id "
            + "WHERE m.code IN ('VERBAL','JUDGEMENT','QUANTITY','DATA_ANALYSIS','COMMON_SENSE') AND m.status='ACTIVE' "
            + "GROUP BY m.id, m.code, m.name, m.sort_no "
            + "ORDER BY m.sort_no, m.id")
    List<InventoryItem> moduleStats();
}
