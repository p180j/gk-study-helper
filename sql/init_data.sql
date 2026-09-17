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

-- 第九阶段：新用户摸底题。正式初始内容，用于全新用户在无历史能力数据时建立能力基线。
INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','下列成语使用恰当的是：','A','“循序渐进”表示按照一定步骤逐渐深入或提高，使用恰当。',45,60,'MANUAL','新用户摸底题','VALIDATION','ACTIVE',1,'9010000000000000000000000000000000000000000000000000000000000001');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','学习应当循序渐进',1),(@q,'B','他做事总是首当其冲地逃避',2),(@q,'C','这件小事令人叹为观止地担忧',3),(@q,'D','大家对错误充耳不闻地观看',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='VERBAL';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','所有参加培训的人都通过了测试，小李参加了培训。可以推出：','B','根据充分条件关系，小李参加培训，因此通过测试。',45,70,'MANUAL','新用户摸底题','VALIDATION','ACTIVE',1,'9010000000000000000000000000000000000000000000000000000000000002');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','小李没有通过测试',1),(@q,'B','小李通过了测试',2),(@q,'C','通过测试的人都参加了培训',3),(@q,'D','无法判断',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='JUDGEMENT';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','某商品原价100元，先涨价20%，再降价20%，现价是多少元？','B','100×1.2×0.8=96元。',50,70,'MANUAL','新用户摸底题','VALIDATION','ACTIVE',1,'9010000000000000000000000000000000000000000000000000000000000003');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','80',1),(@q,'B','96',2),(@q,'C','100',3),(@q,'D','120',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='QUANTITY';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','某地去年产值为200亿元，今年增长10%，今年产值为多少亿元？','C','200×(1+10%)=220亿元。',40,60,'MANUAL','新用户摸底题','VALIDATION','ACTIVE',1,'9010000000000000000000000000000000000000000000000000000000000004');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','200',1),(@q,'B','210',2),(@q,'C','220',3),(@q,'D','240',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='DATA_ANALYSIS';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','我国的根本政治制度是：','D','人民代表大会制度是我国的根本政治制度。',35,45,'MANUAL','新用户摸底题','VALIDATION','ACTIVE',1,'9010000000000000000000000000000000000000000000000000000000000005');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','基层群众自治制度',1),(@q,'B','民族区域自治制度',2),(@q,'C','多党合作和政治协商制度',3),(@q,'D','人民代表大会制度',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='COMMON_SENSE';
