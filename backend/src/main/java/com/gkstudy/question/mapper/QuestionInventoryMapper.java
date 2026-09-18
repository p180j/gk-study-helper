package com.gkstudy.question.mapper;

import com.gkstudy.question.service.QuestionInventoryService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 题库库存查询：可用题量、未做题量与模块库存汇总。 */
@Mapper
public interface QuestionInventoryMapper {
    /**
     * 知识点取数范围（三级层级：科目→模块→知识点）：
     * 模块级知识点 = 自身 + 子知识点；三级知识点 = 自身 + 父模块自身 + 同模块兄弟知识点；科目级 = 全部模块。
     * 模块级绝不扩展到其它模块（其父级为科目级，父级的父级为 NULL 时不做兄弟扩展）。
     */
    String KP_SCOPE = "(kp.id=#{knowledgePointId} OR kp.parent_id=#{knowledgePointId} "
            + "OR kp.id=(SELECT self.parent_id FROM knowledge_point self WHERE self.id=#{knowledgePointId}) "
            + "OR (kp.parent_id=(SELECT self2.parent_id FROM knowledge_point self2 WHERE self2.id=#{knowledgePointId}) "
            + "AND (SELECT p.parent_id FROM knowledge_point p WHERE p.id=(SELECT self3.parent_id FROM knowledge_point self3 WHERE self3.id=#{knowledgePointId})) IS NOT NULL))";

    @Select("<script>SELECT COUNT(DISTINCT q.id) FROM question q JOIN question_knowledge qk ON qk.question_id=q.id "
            + "JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id "
            + "WHERE " + KP_SCOPE + " AND q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') "
            + "<if test='purpose == \"VALIDATION\"'>AND (q.source_level IS NULL OR q.source_level NOT IN ('C','D'))</if></script>")
    int countAvailable(@Param("knowledgePointId") Long knowledgePointId, @Param("purpose") String purpose);

    @Select("<script>SELECT COUNT(DISTINCT q.id) FROM question q JOIN question_knowledge qk ON qk.question_id=q.id "
            + "JOIN knowledge_point kp ON kp.id=qk.knowledge_point_id "
            + "WHERE " + KP_SCOPE + " AND q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') "
            + "AND NOT EXISTS (SELECT 1 FROM answer_record ar WHERE ar.user_id=#{userId} AND ar.question_id=q.id) "
            + "<if test='purpose == \"VALIDATION\"'>AND (q.source_level IS NULL OR q.source_level NOT IN ('C','D'))</if></script>")
    int countUnused(@Param("knowledgePointId") Long knowledgePointId, @Param("purpose") String purpose, @Param("userId") Long userId);

    /** 行测五大模块库存汇总（模块自身 + 子知识点范围内用户未做的可训练题，供小程序自由学习页展示）。 */
    @Select("SELECT kp.code AS module_code,kp.name AS module_name,COUNT(DISTINCT q.id) AS available_count "
            + "FROM knowledge_point kp "
            + "LEFT JOIN knowledge_point child ON (child.id=kp.id OR child.parent_id=kp.id) "
            + "LEFT JOIN question_knowledge qk ON qk.knowledge_point_id=child.id "
            + "LEFT JOIN question q ON q.id=qk.question_id AND q.status='ACTIVE' AND q.usage_type IN ('TRAINING','VALIDATION') "
            + "AND (#{userId} IS NULL OR NOT EXISTS (SELECT 1 FROM answer_record ar WHERE ar.user_id=#{userId} AND ar.question_id=q.id)) "
            + "WHERE kp.code IN ('VERBAL','JUDGEMENT','QUANTITY','DATA_ANALYSIS','COMMON_SENSE') "
            + "AND kp.status='ACTIVE' "
            + "GROUP BY kp.id,kp.code,kp.name,kp.sort_no ORDER BY kp.sort_no,kp.id")
    List<QuestionInventoryService.ModuleInventory> moduleSummary(Long userId);

    /** 后台库存总览使用的全部普通可训练题数量。 */
    @Select("SELECT COUNT(*) FROM question WHERE status='ACTIVE' AND usage_type IN ('TRAINING','VALIDATION')")
    int totalAvailable();
}
