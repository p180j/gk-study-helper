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

-- ============================================================
-- 第十阶段：初始模考内容。
-- 10 道模考专用题（usage_type=MOCK_RESERVED，不进入日常训练、摸底与验证选题），
-- 外加行测、申论各一套已启用试卷，空库初始化后即可进行完整计时模考。
-- ============================================================

-- 言语理解 2 题
INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','依次填入下列句子横线处的词语，最恰当的一项是：①文化遗产保护不能一味____，要在传承中创新。②面对不实指责，他用确凿事实____了对方的观点。','B','①“复制”与后文“在传承中创新”形成对照；②“反驳”与“用事实”搭配恰当。',50,70,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000001');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','封存／回应',1),(@q,'B','复制／反驳',2),(@q,'C','守旧／反映',3),(@q,'D','保存／批判',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='VERBAL';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','数字经济正在重塑生产生活方式，但其快速发展也带来数据安全、平台垄断等新挑战。这段文字意在说明：','B','文段先肯定积极作用，再指出新挑战，重点落在“新挑战”上，意在说明数字经济需要规范引导。',50,80,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000002');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','数字经济利大于弊',1),(@q,'B','数字经济发展需要规范引导',2),(@q,'C','数据安全是数字经济的核心',3),(@q,'D','平台垄断问题无法避免',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='VERBAL';

-- 判断推理 2 题
INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','某单位选派人员参加培训：如果小张参加，那么小李也参加。已知小李没有参加。由此可以推出：','B','充分条件假言推理的否定后件式：由“小李没参加”可推出“小张没有参加”。',55,75,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000003');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','小张参加了',1),(@q,'B','小张没有参加',2),(@q,'C','小张和小李都参加了',3),(@q,'D','无法判断小张是否参加',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='JUDGEMENT';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','“回声效应”指信息在相对封闭的传播圈层内反复强化，使圈层内观点日趋极化的现象。根据定义，下列属于回声效应的是：','B','B 项同一社群内观点相同的文章被反复转发强化，观点越来越极端，符合定义；其余选项均体现多元信息流动。',55,75,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000004');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','平台向用户推荐与其观点不同的内容',1),(@q,'B','某兴趣社群成员反复转发观点相同的文章，观点越来越极端',2),(@q,'C','记者综合多位不同立场专家的意见形成报道',3),(@q,'D','图书馆采购了不同立场作者的书籍',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='JUDGEMENT';

-- 数量关系 2 题
INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','一项工程甲单独做 12 天完成，乙单独做 18 天完成。两人合作需要多少天完成？','A','1÷(1/12+1/18)=1÷(5/36)=7.2 天。',60,90,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000005');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','7.2',1),(@q,'B','7.5',2),(@q,'C','8',3),(@q,'D','6.5',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='QUANTITY';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','某商品按成本价加价 50% 定价，后打 8 折出售，仍获利 24 元。该商品成本是多少元？','A','售价=成本×1.5×0.8=成本×1.2，获利 20% 成本=24 元，成本=120 元。',60,90,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000006');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','120',1),(@q,'B','140',2),(@q,'C','150',3),(@q,'D','160',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='QUANTITY';

-- 资料分析 2 题
INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','2023 年某省高新技术产业产值为 4800 亿元，同比增长 20%。2022 年该省高新技术产业产值约为多少亿元？','B','基期量=4800÷1.2=4000 亿元。',50,70,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000007');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','3800',1),(@q,'B','4000',2),(@q,'C','4200',3),(@q,'D','4400',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='DATA_ANALYSIS';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','某市 2023 年 GDP 为 6000 亿元，其中第三产业增加值为 3600 亿元。第三产业增加值占 GDP 的比重为：','C','3600÷6000=60%。',45,60,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000008');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','50%',1),(@q,'B','55%',2),(@q,'C','60%',3),(@q,'D','65%',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='DATA_ANALYSIS';

-- 常识判断 2 题
INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','根据我国宪法，公民的下列基本权利中，属于政治权利的是：','C','选举权和被选举权属于政治权利；人身自由、受教育权、物质帮助权均不属于政治权利范畴。',40,40,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000009');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','人身自由',1),(@q,'B','受教育权',2),(@q,'C','选举权和被选举权',3),(@q,'D','物质帮助权',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='COMMON_SENSE';

