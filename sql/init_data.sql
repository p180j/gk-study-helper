INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES (NULL,'XINGCE','行测','CIVIL_SERVICE',1,1,1.00);
SET @root=LAST_INSERT_ID();
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES
(@root,'VERBAL','言语理解','CIVIL_SERVICE',2,1,0.90),(@root,'JUDGEMENT','判断推理','CIVIL_SERVICE',2,2,0.90),(@root,'QUANTITY','数量关系','CIVIL_SERVICE',2,3,0.80),(@root,'DATA_ANALYSIS','资料分析','CIVIL_SERVICE',2,4,1.00),(@root,'COMMON_SENSE','常识判断','CIVIL_SERVICE',2,5,0.60);
SET @data=(SELECT id FROM knowledge_point WHERE code='DATA_ANALYSIS');
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance) VALUES
(@data,'DATA_LOCATION','数据定位','CIVIL_SERVICE',3,1,0.85),(@data,'GROWTH_RATE','增长率','CIVIL_SERVICE',3,2,1.00),(@data,'GROWTH_AMOUNT','增长量','CIVIL_SERVICE',3,3,0.95),(@data,'BASE_AMOUNT','基期量','CIVIL_SERVICE',3,4,1.00),(@data,'CURRENT_AMOUNT','现期量','CIVIL_SERVICE',3,5,0.90),(@data,'AVG_GROWTH_RATE','年均增长率','CIVIL_SERVICE',3,6,1.00),(@data,'CONTRIBUTION_RATE','贡献率','CIVIL_SERVICE',3,7,0.85),(@data,'PULL_GROWTH_RATE','拉动增长率','CIVIL_SERVICE',3,8,0.85),(@data,'CURRENT_PROPORTION','现期比重','CIVIL_SERVICE',3,9,0.95),(@data,'BASE_PROPORTION','基期比重','CIVIL_SERVICE',3,10,0.95),(@data,'PROPORTION_CHANGE','比重变化','CIVIL_SERVICE',3,11,0.95),(@data,'AVERAGE','平均数','CIVIL_SERVICE',3,12,0.90),(@data,'MULTIPLE','倍数','CIVIL_SERVICE',3,13,0.85),(@data,'COMPREHENSIVE_JUDGEMENT','综合判断','CIVIL_SERVICE',3,14,1.00);
UPDATE knowledge_point SET improvement_potential=0.90,transfer_value=0.95 WHERE code='AVG_GROWTH_RATE';
SET @judgement=(SELECT id FROM knowledge_point WHERE code='JUDGEMENT');
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value) VALUES
(@judgement,'LOGICAL_JUDGEMENT','逻辑判断','CIVIL_SERVICE',3,1,0.95,0.75,0.85),(@judgement,'DEFINITION_JUDGEMENT','定义判断','CIVIL_SERVICE',3,2,0.80,0.70,0.60);
SET @quantity=(SELECT id FROM knowledge_point WHERE code='QUANTITY');
INSERT INTO knowledge_point(parent_id,code,name,exam_type,level,sort_no,importance,improvement_potential,transfer_value) VALUES
(@quantity,'PERMUTATION_COMBINATION','排列组合','CIVIL_SERVICE',3,1,0.65,0.35,0.30);
