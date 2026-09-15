# 开发验收报告

## 1. 本次任务

本次目标：完成第六阶段可用页面，并补齐 AnswerRecord → ErrorDiagnosis → LearningProblem 错因诊断链路和微信开发者工具实际验收。

当前所属阶段：第六阶段，小程序 + 管理控台第一批可用页面。

对应 product-plan：题目 / 学习内容 → 真实学习行为 → 能力画像 → 学习问题 → 问题优先级 → 每日计划 → 继续训练。

---

## 2. 实际完成

- 原生小程序实现首页、学习、能力三个底部 Tab，全部读取真实后端 API。
- 首页展示最多 3 个核心问题、今日计划、计划时长、任务目的、原因及学习入口。
- 学习页支持按 DailyPlanItem 获取安全题目、计时、选答案、信心、错因、提交结果、解析、能力变化和下一题。
- 能力页展示知识点四项能力、样本数、状态及 LearningProblem 证据。
- 集中实现规则型 ErrorDiagnosis，根据用户答案、正确答案、错因类型、信心、耗时、知识点和近期同类作答生成候选根因。
- 首次错误只产生低置信待确认候选；同因重复出现提高置信度，并通过唯一约束更新同一条诊断记录。
- 小程序答错后展示“系统判断”及确认/不是操作，用户决定持久化；LearningProblem 和能力页展示当前未否认的主要根因。
- 管理控台实现题库列表/筛选/详情、CSV 导入结果、知识点层级和答题记录。
- 统一 API 地址、超时、业务错误、HTTP 错误、loading 及当前用户上下文。
- 补充前端必需的三个最小后端接口，并为全新用户增加基于真实可用题目的初始训练计划。
- 清理第四、第五和本阶段所有本地验收用户、作答、问题、计划和题目数据。
- 未开发 AI、爬虫、申论、政治阅读、模考、社区、排行榜、复杂权限或大型前端框架。

---

## 3. 主要修改文件

### 新增

- `miniapp/app.js`、`app.json`、`app.wxss`、`project.config.json`
- `miniapp/utils/request.js`
- `miniapp/pages/home/*`
- `miniapp/pages/learn/*`
- `miniapp/pages/ability/*`
- `admin-web/index.html`、`api.js`、`app.js`、`styles.css`
- `backend/src/main/java/com/gkstudy/common/WebConfig.java`
- `backend/src/main/java/com/gkstudy/plan/service/PlanQuestionService.java`
- `backend/src/main/java/com/gkstudy/question/controller/AdminQuestionController.java`
- `backend/src/main/java/com/gkstudy/question/controller/KnowledgePointController.java`
- `backend/src/main/java/com/gkstudy/question/dto/AdminQuestionResponse.java`
- `backend/src/main/java/com/gkstudy/question/mapper/KnowledgePointMapper.java`
- `backend/src/main/java/com/gkstudy/question/model/KnowledgePointView.java`
- `backend/src/main/java/com/gkstudy/question/service/KnowledgePointService.java`
- `backend/src/test/java/com/gkstudy/plan/PlanQuestionServiceTest.java`
- `backend/src/test/java/com/gkstudy/question/FrontendQuestionContractTest.java`
- `backend/src/main/java/com/gkstudy/errordiagnosis/*`
- `backend/src/test/java/com/gkstudy/errordiagnosis/ErrorDiagnosisEngineTest.java`
- `backend/src/test/java/com/gkstudy/errordiagnosis/ErrorDiagnosisServiceTest.java`

### 修改

- Ability、DailyPlan、Question、Practice、LearningProblem 相关模型、Mapper、Service、Controller 和测试
- `miniapp/pages/learn/*`、`miniapp/pages/ability/*`
- `sql/schema.sql`、`sql/upgrade.sql`
- `sql/init_data.sql`
- `docs/progress.md`
- `docs/acceptance-report.md`

### 删除

- 无。

---

## 4. 数据库变化

是否修改数据库：

YES

修改内容：

- 新增 `error_diagnosis`，保存 suspectedCause、evidence、confidence、confirmedByUser、status、出现次数和最近错误选项。
- 唯一键 `(user_id, knowledge_point_id, suspected_cause)` 防止同类诊断无限重复；`selected_option_key` 为后续“选项 → 典型错误原因”保留最小数据入口。
- importance、improvement_potential、transfer_value 是 ProblemPriorityEngine 使用的正式知识点初始配置，予以保留，并在 init_data.sql 增加用途注释。
- 本地用户 94004、95005、96006、96007、96008 及对应验收数据均已精确清理。

