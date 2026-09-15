-- 当前开发增量区。完成验证后须同步合并至 schema.sql，并清空本文件中的已合并语句。

ALTER TABLE knowledge_point
    ADD COLUMN improvement_potential DECIMAL(5,2) NOT NULL DEFAULT 0.50 AFTER importance,
    ADD COLUMN transfer_value DECIMAL(5,2) NOT NULL DEFAULT 0.50 AFTER improvement_potential;

UPDATE knowledge_point SET improvement_potential=0.90,transfer_value=0.95 WHERE code='AVG_GROWTH_RATE';

INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value)
SELECT id,'LOGICAL_JUDGEMENT','逻辑判断','CIVIL_SERVICE',3,1,0.95,0.75,0.85 FROM knowledge_point
WHERE code='JUDGEMENT' AND NOT EXISTS (SELECT 1 FROM knowledge_point WHERE code='LOGICAL_JUDGEMENT');

INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value)
SELECT id,'DEFINITION_JUDGEMENT','定义判断','CIVIL_SERVICE',3,2,0.80,0.70,0.60 FROM knowledge_point
WHERE code='JUDGEMENT' AND NOT EXISTS (SELECT 1 FROM knowledge_point WHERE code='DEFINITION_JUDGEMENT');

INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value)
SELECT id,'PERMUTATION_COMBINATION','排列组合','CIVIL_SERVICE',3,1,0.65,0.35,0.30 FROM knowledge_point
WHERE code='QUANTITY' AND NOT EXISTS (SELECT 1 FROM knowledge_point WHERE code='PERMUTATION_COMBINATION');

ALTER TABLE daily_plan_item
    ADD COLUMN learning_problem_id BIGINT AFTER target_id,
    ADD COLUMN knowledge_point_id BIGINT AFTER learning_problem_id,
    ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'TRAINING' AFTER knowledge_point_id,
    ADD COLUMN reason VARCHAR(500) NOT NULL DEFAULT '' AFTER purpose;
