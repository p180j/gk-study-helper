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

## 第九阶段第一检查点：新用户初始化、摸底计划与小程序核心体验

- [x] 新用户无需预写 ability_profile 即返回 13 项完整公务员核心能力地图，全部显示“未测评”
- [x] 未测评能力的 mastery / speed / stability / confidence / sampleCount 均为 0，且不会产生 LearningProblem
- [x] 已有用户能力数据保持不变，提供已评估能力数、总能力数和覆盖率
- [x] 新用户自动生成 ASSESSMENT 摸底计划，并根据考试重要度、未测评程度、可信度、题库可用性和计划时长选题
- [x] DailyPlan purpose 支持 ASSESSMENT / TRAINING / VALIDATION / MAINTENANCE，已有有效当天计划继续保持幂等
- [x] 修复历史空计划阻止新用户生成摸底任务的问题，仅对零任务无效计划自动重建
- [x] 新增 5 道正式摸底题，覆盖言语、判断、数量、资料和常识，空库初始化后即可学习
- [x] 按 UI 参考图重构首页、学习、能力、做题结果、申论批改、政治阅读和 AI Coach
- [x] 用户可见的任务目的、能力代码和问题状态统一显示中文，不再暴露内部英文枚举
- [x] 微信开发者工具真实运行并截图验证全新用户首页、已有数据首页、学习页、能力页、错题真实 AI、申论真实 AI、政治阅读和 AI Coach
- [x] Java 11 下后端自动测试 139 项全部通过，前端脚本语法和模拟器控制台错误检查通过
- [x] 独立空库仅执行 schema.sql、init_data.sql，验证 20 张表、51 个知识点、5 道题和 20 个选项

## 第九阶段第一检查点：小程序视觉精修（洞察与层级）

- [x] display.js 补全任务目的、问题状态、能力状态、评测者、能力代码全套中文映射
- [x] AI Coach 页改为三层结构：结论 / 关键证据 / 下一步建议，完整 AI 分析默认折叠可展开
- [x] 申论 AI 批改首屏精修：综合评分突出 + 能力评分 + 各区块最多 3 条，其余经“查看完整批改”展开
- [x] 能力页视觉层级：趋势 ↑/↓ 着色、薄弱项柔和橙提示、改善项绿色、未测评灰色
- [x] 后端新增 GET /api/ai/coach/insight：按“已确认错因根因 → 学习问题 → 最弱能力 → 基线”优先级取真实数据，不经过 AI 生成
- [x] 后端能力趋势：AbilityProfile 增加 masteryTrend 非表字段（对比 ability_history 上一快照聚合），overview 接口返回
- [x] 首页 AI 今日洞察改为“一行具体问题 + 一行简短建议”，洞察接口失败静默回退基线提示
- [x] Java 11 下后端自动测试 178 项全部通过；修改的 5 个前端脚本 node --check 通过；wxml 扫描无英文枚举直接展示
- [ ] 微信开发者工具截图与参考图比对验收（NOT VERIFIED：本次会话无法运行微信开发者工具，待补）

## 第九阶段后半部分：多 AI Provider + 内容自动化

### 多 AI Provider

- [x] AiProvider 接口扩展 providerCode / currentModel，五家 Provider 正式支持：DeepSeek、Gemini、GLM / 智谱、GPT / OpenAI、Qwen / 通义千问
- [x] OpenAI 兼容协议统一 OpenAiCompatibleClient，Gemini 独立协议实现；厂商判断只允许出现在 ProviderRegistry，业务模块零厂商分支
- [x] ProviderRegistry 按数据库默认配置动态构建客户端，配置变更自动重建；未配置时回退环境变量（兼容第八阶段部署）
- [x] AiProviderConfigMapper / AdminAiProviderService / AdminAiProviderController：配置保存、启用停用、默认唯一、测试连接（保存前可用表单值直接测试）
- [x] API Key AES-256-GCM 加密存储（AI_CONFIG_MASTER_KEY），任何接口只返回 maskedKey（如 sk-****abcd），掩码值复用被拒绝，修改必须重输完整 Key
- [x] 测试连接真实区分七种状态（成功 / Key 无效 / 模型不存在 / 超时 / 限流 / 额度不足 / 网络或 Provider 异常），全部中文展示并记录耗时
- [x] 每家提供默认模型列表 + 自定义模型名 + Base URL 高级选项，模型更新无需发版
- [x] 管理控台 AI 设置页：五家配置状态 / 启用状态 / 当前模型 / 最后测试结果与时间、Key 输入、测试连接、保存、设为默认
- [x] 真实验证测试连接链路：DeepSeek / GLM / Qwen 无效 Key 均真实请求远端并正确返回“API Key 无效”（185–264ms）；Gemini / OpenAI 在当前网络环境正确返回“请求超时”
- [ ] 两家 Provider 真实 AI 调用成功切换验收（NOT VERIFIED：本次会话无真实 API Key；链路与状态机已验证，待配置 Key 后在管理控台完成）

### 内容自动化 / 爬虫

- [x] content_source 来源配置表：name / baseUrl / sourceType / examType / trustLevel / enabled / crawlStrategy / lastCrawlTime / status，配置进数据库不加 YAML
- [x] content_staging 暂存表 + 原始附件本地保存（sourceUrl、发布单位、发布/抓取时间、原文件名、mimeType、fileHash、原始文件路径）
- [x] 状态机 DISCOVERED → DOWNLOADED → PARSED → DEDUPED → READY → IMPORTED / NEEDS_REVIEW / FAILED，管理页统一中文显示，单条失败只标记自身不影响批次
- [x] 抓取管线：发现 → 下载 → 解析 → 去重 → 分类 → 入库；jsoup 实现，只抓公开页面，不绕过登录 / 验证码 / 付费 / 访问控制
- [x] 三重规则去重（文件 hash → 规范化内容 hash → URL 唯一），AI 不参与删除决策，原始文件全部保留
- [x] trustLevel S / A 自动入库为阅读材料，B / C / D 强制进入“需要人工检查”，低可信内容不参与核心能力测量
- [x] 真实公开来源（人社部门户）完整跑通：388 个链接全部处理，362 条自动入库，26 条进入人工检查，0 条失败
- [x] 第二次抓取相同来源：已存在 URL 全部跳过，无重复入库
- [x] 人工处理路径验证：需要检查条目可查看 / 入库 / 丢弃，状态流转正确
- [x] 管理控台内容管理：内容来源、抓取任务、Staging 分页与筛选、异常处理、内容库存（按知识点统计未使用高质量题）
- [x] 新增 31 项单元测试（Key 加解密 / 掩码 / 默认唯一 / 抓取管线 / 去重 / 状态隔离 / 人工处理），Java 11 下全量 178 项全部通过
- [x] 三表增量合并 schema.sql，upgrade.sql 清理恢复注释；独立空库仅执行 schema.sql、init_data.sql 后 Spring Boot 启动成功