schema.sql 是否已同步：

YES

init_data.sql 是否变化：

YES（仅保留正式权重用途注释，未增加验收临时知识点数据）

upgrade.sql 当前状态：当前无待执行增量，仅保留说明注释。

---

## 5. 接口变化

新增接口：

- `GET /api/plan/items/{itemId}/questions`：学习页按当前用户计划项获取不含答案和解析的真实题目。
- `GET /api/admin/questions/{id}`：管理控台查看含答案、解析和知识点的题目详情。
- `GET /api/knowledge-points`：管理控台读取知识点层级及正式权重。
- `POST /api/error-diagnoses/{id}/decision`：保存当前用户对候选错因的确认或否认，并同步 LearningProblem 主要根因。

修改接口：

- `GET /api/questions`：增加 questionType、usageType、keyword 基础筛选及 createTime。
- `GET /api/abilities`：增加 knowledgePointCode、knowledgePointName 供能力页展示。
- `POST /api/questions/import`：结果增加 totalCount。
- DailyPlan：无能力的新用户可从已有真实题目的高重要度知识点生成初始 TRAINING 任务。
- `POST /api/practice/answer`：错误答案增加候选 `errorDiagnosis` 返回，正确答案返回 null。

---

## 6. 编译与测试

### 编译

命令：设置 JDK 11 后执行 `mvn clean test`

结果：

PASS

### 自动测试

命令：`mvn clean test`

通过：55

失败：0

结果：

PASS

补充验证：新增 7 个错因诊断测试，覆盖单次低置信候选、重复同因置信度提升、用户确认、用户否认、正确答案不生成无意义记录、诊断去重及 LearningProblem 读取主要根因；小程序相关 JavaScript 和 JSON 配置通过语法检查。

---

## 7. 启动验证

Spring Boot：

PASS

数据库连接：

PASS

关键启动日志：同一 MySQL 实例创建全新临时库，仅执行 schema.sql、init_data.sql 后得到 14 张表、23 个知识点；最终代码在空库及 application.yml 默认正式库分别出现 `Started GkStudyApplication`，实际请求后均出现 `HikariPool-1 - Start completed`。Bean、MyBatis 映射及 SQL 均无错误。

---

## 8. 实际业务验证

本次实际执行的业务场景：

1. 管理接口导入 7 行 CSV，6 行成功，1 行因无效知识点失败；成功行未回滚。
2. 新用户从首页获得 45 分钟初始训练计划及 6 道安全题目，提交 2 道正确、4 道错误答案。
3. AnswerRecord 增至 6 条，能力更新为 mastery=25.77、speed=43.21、stability=25.33、confidence=45.08、sampleCount=6。
4. 形成 MASTERY/CONFIRMED、STABILITY/CONFIRMED、SPEED/OBSERVING 三类问题；当天计划保持不变，显式重建后改为优先处理稳定性问题。
5. 浏览器管理控台上传 2 行 CSV，页面显示总计 2、成功 1、失败 1及失败行号/原因。
6. 小程序页面逻辑实际作答管理控台导入的题目 #11，管理控台答题记录页面显示答案 B、快照 B、正确、39000ms、DAILY、SURE、UNKNOWN。
7. 将 API 地址切换到不可用端口，管理控台与小程序均显示明确错误状态且不白屏；恢复连接后正常加载。
8. 在独立空库导入真实增长率题目，同一用户连续 3 次以 CONDITION 错因答错：候选置信度由 40 提升至 70，诊断 ID 始终一致；随后用户确认，状态变为 CONFIRMED、confidence=100。
9. 同一用户再正确作答后 AnswerRecord 总数为 4、ErrorDiagnosis 仍为 1；LearningProblem 的 MASTERY/SPEED/STABILITY 均展示“条件理解偏差”。另一用户否认“粗心失误”后诊断为 REJECTED，LearningProblem 根因被清除。

实际结果：真实后端与数据库链路全部正确；错因候选可解释、可累计、可确认/否认且不修改 AnswerRecord；学习端提交前响应无 answer/analysis，提交后才展示正确答案和解析；管理端详情可查看答案。微信开发者工具运行仍为 NOT VERIFIED。

---

## 9. 核心链路验证

涉及核心学习闭环：

YES

若 YES：

```text
管理控台 CSV 导入
→ 题库看到新题
→ 小程序首页 / 今日计划
→ 小程序完成真实题目
→ AnswerRecord
→ ErrorDiagnosis
→ AbilityProfile
→ LearningProblem
→ 能力页刷新
→ 显式重建 DailyPlan
→ 管理控台看到作答
```

