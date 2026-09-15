# 开发验收报告

## 1. 本次任务

本次目标：根据 AbilityProfile 与真实 AnswerRecord 识别并持续跟踪掌握、速度和稳定性学习问题。

当前所属阶段：第四阶段，LearningProblem 学习问题。

对应 product-plan：真实学习行为 → 能力画像 → 学习问题；问题改善后进入验证，验证通过后解决。

---

## 2. 实际完成

- 集中实现 LearningProblemEngine，统一管理三类问题识别与生命周期规则。
- 答题事务内在 AbilityProfile 更新后创建或更新 LearningProblem。
- 实现 OBSERVING → CONFIRMED → PROCESSING → VERIFYING → RESOLVED → REOPENED。
- 同一用户、知识点、问题类型复用同一主记录，并保存状态变化历史。
- 保存近期错误、超时、波动、能力分、置信度、样本数和验证计数等可解释证据。
- 提供 LearningProblem 查询接口；能力回放不制造或更新 LearningProblem。
- 未开发 DailyPlan、AI、爬虫、申论、政治阅读或模考。

---

## 3. 主要修改文件

### 新增

- `backend/src/main/java/com/gkstudy/learningproblem/engine/LearningProblemEngine.java`
- `backend/src/main/java/com/gkstudy/learningproblem/model/LearningProblem.java`
- `backend/src/main/java/com/gkstudy/learningproblem/mapper/LearningProblemMapper.java`
- `backend/src/main/java/com/gkstudy/learningproblem/service/LearningProblemService.java`
- `backend/src/main/java/com/gkstudy/learningproblem/controller/LearningProblemController.java`
- `backend/src/test/java/com/gkstudy/learningproblem/LearningProblemEngineTest.java`
- `backend/src/test/java/com/gkstudy/learningproblem/LearningProblemServiceTest.java`

### 修改

- `backend/src/main/java/com/gkstudy/practice/service/PracticeService.java`
- `backend/src/test/java/com/gkstudy/practice/PracticeServiceTest.java`
- `sql/schema.sql`
- `sql/init_data.sql`
- `sql/upgrade.sql`
- `docs/progress.md`
- `docs/acceptance-report.md`

### 删除

- 无。

---

## 4. 数据库变化

是否修改数据库：

YES

修改内容：

- 新增 `learning_problem_history`，保存同一 LearningProblem 的状态迁移和当时证据。
- 继续使用 `learning_problem` 唯一键约束用户、知识点、问题类型去重。
- `schema.sql`、`init_data.sql` 改为在连接指定的数据库中执行，便于安全验收独立空库，不改变表和初始化数据语义。

schema.sql 是否已同步：

YES

init_data.sql 是否变化：

YES（仅移除硬编码 `USE`，初始化内容未变化）

upgrade.sql 当前状态：增量已在现有数据库执行验证并合并至 schema.sql，文件已清理为说明注释。

---

## 5. 接口变化

新增接口：

- `GET /api/learning-problems`：按当前用户查询问题类型、知识点、状态、严重度、基础优先分、证据、发现时间和解决时间。

修改接口：

- `POST /api/practice/answer`：AnswerRecord 与 AbilityProfile 更新后，在同一事务内自动评估 LearningProblem；响应格式不变。

---

## 6. 编译与测试

### 编译

命令：设置 JDK 11 后执行 `mvn clean test`

结果：

PASS

### 自动测试

命令：`mvn clean test`

通过：30

失败：0

结果：

PASS

覆盖：样本不足、持续低 mastery、速度异常、稳定性异常、低置信度保护、问题去重、PROCESSING、VERIFYING、连续三次验证后 RESOLVED、验证失败重置、RESOLVED 后 REOPENED、回放不制造问题，以及原有题库、答题和能力回归测试。

---

## 7. 启动验证

Spring Boot：

PASS

数据库连接：

PASS

关键启动日志：最终代码连接独立空库后 `Started GkStudyApplication in 3.135 seconds`；实际请求 LearningProblem 接口后 `HikariPool-1 - Start completed`，Bean、MyBatis、SQL 均无启动或执行错误。

---

## 8. 实际业务验证

本次实际执行的业务场景：

1. 导入 4 道“年均增长率”验收题，使用独立用户依次提交错、错、超时答对、错。
2. 连续稳定答对 4 次，观察问题进入 PROCESSING、VERIFYING。
3. 再提交 3 次 VALIDATION 正确答案，观察问题进入 RESOLVED。
4. 后续提交 6 次明显退步的验证答案，观察已解决问题进入 REOPENED。
5. 执行能力回放，比较 AnswerRecord、AbilityHistory、LearningProblem 和问题历史数量及状态。

实际结果：MASTERY 问题完整经历 OBSERVING、CONFIRMED、PROCESSING、VERIFYING、RESOLVED、REOPENED；对应主记录始终只有 1 条，证据随表现更新，6 次状态迁移均留有历史。

---

## 9. 核心链路验证

涉及核心学习闭环：

YES（本阶段验证到 LearningProblem，不开发 DailyPlan）

若 YES：

```text
题目
→ 答题
→ AnswerRecord
→ AbilityProfile
→ LearningProblem
```

验证结果：

PASS

具体数据：

* 答题记录：17 条；回放前后均为 17 条，原始记录未修改。
* 能力变化：mastery 从初始 50 经异常降至 29.07，稳定训练后升至 76.17，后续退步降至 41.82。
* 学习问题变化：MASTERY 主记录 1 条；状态按 OBSERVING → CONFIRMED → PROCESSING → VERIFYING → RESOLVED → REOPENED 变化。
* 证据：REOPENED 时最近 6 题错 6 题、mastery=41.82、confidence=80.83、样本数=17。
* 回放隔离：回放前后 AnswerRecord 17、AbilityHistory 17、LearningProblem 3、问题历史 12，MASTERY 状态均为 REOPENED。
* 计划变化：N/A，本阶段未开发 DailyPlan。

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
* 新配置文件：NO
* 其他复杂基础设施：NO

说明：未新增生产依赖；规则集中在 LearningProblemEngine，持久化沿用 MyBatis 和现有 MySQL 单体架构。

---

## 12. 本次发现并修复的问题

1. 基线 SQL 硬编码数据库名，无法在同一实例安全验证独立空库；改为使用连接指定的数据库。
2. PowerShell 文本管道会破坏 SQL 中的 UTF-8 中文；空库验收改由 MySQL 直接读取原始 SQL 文件并复验通过。
3. 验证阶段若累计过一次失败，原规则会永久无法满足全通过条件；改为连续验证通过计数，失败即重置。
4. 默认 8080 端口被占用；改用独立验收端口 18080 后启动通过。

---

## 13. 尚未解决的问题

1. 现有本地 `gk_study_helper` 中保留本次用户 94004 和“第四阶段验收”测试数据；自动清理因核心数据删除安全限制未执行，不影响功能验收。

---

## 14. 当前阻塞

无。

---

## 15. progress.md 更新

本次新增完成项：

* [x] 三类 LearningProblem 识别与集中规则
* [x] 完整生命周期、去重、证据与状态历史
* [x] 答题后自动更新与能力回放隔离
* [x] 自动测试、真实链路、空库及启动验收

仍未完成：

* [ ] DailyPlan 与下一次计划训练优先级调整（不在本阶段范围）

---

## 16. 是否建议进入下一步

YES

原因：系统已能根据真实能力和答题表现稳定识别、验证、解决并重新打开学习问题；本次按要求结束，不继续开发 DailyPlan。

---

## 17. 最终验收结论

PASS
