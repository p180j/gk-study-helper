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
- [x] AI Coach 页改为三层结构：结论 / 关键证据（最多 3 条）/ 下一步建议，完整 AI 分析默认折叠可展开
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

## 第十阶段：模考 + 考试能力校准

- [x] 新增模考数据模型六表：mock_paper / mock_paper_section / mock_paper_item / mock_session / mock_answer / mock_result，复用现有 Question / EssayQuestion，不复制题库
- [x] 行测全真模考：选卷 → 考试说明 → 总计时 → 连续作答 → 跳题 / 切题 → 主动交卷 / 到时自动交卷，考试中不显示答案与解析
- [x] 每题记录作答顺序、耗时、用户答案、正确答案快照、resultStatus（CORRECT / WRONG / UNANSWERED / SKIPPED / TIMEOUT，跳过与未做严格区分）
- [x] 申论整套计时模考：材料 + 多小题共用总计时，记录每题开始时间、用时、原始答案、字数、作答顺序；交卷后调用 AiEssayGrader 真实评分，AI 失败降级不阻断交卷
- [x] 模考结果分析：总分、正确率、完成率、总耗时 + 各模块得分 / 正确率 / 完成率 / 平均耗时 / 超时题数 / 未答题数 / 后半程正确率下降
- [x] 专项能力 vs 模考表现对比与考试能力校准集中实现 MockCalibrationEngine：区分知识薄弱（MASTERY）与完成率 / 时间分配 / 稳定性 / 做题顺序 / 表现落差（EXAM_*）五类考试问题，多次模考提升校准 confidence
- [x] 模考不覆盖 AbilityProfile：全程只读日常能力，校准结论写入 mock_result.calibration_json，能力画像保持不变（已实测验证）
- [x] 模考问题进入 LearningProblem 并复用完整生命周期：一次 OBSERVING、重复 CONFIRMED、改善后 PROCESSING → VERIFYING → RESOLVED
- [x] 模考结果影响下一次 DailyPlan：模考时间分配问题驱动资料分析限时训练安排，知识薄弱安排基础 TRAINING，不修改当日已有计划
- [x] MOCK_RESERVED 题目严格隔离：不进入日常训练 / 摸底 / 验证选题，试卷启用前校验卷内题目全部为模考专用
- [x] EXAM_ORDER_STRATEGY 做题顺序问题识别：按全局作答顺序统计模块穿插度（连续段数）
- [x] init_data.sql 新增初始模考内容：10 道 MOCK_RESERVED 题（言语 / 判断 / 数量 / 资料 / 常识各 2 道）+ 行测全真模拟（第一套）+ 申论全真模拟（第一套），空库即可模考
- [x] 小程序学习页新增“模拟考试”入口（保持三 Tab），试卷列表 / 考试说明 / 行测与申论考试页 / 交卷确认 / 结果 / 模块分析 / 专项能力对比全中文展示，内部枚举统一映射
- [x] 能力页新增“考试表现”区块：专项能力 vs 模考表现对比，明确说明专项能力 ≠ 预测分数
- [x] 管理控台模考试卷管理：创建 / 详情 / 题目组成 / Section / 启用停用 / 模考记录与结果查看
- [x] 真实验收场景 A-E 全部通过：行测完整模考（顺序 / 耗时 / 对错 / 跳过 / 未答 / 超时）、能力差异识别（资料日常 78 模考 50 → 时间分配而非知识薄弱）、知识薄弱增强（数量 40/0 → MASTERY）、重复模考状态推进（OBSERVING → CONFIRMED → PROCESSING）、申论整套模考（DeepSeek 真实评分 92/88、原始答案保留、每题耗时可见）
- [x] 到时自动交卷实测：超时 session 访问即自动交卷（AUTO_TIME_LIMIT）并补齐 UNANSWERED
- [x] 新增 26 项模考单元测试（试卷隔离 / 会话创建 / 每题耗时顺序 / 五种 resultStatus / 自动与主动交卷 / 结果聚合 / 五类考试问题判定 / 申论评分成功缩放与失败降级 / 不覆盖能力 / 问题生成与状态推进 / 计划联动），Java 11 下全量 203 项全部通过
- [x] 模考六表与初始数据增量已验证并合并 schema.sql / init_data.sql，upgrade.sql 清理恢复注释
- [x] 独立空库仅执行 schema.sql、init_data.sql（29 张表、10 道模考题、2 套试卷）后 Spring Boot 启动验收通过
- [x] 收口修复：用户可见时间统一人类可读格式（倒计时 01:59:52、平均耗时 119分52秒 / 32秒、管理控台 32 秒 / 2.5 分钟，禁止展示原始毫秒与大整数秒）；无专项能力数据显示“未测评”不显示“专项能力 0”；结果页正确率 / 完成率为主指标、耗时 / 超时 / 跳过 / 未答为次级信息（计算逻辑零改动）

