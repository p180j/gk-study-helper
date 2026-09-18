# 开发验收报告

## 1. 本次任务

本次目标：以真实备考用户为中心重构小程序首页、学习页、能力页和 AI Coach；让每日计划是推荐而不是限制，用户能主动选择练习，并以中文理解能力与建议。

当前所属阶段：产品体验改造（不进入下一阶段）

对应 product-plan：题目/学习内容 → 真实学习行为 → 能力画像 → 学习问题 → 问题优先级 → 每日计划 → 继续训练。

---

## 2. 实际完成

- 首页只保留“今天学什么”：当前推荐任务、完成进度、安排原因及唯一主操作“继续今日学习”。
- 学习页增加真实任务切换、按模块主动练习、按当前问题练习、按错题知识点回顾；主动练习复用既有真实取题与 AnswerRecord 链路。
- 学习页将“切换今日任务”和“自主选择练习”分开；五个行测模块直接按真实库存进入练习，避免把当天计划误解为全部可选模块。
- 能力页改为行测综合能力、始终完整可见的六维能力图和按需展开详情；未测评数据明确展示为“待测评”，不会被当成 0 分或造成图形残缺。
- 能力分增加 20 道有效作答门槛；摸底阶段只显示“摸底中”，行测综合分必须五个模块均达到门槛，不能由单个模块替代。
- 集中映射用户可见的任务目的、问题、状态、错因和 AI 文本；AI Coach 改为先展示真实洞察，再由用户请求完整 AI 分析。

---

## 3. 主要修改文件

### 新增

- `miniapp/utils/ability-view.js`
- `miniapp/utils/ability-view.test.js`

### 修改

- `miniapp/app.js`
- `miniapp/utils/display.js`
- `miniapp/pages/home/home.{js,wxml,wxss}`
- `miniapp/pages/learn/learn.{js,wxml,wxss}`
- `miniapp/pages/ability/ability.{js,wxml,wxss}`
- `miniapp/pages/coach/coach.{js,wxml}`
- `backend/src/main/java/com/gkstudy/coach/service/AiCoachService.java`
- `docs/progress.md`
- `docs/acceptance-report.md`

### 删除

- 无。

---

## 4. 数据库变化

是否修改数据库：

NO

修改内容：

- 无表、字段或数据语义变更；复用既有 AbilityProfile、DailyPlan、LearningProblem、AnswerRecord 与 AI Coach 数据。

schema.sql 是否已同步：

N/A

init_data.sql 是否变化：

NO

upgrade.sql 当前状态：

未新增增量。

---

## 5. 接口变化

新增接口：

- 无；页面复用 `GET /api/plan/today`、`GET /api/abilities/overview`、`GET /api/learning-problems/core`、`GET /api/practice/answers`、`GET /api/questions/practice`、`GET/POST /api/ai/coach/...`。

修改接口：

- 无接口兼容性变更；AI Coach 的系统提示补充“简体中文、禁止内部枚举/字段名”约束。

---

## 6. 编译与测试

### 编译

命令：

`JAVA_HOME=D:\all_jdk\jdk11_23\jdk mvn clean test`

结果：

FAIL：两次执行均无法删除 `backend/target/test-classes/...ReadingServiceTest.class`，该目录被 IDEA 正在运行的 gk-study-helper 实例锁定。

### 自动测试

命令：

`mvn test`；`node miniapp/utils/ability-view.test.js`

通过：

- Java 11：280 项，0 失败。
- 小程序能力视图测试：通过未测评不作为 0 分、20 题正式评分门槛、行测综合分不被单模块替代、申论聚合、分数边界裁剪和英文枚举中文映射。
- 本次界面调整：`node --check miniapp/pages/learn/learn.js`、`node --check miniapp/pages/ability/ability.js` 通过；`git diff --check` 无空白错误。

失败：

0

结果：

PASS（不包含被运行实例锁定的 `clean` 步骤）。

---

## 7. 启动验证

Spring Boot：

PASS

数据库连接：

PASS

关键启动日志：

已有 Java 后端仍在 8089 端口监听，但由 IDEA 在本次评分调整前启动；当前新评分规则尚未随服务重启加载，运行复验为 NOT VERIFIED。此前 `GET /api/plan/today`、`GET /api/abilities/overview`、`GET /api/learning-problems/core` 均实际返回 HTTP 200。

---

## 8. 实际业务验证

本次实际执行的业务场景：

1. 用户 1 的今日计划实际返回 2 个任务；计划任务 56 为数量关系摸底，进度 2/10；任务 57 为常识判断，进度 10/10。
2. 两个计划任务分别通过真实 `GET /api/plan/items/{id}/questions?limit=3` 取题；均返回 3 题且响应不含答案。
3. `GET /api/questions/practice?knowledgePointCode=DATA_ANALYSIS&limit=3` 实际成功返回 3 道主动练习题，响应不含答案。
4. `GET /api/abilities/overview` 实际返回已评估 5/13；`GET /api/learning-problems/core` 实际返回 1 个核心问题；`GET /api/ai/coach/insight` 实际返回错误诊断来源洞察。
5. 微信开发者工具官方 CLI 已实际信任并打开项目，Agent 自动化服务启动成功；CLI Agent 编译请求被工具自身 `agent.skills is empty in app.json` 阻止。
6. 经用户明确授权后，真实 `POST /api/ai/coach` 使用当前用户学习上下文调用 DeepSeek 成功；返回“今天最值得练数量关系……”等简体中文建议，未出现内部英文枚举。
7. 本次界面调整后，微信开发者工具 CLI 重新执行 `auto --project miniapp --trust-project` 成功；工具服务正常启动并使用项目 AppID。
8. 新评分规则回归：连续 4 道正确作答仍低于 70 分；累计不足 20 题的模块和覆盖不全的行测综合能力均不会返回展示分数。

