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

## 第六阶段：小程序与管理控台第一批页面

- [x] 确认 importance、improvement_potential、transfer_value 为正式问题优先级初始配置，并补充用途注释
- [x] 清理本地第四、第五及本阶段临时验收数据
- [x] 原生小程序实现首页、学习、能力三个底部 Tab
- [x] 小程序统一真实 API 请求、用户上下文、loading / empty / error 和重复提交保护
- [x] 学习页完成计划取题、计时、信心/错因、正确/错误提交、解析和能力变化展示
- [x] 管理控台实现题库列表/筛选/详情、CSV 导入、知识点和答题记录
- [x] 补充管理端题目详情、知识点层级、计划任务安全题目列表三个最小接口
- [x] 验证学习端题目提交前不返回答案/解析，管理端详情可查看答案
- [x] 管理控台在浏览器中连接真实后端完成页面和失败状态验收
- [x] 小程序页面逻辑连接真实后端完成首页、正确/错误答题、重复提交保护、能力页和失败状态验收
- [x] 集中实现规则型 ErrorDiagnosis，保存候选错因、证据、置信度、用户确认结果和诊断状态
- [x] 同一用户、知识点、候选错因复用同一诊断记录，重复证据提高置信度且不修改 AnswerRecord
- [x] 答错结果支持轻量确认/否认，LearningProblem 和能力页可展示当前主要根因
- [x] 管理控台导入题目后，小程序实际作答，管理控台看到对应 AnswerRecord
- [x] 后端自动测试 55 项全部通过，前端脚本和 JSON 配置语法检查通过
- [x] 独立空库仅执行 schema.sql、init_data.sql 后得到 14 张表、23 个知识点，并完成 Spring Boot、数据库和 MyBatis 启动验收
- [x] 在微信开发者工具中完成小程序编译和模拟器真实运行验收，覆盖三 Tab、正确/错误答题、错因确认、loading / empty / error 和防重复提交

## 第七阶段：申论训练 + 政治阅读

- [x] 新增申论数据模型：essay_question（题目/材料/参考要点）、essay_answer（原始作答只增不改）、essay_evaluation（结构化评分）
- [x] 申论能力维度与申论主题作为知识点挂载 SHENLUN 根下，复用 ability_profile / learning_problem / daily_plan_item
- [x] 8 个申论能力维度 + 8 个作文预留维度 + 10 个申论主题知识点初始数据
- [x] 可替换评分接口 EssayGrader + 明确标记的本地规则实现 LocalRuleEssayGrader（LOCAL_RULE_V1）
- [x] 评分结果结构化持久化：totalScore、dimensionScores、strengths、problems、missingPoints、evidence、suggestions
- [x] 小程序申论训练流程：任务列表 → 材料/题目 → 作答 → 提交 → 原始保存 → 评分 → 能力变化展示
- [x] LearningProblem 扩展 ESSAY_MISSING_POINTS / ESSAY_ANALYSIS / ESSAY_EXPRESSION / ESSAY_STRUCTURE / CONTENT_GAP，复用统一生命周期
- [x] 政治阅读数据模型：political_topic（10 个初始专题）+ reading_material（结构化字段）+ reading_record
- [x] 小程序政治阅读：专题列表、材料结构化查看（核心观点/问题/原因/对策/规范表达/案例/适用主题）、已读/收藏/掌握标记
- [x] 申论-阅读联动：主题申论弱且阅读覆盖不足产生 CONTENT_GAP，计划同时安排主题 ESSAY + READING，完成阅读后缺口 RESOLVED
- [x] DailyPlan 正式支持 QUESTION_SET / REVIEW / ESSAY / READING，按问题动态组合分钟数，当天计划保持稳定
- [x] 能力页按行测/申论分组展示，展示申论能力与 LearningProblem
- [x] 管理控台：申论题目/作答/评分查看、政治专题/材料管理与状态管理
- [x] 真实验收场景 A：申论作答 → 原始保存 → 结构化评分 96.2 → 6 项能力更新 → 问题 CONFIRMED→VERIFYING → 计划安排申论专项
- [x] 真实验收场景 B：后台新增基层治理材料 → 小程序阅读标记完成 → 阅读记录保存 → 专题进度更新
- [x] 真实验收场景 C：同一 45 分钟计划同时出现基层治理 ESSAY（27 分钟）+ READING（18 分钟）
- [x] 验证原始作答不可被评分覆盖（3 条历史作答全部原样保留）
- [x] 后端自动测试 123 项全部通过（含既有 55 项回归）
- [x] 从独立空库仅执行 schema.sql、init_data.sql（20 张表、51 个知识点、10 个专题）并完成 Spring Boot 启动验收

## 第八阶段：真实 AI 能力接入

- [x] 在 Java 11 基线上实现统一 OpenAI 兼容 AiProvider，集中管理超时、重试、结构化响应与受控异常
- [x] 实现 REAL_AI 申论评分并保存完整调用审计；失败时保留原始作答、记录失败并支持重试
- [x] 实现重复错因触发的 AI 根因分析，规则诊断仍为事实来源
- [x] 实现政治阅读材料 AI 结构化，成功后保持 DRAFT 待人工确认，失败不覆盖原文
- [x] 实现只读 AI Coach，依据真实能力、问题、错因、评分、计划和近期答题解释现状
- [x] 小程序与管理控台补充 AI 展示、失败提示、重试和人工确认入口
- [x] 完成 TEST_STUB 协议与降级测试；Java 11 下后端自动测试 134 项通过
- [x] 主库升级并同步 schema.sql；独立空库仅执行 schema.sql、init_data.sql 后完成启动验证
- [x] 使用 DeepSeek `deepseek-flash` 完成申论评分、错因深挖、政治阅读结构化、AI Coach 四条真实外部调用验收
- [x] 验证 REAL_AI 审计、置信度百分制兼容、证据约束、失败降级和核心数据保护
- [x] 第八阶段真实 AI 能力验收通过（Key 仅用于运行环境，未写入仓库）
