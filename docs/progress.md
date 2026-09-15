# 开发进度

## 第一阶段：题目—作答—能力—问题—计划闭环

- [x] 建立 `backend`、`miniapp`、`admin-web`、`sql`、`docs` 基础目录
- [x] 建立 Spring Boot、MyBatis、MySQL 后端工程地基
- [x] 建立 `schema.sql`、`init_data.sql`、`upgrade.sql` SQL 基线
- [x] 通过全新空库初始化验收（12 张表、20 个知识点）
- [x] 通过连接全新测试库的 Spring Boot 启动验收
- [x] 完成 CSV 题目导入及单行失败隔离
- [x] 完成每日计划生成
- [x] 完成题目查询、训练答题和不可变 AnswerRecord 快照保存
- [x] 完成能力画像及能力历史更新
- [x] 完成学习问题识别、验证与解决
- [x] 完成下一次计划训练优先级调整
- [x] 跑通第一阶段核心学习闭环

## 第二阶段：题库与答题链路

- [x] 校验题库、选项、知识点和媒体关联结构
- [x] 支持 SINGLE 题型及题目状态、来源、用途、版本、难度、参考耗时和来源信息
- [x] 支持 CSV 批量导入、基础重复校验及逐行失败隔离
- [x] 完成题目列表、题目详情、提交答案、答题历史和题目导入接口
- [x] 保存题目版本、正确答案、难度、知识点、耗时、训练场景、信心和错因快照
- [x] 验证题目修改后历史 AnswerRecord 快照保持不变
- [x] 自动测试 6 项全部通过
- [x] 从空库执行 schema.sql、init_data.sql 并启动应用

## 第三阶段：能力引擎

- [x] 集中实现可解释的 mastery、speed、stability、confidence 算法
- [x] 按 practiceType、confidenceType、题目难度、耗时、样本成熟度计算能力变化
- [x] 支持 PRIMARY / SECONDARY 知识点权重差异
- [x] 答题事务内更新 AbilityProfile 并追加 AbilityHistory
- [x] 提交答案接口返回精简能力变化
- [x] 支持按用户回放全部 AnswerRecord 并重建 AbilityProfile
- [x] 回放仅重建 AbilityProfile，不重复追加 AbilityHistory
- [x] 验证 6 次正常答题产生 6 条 AbilityHistory，单次及重复回放后仍为 6 条
- [x] 验证正常计算与重复回放的 AbilityProfile 完全一致且不修改 AnswerRecord
- [x] 自动测试 19 项全部通过
- [x] 验证数据库增量并合并 schema.sql，upgrade.sql 已清理
- [x] 从空库执行 schema.sql、init_data.sql 并启动应用

## 第四阶段：LearningProblem 学习问题

- [x] 集中实现 LearningProblemEngine，规则使用 AbilityProfile 与近期 AnswerRecord
- [x] 识别 MASTERY、SPEED、STABILITY 三类学习问题
- [x] 实现 OBSERVING、CONFIRMED、PROCESSING、VERIFYING、RESOLVED、REOPENED 生命周期
- [x] 同一用户、知识点、问题类型复用同一 LearningProblem，并保存状态变化历史
- [x] 保存样本、近期错误、超时、波动、四项能力、验证次数等可解释证据
- [x] 答题事务内自动创建或更新 LearningProblem，能力回放不重复制造问题
- [x] 提供 GET /api/learning-problems 查询接口
- [x] 自动测试 30 项全部通过
- [x] 真实验证“年均增长率”问题完整生命周期及去重
- [x] 从独立空库仅执行 schema.sql、init_data.sql，并完成 Spring Boot 与 MyBatis 启动验收

## 第五阶段：问题优先级与 DailyPlan

- [x] 集中实现 ProblemPriorityEngine，统一管理严重度、考试重要度、持续性、提升收益、可信度、迁移价值和问题状态权重
- [x] 排除 RESOLVED，保护低置信度 OBSERVING，优先为 VERIFYING 安排验证任务，核心问题最多 3 个
- [x] 验证“最弱不一定最优先”，高收益年均增长率优先于更弱的排列组合
- [x] 集中实现 DailyPlanEngine，支持 20 / 45 / 60 / 90 分钟计划
- [x] 计划明细支持 QUESTION_SET / REVIEW，并保存问题、知识点、目的、分钟数、顺序和生成原因
- [x] GET /api/plan/today 保持当日计划幂等，POST /api/plan/generate 支持显式重建
- [x] 提供 GET /api/learning-problems/core，返回最多 3 个当前核心问题
- [x] 验证问题解决后训练优先级降低、候补问题补位，答题后不重建当天计划
- [x] 自动测试 41 项全部通过
- [x] 从独立空库仅执行 schema.sql、init_data.sql，并完成 Spring Boot、数据库与 MyBatis 启动验收