实际结果：

- 真实后端数据可支撑任务切换、主动练习、能力图和中文 AI 洞察。
- AI Coach 的真实 DeepSeek 请求成功，中文输出约束实际生效。
- 小程序 JS 全部通过 `node --check`，能力聚合测试通过。
- 微信开发者工具 GUI 点击、截图和连续作答未完成，见“尚未解决的问题”。
- 8089 上的旧 IDEA 运行实例尚未重启，因此不能将新评分规则标记为已运行生效。

---

## 9. 核心链路验证

涉及核心学习闭环：

YES

```text
题目
→ 答题
→ AnswerRecord
→ Ability
→ LearningProblem
→ DailyPlan
```

验证结果：

PASS（后端真实链路与本轮所复用 API）；小程序 GUI 端到端交互为 NOT VERIFIED。

具体数据：

* 答题记录：主动练习继续使用 `EXTRA`，不携带 `planItemId`，沿用既有“不推进 DailyPlanItem”自动测试。
* 能力变化：能力概览实际为 5/13；六维图仅聚合真实已测模块，未测评维度为 `null` 展示而非 0 分。
* 学习问题变化：核心问题 API 实际返回 1 条，学习页可按其知识点进入主动练习。
* 计划变化：任务切换仅改变小程序当前训练上下文，不调用重建计划接口；当天计划稳定性保持不变。

---

## 10. 产品规划偏离检查

是否偏离 `docs/product-plan.md`：

NO

若 YES：

不适用。

是否需要用户决策：

NO

---

## 11. 架构复杂度检查

是否新增：

* 微服务：NO
* Redis：NO
* MQ：NO
* 新SQL文件：NO
* 新配置文件：NO
* 其他复杂基础设施：NO

说明：仅增加小程序展示层聚合与集中中文映射，复用现有单体后端和 API。

---

## 12. 本次发现并修复的问题

1. “切换任务”只有文案，没有可执行交互——已实现任务选择卡，明确说明切换不重建计划、不丢失进度。
2. 首页与学习页重复承担“开始学习”——首页收敛为今日推荐，学习页承担任务工作台和主动练习。
3. 能力页把速度、稳定性、置信度等内部算法指标直接平铺，未测评容易被理解为 0 分——已改为综合能力、六维图、按需展开详情和未测评状态。
4. 内部英文枚举直接显示给用户——已建立集中中文映射，并约束 AI Coach 输出简体中文。
5. AI Coach 一打开就调用外部服务，短暂失败时会削弱基础页面可用性——已改为先显示真实洞察，由用户主动请求完整 AI 分析。
6. “切换任务”看起来像只能选择两个模块——已明确为“切换今日任务”，并将五个行测模块的自主练习入口前置。
7. 六维图只有全部测评后才闭合，申论未测评时容易被误认为页面只展示一半——已改为完整六轴底图、每轴标签与“待测评”状态，数据线只连接相邻已测维度。
8. 单个常识模块的 100 分被当成“行测综合能力 100”——已要求五模块全部完成 20 题正式评估后才展示综合分；此前仅显示能力画像建立中。

---

## 13. 尚未解决的问题

1. 微信开发者工具 GUI 自动化连接两次返回空窗口与 `nodeRepl.fetch request failed`；无法实际点击、截图或验证连续作答，必须标记为 NOT VERIFIED。
2. 新评分规则尚未加载到 8089 的 IDEA 运行实例；需要明确授权停止并重启该实例后才能回放当前用户画像并实测。
3. `mvn clean test` 两次失败：IDEA 运行实例锁定 `backend/target/test-classes`；`mvn test` 已实际通过 280 项。

---

## 14. 当前阻塞

微信开发者工具 GUI 自动化连接不可用；新评分规则等待当前 IDEA 后端安全重启后运行复验；真实 AI Coach 外发调用已在用户授权后完成验证。

---

## 15. progress.md 更新

本次新增完成项：

* [x] 首页、学习、能力、AI Coach 的用户视角信息架构改造
* [x] 真实任务切换、主动练习、六维能力图和集中中文映射
* [x] Java 11 全量测试与小程序能力视图自动测试
* [x] 用户授权后的真实 DeepSeek AI Coach 调用与中文输出验证
* [x] 学习页任务/自主练习入口分离与完整六轴能力图展示
* [x] 20 题能力评分门槛、五模块行测综合分门槛与 280 项 Java 回归

仍未完成：

* [ ] 微信开发者工具真实 UI 点击、截图与连续答题（NOT VERIFIED）
* [ ] Java 11 `mvn clean test`（IDEA 锁定构建产物）
* [ ] 新评分规则运行重启、能力回放和真实接口验证（等待用户授权）

---

## 16. 是否建议进入下一步

NO

原因：后端回归、脚本和真实 DeepSeek 调用已验证，但新评分规则尚未重启到运行实例；同时用户明确要求的微信开发者工具实际运行/截图和 `mvn clean test` 尚未完成。

---

## 17. 最终验收结论

FAIL

如果 FAIL：

必须先修复：

1. 恢复微信开发者工具 GUI 自动化连接，完成首页、任务切换、主动练习、能力图、AI Coach、错误结果的实际点击和截图验收。
2. 经用户授权后停止并重启 IDEA 启动的 gk-study-helper 后端，回放当前用户 AbilityProfile 并验证“摸底中 / 20 题能力分 / 五模块综合分”真实接口结果。
3. 在停止锁定进程后执行 Java 11 `mvn clean test`。