## 第十一阶段：题库采集中心（采集自动化 + 质量门禁 + 批量上传）

- [x] ContentQualityService 集中纯规则质量门禁：题干 / 选项 / 答案 / 解析 / 乱码噪声 / 知识点 / 来源可信度（S/A +10、B 0、C/D -15）打分，score>=60 且无致命问题且 confidence>=70 视为通过，不用 AI，可独立单测
- [x] QuestionTextExtractor 从纯文本抽取单选题候选：题号 / 题型标记切块 + A-D 选项行 + 答案行 + 可选解析，题干中出现的答案字样不干扰抽取，无答案候选同样输出由门禁拦截，无结构返回空列表
- [x] QuestionStagingImportService 分流入库：S/A 质量通过自动入库；B 需 confidence>=80（记抽样质检提示）；C/D 需 confidence>=85（source_level 写入，验证取题自然排除）；不满足生成逐题 NEEDS_REVIEW 暂存（原 URL#idx-n 防冲突、质量三字段、中文异常原因），原始数据不丢失
- [x] content_hash 与手工 CSV 导入完全一致（sha256(stem|A:text|...|answer)），与 question 表撞记 duplicates 跳过，全局统一去重
- [x] 疑似试题采集内容不再一律人工复核：先抽取候选再走质量分流，自动入库后 staging 标记 IMPORTED + “自动入库 N 题/重复 M/异常 K” 备注；抽取不出结构才保留原人工路径
- [x] 采集改造异步：POST /sources/{id}/crawl 立即返回 {logId,status}，后台单线程执行并回写 content_crawl_log（RUNNING/SUCCESS/FAILED + 各阶段计数），同来源 RUNNING 时拒绝重复触发（CRAWL_ALREADY_RUNNING）
- [x] GET /crawl-logs 采集日志列表（最近 20 条，新→旧，原始枚举由前端映射中文）
- [x] GET /inventory 重构为聚合响应 InventoryOverviewView：总览（可训练 / 模考专用 / 申论 / AI 生成 / 待人工检查 / 采集失败来源）+ 一级模块（含子知识点、lowStock=unused<阈值，阈值 @Value 默认 10）
- [x] POST /upload 题目文件批量上传：CSV / XLSX / XLS（Apache POI，表头同 CSV 模板），保存原始文件 → file 级 staging（ATTACHMENT）→ 逐行转候选 → 质量分流入库 → 同步返回 {stagingId,total,imported,duplicates,needsReview,failed,errors 前10条}，重复文件上传直接拒绝
- [x] POST /staging/{id}/review 支持 IMPORT_QUESTION（修正题目入库，校验同手工导入规则）与 CONFIRM_DUPLICATE，保留 IMPORT_MATERIAL / DISCARD；新增 POST /staging/batch-review 批量丢弃 / 确认重复
- [x] content_staging 增量：quality_score / quality_confidence / quality_issues 三字段（upgrade.sql 追加 + schema.sql 合并，空库基线完整）；Question 模型映射 source_level / quality_score
- [x] 新增 55 项单元测试（质量门禁 13 / 抽取器 8 / 分流入库 13 / 上传 8 / 采集与人工处理 13 适配扩展），Java 11 下全量 273 项全部通过
- [x] 后端接口层面真机验证：手动触发异步采集（立即返回 logId、日志回写、重复触发拦截）、文件批量上传（CSV/XLSX 分流入库）、库存聚合查询