INSERT INTO question(question_type,stem,answer,analysis,difficulty_expected,standard_time_seconds,source_type,source_name,usage_type,status,version,content_hash)
VALUES('SINGLE','下列关于我国科技成就的说法，正确的是：','C','“奋斗者”号创造了我国载人深潜新纪录；“天问一号”任务实现火星着陆而非月球背面，“嫦娥”系列实现月球采样返回，“北斗”面向全球提供服务。',45,40,'MANUAL','初始模考卷','MOCK_RESERVED','ACTIVE',1,'9020000000000000000000000000000000000000000000000000000000000010');
SET @q=LAST_INSERT_ID();
INSERT INTO question_option(question_id,option_key,option_text,sort_no) VALUES(@q,'A','“嫦娥”系列任务实现了火星采样返回',1),(@q,'B','“天问一号”实现了月球背面软着陆',2),(@q,'C','“奋斗者”号创造了我国载人深潜新纪录',3),(@q,'D','“北斗”系统只能用于国内导航',4);
INSERT INTO question_knowledge(question_id,knowledge_point_id,relation_type,weight) SELECT @q,id,'PRIMARY',1.00 FROM knowledge_point WHERE code='COMMON_SENSE';

-- 行测全真模拟试卷（第一套）：5 模块 × 2 题，120 分钟 100 分，初始即启用。
INSERT INTO mock_paper(name,exam_type,source_year,source,usage_type,duration_minutes,total_score,status)
VALUES('行测全真模拟（第一套）','XINGCE',2025,'初始内置','MOCK_RESERVED',120,100.00,'ACTIVE');
SET @mp=LAST_INSERT_ID();
SET @mq1=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000001');
SET @mq2=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000002');
SET @mq3=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000003');
SET @mq4=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000004');
SET @mq5=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000005');
SET @mq6=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000006');
SET @mq7=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000007');
SET @mq8=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000008');
SET @mq9=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000009');
SET @mq10=(SELECT id FROM question WHERE content_hash='9020000000000000000000000000000000000000000000000000000000000010');
INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) SELECT @mp,'言语理解','VERBAL',id,1,20.00 FROM knowledge_point WHERE code='VERBAL';
SET @msec=LAST_INSERT_ID();
INSERT INTO mock_paper_item(paper_id,section_id,item_type,question_id,sort_no,score) VALUES(@mp,@msec,'QUESTION',@mq1,1,10.00),(@mp,@msec,'QUESTION',@mq2,2,10.00);
INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) SELECT @mp,'判断推理','JUDGEMENT',id,2,20.00 FROM knowledge_point WHERE code='JUDGEMENT';
SET @msec=LAST_INSERT_ID();
INSERT INTO mock_paper_item(paper_id,section_id,item_type,question_id,sort_no,score) VALUES(@mp,@msec,'QUESTION',@mq3,3,10.00),(@mp,@msec,'QUESTION',@mq4,4,10.00);
INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) SELECT @mp,'数量关系','QUANTITY',id,3,20.00 FROM knowledge_point WHERE code='QUANTITY';
SET @msec=LAST_INSERT_ID();
INSERT INTO mock_paper_item(paper_id,section_id,item_type,question_id,sort_no,score) VALUES(@mp,@msec,'QUESTION',@mq5,5,10.00),(@mp,@msec,'QUESTION',@mq6,6,10.00);
INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) SELECT @mp,'资料分析','DATA_ANALYSIS',id,4,20.00 FROM knowledge_point WHERE code='DATA_ANALYSIS';
SET @msec=LAST_INSERT_ID();
INSERT INTO mock_paper_item(paper_id,section_id,item_type,question_id,sort_no,score) VALUES(@mp,@msec,'QUESTION',@mq7,7,10.00),(@mp,@msec,'QUESTION',@mq8,8,10.00);
INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) SELECT @mp,'常识判断','COMMON_SENSE',id,5,20.00 FROM knowledge_point WHERE code='COMMON_SENSE';
SET @msec=LAST_INSERT_ID();
INSERT INTO mock_paper_item(paper_id,section_id,item_type,question_id,sort_no,score) VALUES(@mp,@msec,'QUESTION',@mq9,9,10.00),(@mp,@msec,'QUESTION',@mq10,10,10.00);