验证结果：

PASS

具体数据：

* 答题记录：原第六阶段主链及管理联动记录字段一致；本次独立空库错因链新增 4 条 AnswerRecord，诊断累计过程中原记录未修改。
* 能力变化：年均增长率从初始样本更新至 mastery=25.77、speed=43.21、stability=25.33、confidence=45.08。
* 学习问题变化：生成 3 类问题，其中 MASTERY、STABILITY 为 CONFIRMED，SPEED 为 OBSERVING。
* 错因诊断：同因 3 次错误由 confidence=40 提升至 70，仅 1 条记录；确认后为 100/CONFIRMED，主要根因同步至 LearningProblem；否认后为 REJECTED 并清除对应问题根因。
* 计划变化：首次为“完成年均增长率初始训练以积累真实能力样本”；普通答题后当天 planId 不变；显式重建后为“优先处理年均增长率稳定性问题”。
* 数据联动：管理控台导入题 #11 被小程序实际作答，管理控台随后看到对应 AnswerRecord。

---

## 10. 产品规划偏离检查

是否偏离 `docs/product-plan.md`：

NO

若 YES：

偏离内容：N/A

原因：N/A

是否需要用户决策：

NO

---

## 11. 架构复杂度检查

是否新增：

* 微服务：NO
* Redis：NO
* MQ：NO
* 新SQL文件：NO
* 新配置文件：YES（仅原生小程序必需的 app.json、project.config.json 和页面 json）
* 其他复杂基础设施：NO

说明：小程序使用原生微信方案；管理控台使用无构建依赖的 HTML/CSS/JavaScript；未新增生产依赖、状态框架、UI 框架或复杂权限系统。

---

## 12. 本次发现并修复的问题

1. 新用户没有能力或问题时会得到空计划；增加基于真实可用题目的初始训练候选，作答后由能力和问题优先级接管。
2. 管理控台 API 失败时曾保留上一页面的旧内容；改为清空旧视图，仅显示错误和重试入口。
3. 统一管理控台请求补充 10 秒超时，并修正失败后状态被“就绪”覆盖的问题。
4. Maven target 下两个历史跟踪生成清单会因测试产生提交差异；已按既有 ignore 要求精确还原。
5. 初次通过 MySQL 客户端执行中文 init_data.sql 时因客户端字符集报错；删除专用临时库后指定 utf8mb4，从零仅执行 schema.sql、init_data.sql 验证通过。
6. 修正能力页证据字段名，并增加 LearningProblem 主要根因展示。

---

## 13. 尚未解决的问题

1. 本机未安装微信开发者工具，无法实际执行小程序 IDE 编译及模拟器/真机运行；该项为 NOT VERIFIED。页面逻辑已通过兼容 wx API 的真实后端运行验收，但不能替代微信运行环境验收。

---

## 14. 当前阻塞

微信开发者工具不可用。需要在已安装微信开发者工具的环境中导入 miniapp 目录，完成编译及三 Tab 模拟器/真机验收。

---

## 15. progress.md 更新

本次新增完成项：

* [x] 小程序三 Tab、统一请求和真实学习页面逻辑
* [x] 管理控台四类页面及浏览器真实数据验收
* [x] 最小后端接口、安全答案契约和新用户初始训练
* [x] 管理导入 → 小程序作答 → 能力/问题 → 计划 → 管理记录完整链路
* [x] 后端测试、独立空库及 Spring Boot 启动
* [x] ErrorDiagnosis 规则、去重、置信度累计、用户确认/否认和 LearningProblem 根因联动
* [x] 55 个后端自动测试、14 表/23 知识点空库及 application.yml 默认数据库启动复验

仍未完成：

* [ ] 微信开发者工具实际编译和模拟器/真机页面运行（NOT VERIFIED）

---

## 16. 是否建议进入下一步

NO

原因：ErrorDiagnosis、真实 API、空库、正式本地数据库和管理控台均已通过，但按项目“不允许伪造验收”规则，小程序尚缺微信开发者工具环境的实际运行验证。

---

## 17. 最终验收结论

FAIL

如果 FAIL：

必须先修复：

1. 在微信开发者工具中导入 `miniapp`，确认首页、学习、能力三 Tab 编译和运行正常。
2. 在该环境复验正确/错误答题、loading/empty/error 与重复提交保护后，才能将第六阶段改为 PASS。
