INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES (NULL,'XINGCE','行测','CIVIL_SERVICE',1,1,1.00);
SET @root=LAST_INSERT_ID();
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES
(@root,'VERBAL','言语理解','CIVIL_SERVICE',2,1,0.90),(@root,'JUDGEMENT','判断推理','CIVIL_SERVICE',2,2,0.90),(@root,'QUANTITY','数量关系','CIVIL_SERVICE',2,3,0.80),(@root,'DATA_ANALYSIS','资料分析','CIVIL_SERVICE',2,4,1.00),(@root,'COMMON_SENSE','常识判断','CIVIL_SERVICE',2,5,0.60);
SET @data=(SELECT id FROM knowledge_point WHERE code='DATA_ANALYSIS');
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES
(@data,'DATA_LOCATION','数据定位','CIVIL_SERVICE',3,1,0.85),(@data,'GROWTH_RATE','增长率','CIVIL_SERVICE',3,2,1.00),(@data,'GROWTH_AMOUNT','增长量','CIVIL_SERVICE',3,3,0.95),(@data,'BASE_AMOUNT','基期量','CIVIL_SERVICE',3,4,1.00),(@data,'CURRENT_AMOUNT','现期量','CIVIL_SERVICE',3,5,0.90),(@data,'AVG_GROWTH_RATE','年均增长率','CIVIL_SERVICE',3,6,1.00),(@data,'CONTRIBUTION_RATE','贡献率','CIVIL_SERVICE',3,7,0.85),(@data,'PULL_GROWTH_RATE','拉动增长率','CIVIL_SERVICE',3,8,0.85),(@data,'CURRENT_PROPORTION','现期比重','CIVIL_SERVICE',3,9,0.95),(@data,'BASE_PROPORTION','基期比重','CIVIL_SERVICE',3,10,0.95),(@data,'PROPORTION_CHANGE','比重变化','CIVIL_SERVICE',3,11,0.95),(@data,'AVERAGE','平均数','CIVIL_SERVICE',3,12,0.90),(@data,'MULTIPLE','倍数','CIVIL_SERVICE',3,13,0.85),(@data,'COMPREHENSIVE_JUDGEMENT','综合判断','CIVIL_SERVICE',3,14,1.00);
-- 以下权重是问题优先级的正式初始配置：importance 表示考试重要度，
-- improvement_potential 表示短期提升收益，transfer_value 表示对其他题型的迁移价值。
UPDATE knowledge_point SET improvement_potential=0.90,transfer_value=0.95 WHERE code='AVG_GROWTH_RATE';
SET @judgement=(SELECT id FROM knowledge_point WHERE code='JUDGEMENT');
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value) VALUES
(@judgement,'LOGICAL_JUDGEMENT','逻辑判断','CIVIL_SERVICE',3,1,0.95,0.75,0.85),(@judgement,'DEFINITION_JUDGEMENT','定义判断','CIVIL_SERVICE',3,2,0.80,0.70,0.60);
SET @quantity=(SELECT id FROM knowledge_point WHERE code='QUANTITY');
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value) VALUES
(@quantity,'PERMUTATION_COMBINATION','排列组合','CIVIL_SERVICE',3,1,0.65,0.35,0.30);