## 部署前收口：主闭环修复 + 题库采集中心前端

- [x] DailyPlanItem 任务模型补全：target_question_count / completed_question_count 入库，完成判定唯一标准为 completedQuestionCount >= targetQuestionCount，杜绝“做 1 题即提示完成”
- [x] QuestionTaskPolicy 集中按时间 / 题型 / 任务类型计算目标题量，消除散落 magic number
- [x] QuestionInventoryService 统一按模块 / 知识点统计可用库存（排除 MOCK_RESERVED），DailyPlan 生成前必须检查真实库存，不足标记 INSUFFICIENT_STOCK 不生成虚假任务
- [x] 新用户摸底最小可信样本：候选知识点库存 >= 5 才生成摸底任务，单知识点不足自动扩展同模块兄弟知识点，整个模块不足不生成虚假任务
- [x] 取题顺序固定：当前知识点未做 → 当前已做 → 同模块兄弟未做 → 兄弟已做
- [x] 题库不足不伪完成：前端页面内提示“已完成 X/Y，当前可用题目不足”，后端不标记完成、不伪造进度
- [x] 小程序答题页重构为 answering / result / task_completed 三态互斥状态机：答对不显示错因选择（错因移至结果卡仅答错显示），最后一题完成后不显示“继续下一题”，不再用中央 Toast 遮挡页面
- [x] 能力变化增加可读解释（掌握 / 速度升降含义说明）
- [x] 答题幂等：answer_record 增加 (user_id, plan_item_id, question_id) 索引，同一题同一任务重复提交返回旧结果，不重复计数、不重复更新能力
- [x] 管理控台新增一级功能“题库采集”5 页：库存概览（总览 + 模块 + 子知识点 + 低库存标记）/ 手动触发采集（异步触发 + 日志轮询）/ 文件导入（CSV / XLSX / XLS）/ 异常处理（逐题修正 + 批量丢弃 / 确认重复）/ 采集记录
- [x] 修复三处核心缺陷：模块级知识点库存统计越界扩展到同科目其它模块（库存虚高）、daily_plan_item UPDATE 列顺序求值导致提前误判完成、小程序库存汇总 exam_type 筛选条件与实际存储不符导致空结果
- [x] 爬虫缺失知识点编码时按题干强特征词自动推断模块级知识点（推断不出才进异常处理），提升自动入库率
- [x] 真实业务验收场景 A-H 全部通过：任务题量与完成判定 / 摸底最小样本与库存扩展 / 库存不足提示不伪完成 / 手动采集与文件导入 / 自动入库与异常分流 / 幂等提交 / MOCK_RESERVED 隔离 / 答题页状态机
- [x] 全量单元测试 273 项全部通过（Java 11）；admin-web 与小程序脚本 node --check 通过
- [x] 增量在主库验证后合并 schema.sql，upgrade.sql 清理恢复注释
- [ ] 管理控台题库采集 5 页浏览器真机操作验收（NOT VERIFIED：脚本语法已检查，待浏览器实际操作确认）
- [ ] 独立空库验收（SKIPPED：本次按用户指示跳过，主库增量已实际执行验证，schema.sql 已同步合并）

## 产品全流程打通验收（本轮）