-- 申论全真模拟试卷（第一套）：材料 + 归纳概括 + 综合分析，共用总计时 90 分钟 40 分。
INSERT INTO essay_question(topic_knowledge_point_id,question_type,material,prompt,word_limit_min,word_limit_max,standard_time_seconds,reference_answer,reference_points,source_type,source_year,source_name,status,version)
SELECT id,'SUMMARY','N 市 Y 区近年来推行社区网格化治理，将全区 87 个社区划分为 620 个网格，每个网格配备 1 名专职网格员和若干兼职志愿者，负责信息采集、隐患排查和民生服务。该区建立“社区吹哨、部门报到”机制，网格员发现的问题可直报区级平台，相关部门须在 24 小时内响应。2024 年，该区通过网格体系化解矛盾纠纷 4300 余起，物业投诉量同比下降 38%。同时，该区培育社区社会组织 210 家，引导居民通过议事会协商解决加装电梯、停车位改造等实际问题 170 余件；部分社区还引入智慧平台，居民扫码即可上报问题并查看办理进度。不过，也有网格员反映考核指标偏多、部门响应时快时慢，个别老旧小区居民参与度仍然不高。','根据给定资料，概括 N 市 Y 区推行社区网格化治理的主要做法。要求：概括准确、条理清楚，不超过 300 字。',100,300,1200,'一是划分治理网格，配备专职网格员和兼职志愿者，承担信息采集、隐患排查和民生服务；二是建立“社区吹哨、部门报到”机制，问题直报区级平台并要求部门限时响应；三是培育社区社会组织，引导居民通过议事会协商解决身边问题；四是引入智慧平台，畅通居民报事与进度查询渠道。','["划分网格并配备专兼职治理力量","建立社区吹哨、部门报到限时响应机制","培育社区社会组织，引导居民协商自治","引入智慧平台畅通居民报事渠道"]','MANUAL',2025,'初始申论模考卷','ACTIVE',1 FROM knowledge_point WHERE code='GRASSROOTS_GOVERNANCE';
SET @eq1=LAST_INSERT_ID();
INSERT INTO essay_question(topic_knowledge_point_id,question_type,material,prompt,word_limit_min,word_limit_max,standard_time_seconds,reference_answer,reference_points,source_type,source_year,source_name,status,version)
SELECT id,'ANALYSIS','N 市 Y 区近年来推行社区网格化治理，将全区 87 个社区划分为 620 个网格，每个网格配备 1 名专职网格员和若干兼职志愿者，负责信息采集、隐患排查和民生服务。该区建立“社区吹哨、部门报到”机制，网格员发现的问题可直报区级平台，相关部门须在 24 小时内响应。2024 年，该区通过网格体系化解矛盾纠纷 4300 余起，物业投诉量同比下降 38%。同时，该区培育社区社会组织，引导居民通过议事会协商解决加装电梯、停车位改造等实际问题 170 余件；部分社区还引入智慧平台，居民扫码即可上报问题并查看办理进度。不过，也有网格员反映考核指标偏多、部门响应时快时慢，个别老旧小区居民参与度仍然不高。','结合给定资料，谈谈你对“基层治理既要有力度，也要有温度”这句话的理解。要求：观点明确、分析合理、条理清晰，不超过 500 字。',200,500,1800,'力度指基层治理的制度化、机制化与响应效率，如网格划分、限时响应机制；温度指治理中对人的关怀与参与，如培育社会组织、居民协商议事。二者相辅相成：力度保证秩序与效率，温度激发认同与参与；应通过优化考核、均衡部门响应、提升老旧小区参与度，实现力度与温度并重。','["力度体现在网格划分、限时响应等制度机制与执行效率","温度体现在社会组织培育与居民协商参与等人文关怀","力度与温度相辅相成，需通过考核优化和参与引导实现并重"]','MANUAL',2025,'初始申论模考卷','ACTIVE',1 FROM knowledge_point WHERE code='GRASSROOTS_GOVERNANCE';
SET @eq2=LAST_INSERT_ID();
INSERT INTO mock_paper(name,exam_type,source_year,source,usage_type,duration_minutes,total_score,status)
VALUES('申论全真模拟（第一套）','SHENLUN',2025,'初始内置','MOCK_RESERVED',90,40.00,'ACTIVE');
SET @sp=LAST_INSERT_ID();
INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) SELECT @sp,'第一题·归纳概括','ESSAY_SUMMARY',id,1,15.00 FROM knowledge_point WHERE code='ESSAY_SUMMARY';
SET @ssec=LAST_INSERT_ID();
INSERT INTO mock_paper_item(paper_id,section_id,item_type,essay_question_id,sort_no,score) VALUES(@sp,@ssec,'ESSAY',@eq1,1,15.00);
INSERT INTO mock_paper_section(paper_id,name,section_code,knowledge_point_id,sort_no,score) SELECT @sp,'第二题·综合分析','ESSAY_ANALYSIS',id,2,25.00 FROM knowledge_point WHERE code='ESSAY_ANALYSIS';
SET @ssec=LAST_INSERT_ID();
INSERT INTO mock_paper_item(paper_id,section_id,item_type,essay_question_id,sort_no,score) VALUES(@sp,@ssec,'ESSAY',@eq2,2,25.00);