-- 申论知识点树：能力维度 + 作文预留 + 申论主题（主题与政治阅读专题一一对应）。
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES (NULL,'SHENLUN','申论','CIVIL_SERVICE',1,2,1.00);
SET @shenlun=LAST_INSERT_ID();
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value) VALUES
(@shenlun,'ESSAY_MATERIAL_READING','材料阅读','CIVIL_SERVICE',2,1,0.90,0.80,0.75),
(@shenlun,'ESSAY_INFO_EXTRACTION','信息提取','CIVIL_SERVICE',2,2,0.90,0.80,0.75),
(@shenlun,'ESSAY_POINT_COMPLETENESS','要点完整性','CIVIL_SERVICE',2,3,0.95,0.85,0.80),
(@shenlun,'ESSAY_SUMMARY','归纳概括','CIVIL_SERVICE',2,4,1.00,0.85,0.75),
(@shenlun,'ESSAY_ANALYSIS','综合分析','CIVIL_SERVICE',2,5,0.95,0.80,0.80),
(@shenlun,'ESSAY_COUNTERMEASURE','提出对策','CIVIL_SERVICE',2,6,0.95,0.85,0.80),
(@shenlun,'ESSAY_IMPLEMENTATION','贯彻执行','CIVIL_SERVICE',2,7,1.00,0.85,0.75),
(@shenlun,'ESSAY_EXPRESSION','文字表达','CIVIL_SERVICE',2,8,0.85,0.70,0.85);
-- 以下 ESSAY_W_ 前缀知识点为后续大作文阶段预留维度，本阶段不参与评分与问题识别。
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,status,importance,improvement_potential,transfer_value) VALUES
(@shenlun,'ESSAY_W_THEME','立意（作文预留）','CIVIL_SERVICE',2,9,'RESERVED',0.90,0.50,0.60),
(@shenlun,'ESSAY_W_STRUCTURE','结构（作文预留）','CIVIL_SERVICE',2,10,'RESERVED',0.85,0.50,0.60),
(@shenlun,'ESSAY_W_ARGUMENTATION','论证（作文预留）','CIVIL_SERVICE',2,11,'RESERVED',0.90,0.50,0.60),
(@shenlun,'ESSAY_W_MATERIAL_USE','材料运用（作文预留）','CIVIL_SERVICE',2,12,'RESERVED',0.80,0.50,0.60),
(@shenlun,'ESSAY_W_LOGIC','逻辑（作文预留）','CIVIL_SERVICE',2,13,'RESERVED',0.85,0.50,0.60),
(@shenlun,'ESSAY_W_LANGUAGE','语言（作文预留）','CIVIL_SERVICE',2,14,'RESERVED',0.85,0.50,0.60),
(@shenlun,'ESSAY_W_TITLE','标题（作文预留）','CIVIL_SERVICE',2,15,'RESERVED',0.70,0.40,0.50),
(@shenlun,'ESSAY_W_OPENING_CLOSING','开头结尾（作文预留）','CIVIL_SERVICE',2,16,'RESERVED',0.75,0.40,0.50);
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES (@shenlun,'ESSAY_THEMES','申论主题','CIVIL_SERVICE',2,17,0.95);
SET @themes=(SELECT id FROM knowledge_point WHERE code='ESSAY_THEMES');
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES
(@themes,'HQ_DEVELOPMENT','高质量发展','CIVIL_SERVICE',3,1,0.90),
(@themes,'TECH_INNOVATION','科技创新','CIVIL_SERVICE',3,2,0.95),
(@themes,'NEW_QUALITY_PRODUCTIVITY','新质生产力','CIVIL_SERVICE',3,3,0.95),
(@themes,'RURAL_REVITALIZATION','乡村振兴','CIVIL_SERVICE',3,4,0.90),
(@themes,'GRASSROOTS_GOVERNANCE','基层治理','CIVIL_SERVICE',3,5,0.90),
(@themes,'PEOPLE_LIVELIHOOD','民生','CIVIL_SERVICE',3,6,0.95),
(@themes,'ECO_CIVILIZATION','生态文明','CIVIL_SERVICE',3,7,0.85),
(@themes,'CULTURE','文化','CIVIL_SERVICE',3,8,0.80),
(@themes,'GOVERNANCE','政府治理','CIVIL_SERVICE',3,9,0.85),
(@themes,'TALENT','人才','CIVIL_SERVICE',3,10,0.85);

-- 政治阅读初始专题，knowledge_point_id 关联同 code 的主题知识点，用于申论-阅读联动。
INSERT INTO political_topic(code,name,knowledge_point_id,description,sort_no)
SELECT k.code,k.name,k.id,'政治阅读与申论联动主题',k.sort_no FROM knowledge_point k WHERE k.parent_id=(SELECT id FROM knowledge_point WHERE code='ESSAY_THEMES');