- [x] 后端统一托管管理后台：`http://127.0.0.1:8089/admin/`、静态资源及 `/api/...` 实际返回 200
- [x] 管理后台库存改为复用 `QuestionInventoryService` 的普通可训练题定义；实际库存为 66 道（言语 13 / 判断 16 / 数量 11 / 资料 12 / 常识 14）
- [x] 真实 CSV 经“上传 → staging → 质量门禁 → 自动入库 / 异常处理”导入 50 道，49 道自动入库、1 道异常修正后入库
- [x] 真实缺答案异常记录未进入 Question；通过管理后台修正答案后入库，普通可训练库存实际从 65 增至 66
- [x] 公开题源采集的 staging 自动入库 7 道题；同一来源重复采集后 Question 数保持 7，不重复增长
- [x] 新用户 99002 由真实库存生成 3 组 5 题摸底任务；首组连续提交 5 题，前四题未完成、第五题才完成
- [x] 同一用户完成全部 3 组任务后 DailyPlan 与全部 DailyPlanItem 均为 COMPLETED；共产生 18 条 AnswerRecord、18 条 AbilityHistory、3 条 AbilityProfile、1 条 LearningProblem、1 条 ErrorDiagnosis
- [x] 显式生成下一计划后，任务从纯 ASSESSMENT 调整为资料分析稳定性 TRAINING + 言语 MAINTENANCE + 数量 ASSESSMENT
- [x] 自由练习补齐模块选择及无 planItemId 的真实取题路径；实际连续答 3 题且 DailyPlanItem 进度保持不变
- [x] 小程序答对时未传 `errorType` 不再返回 400：服务端缺省持久化为 `UNKNOWN`；以真实请求实际返回 HTTP 200，并写入 AnswerRecord
- [x] Java 11 `mvn clean test` 实际通过：279 项，0 失败
- [x] Spring Boot 实际启动并连接主库：8089 端口正常监听
- [ ] 微信开发者工具小程序 UI 点击、截图及连续作答验收（NOT VERIFIED：官方 CLI 已实际导入并打开项目，但本会话的 GUI 自动化连接返回空应用/连接错误，无法伪造交互结果）

## 以真实备考用户为中心的产品体验改造（本轮）

- [x] 首页收敛为“今天学什么”：当前推荐任务、完成进度、安排原因和唯一主操作“继续今日学习”
- [x] 学习页补齐真实可用的任务切换：展示今日任务状态/进度，切换不重建 DailyPlan、不丢失进度
- [x] 学习页补齐主动练习：按模块、按当前学习问题、按错题知识点练习；均走真实题目和 EXTRA AnswerRecord 链路，不推进 DailyPlanItem
- [x] 能力页改为行测综合能力 + 六维能力图（言语/判断/数量/资料/常识/申论）+ 可展开模块详情；未测评维度不再显示为 0 分
- [x] 新增集中中文展示映射，任务目的、问题类型、状态、错因和 AI 文本不再向用户暴露内部英文枚举
- [x] AI Coach 改为先展示真实数据洞察、用户主动请求完整 AI 分析；AI 不可用时保留中文洞察与重试入口
- [x] Java 11 `mvn clean test` 实际通过：279 项，0 失败；小程序纯展示聚合与中文映射 Node 自动测试通过
- [x] 现有主库 API 实际验证：计划、能力概览、核心问题、AI 洞察、计划取题、主动练习取题均返回成功，题目答案未提前返回
- [x] 经用户明确授权后，真实 `POST /api/ai/coach` 调用 DeepSeek 成功；返回为简体中文，且未出现内部英文枚举
- [x] 学习页明确区分“切换今日任务”和“自主选择练习”；五个行测模块在首屏即可按真实库存直接进入练习
- [x] 六维能力图始终绘制完整六轴底图和模块标签；未测维度明确显示“待测评”，不填充为 0 分
- [x] 修改后小程序脚本语法和能力聚合测试通过；微信开发者工具 CLI 实际重新信任并打开项目
- [x] 能力分改为正式评估口径：单模块累计 20 道有效作答后才展示分数；此前统一显示“摸底中”
- [x] 行测综合能力分改为五个行测模块均完成正式评估后才展示，禁止以单个模块高分代替整体能力
- [x] Java 11 `mvn test` 实际通过：280 项，0 失败；前端能力聚合测试通过
- [ ] 微信开发者工具 GUI 点击、截图与小程序端连续作答（NOT VERIFIED：官方 CLI 已打开并信任项目、Agent 服务已启动，但本会话 GUI 自动化连接两次返回空窗口/连接错误）
- [ ] Java 11 `mvn clean test`（FAIL：IDEA 正在运行的 gk-study-helper 锁定 `backend/target/test-classes`，未停止用户运行实例）
- [ ] 新评分规则的 8089 运行复验与当前用户画像回放（NOT VERIFIED：等待明确授权停止并重启 IDEA 启动的旧 Spring Boot 进程）
