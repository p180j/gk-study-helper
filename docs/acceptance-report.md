# 开发验收报告

## 1. 本次任务

本次目标：根据 AbilityProfile 与 LearningProblem 计算问题优先级，并生成稳定、可解释、可追溯的当日学习计划。

当前所属阶段：第五阶段，问题优先级 + DailyPlan。

对应 product-plan：学习问题 → 问题优先级 → 每日计划；在下一次计划中提高核心问题训练优先级。

---

## 2. 实际完成

- 集中实现 ProblemPriorityEngine，统一计算问题优先级并选取最多 3 个核心问题。
- 集中实现 DailyPlanEngine，支持 20、45、60、90 分钟计划及 QUESTION_SET、REVIEW 两类任务。
- VERIFYING 问题生成 VALIDATION 任务；CONFIRMED、PROCESSING、REOPENED 生成 TRAINING 任务。
- 排除 RESOLVED，降低低置信度 OBSERVING 的排序影响，并允许高收益问题优先于单纯最低能力项。
- DailyPlanItem 保存 LearningProblem、KnowledgePoint、目的、计划分钟数、顺序和具体原因。
- 当日计划默认幂等；仅显式生成接口重建，普通答题不触发当天计划变化。
- 提供当日计划、显式计划生成和核心问题查询接口。
- 未开发 AI、爬虫、申论、政治阅读、模考、小程序或管理后台。

---

## 3. 主要修改文件

### 新增

- `backend/src/main/java/com/gkstudy/priority/engine/ProblemPriorityEngine.java`
- `backend/src/main/java/com/gkstudy/priority/service/ProblemPriorityService.java`
- `backend/src/main/java/com/gkstudy/plan/controller/DailyPlanController.java`
- `backend/src/main/java/com/gkstudy/plan/dto/GeneratePlanRequest.java`
- `backend/src/main/java/com/gkstudy/plan/engine/DailyPlanEngine.java`
- `backend/src/main/java/com/gkstudy/plan/mapper/DailyPlanMapper.java`
- `backend/src/main/java/com/gkstudy/plan/model/DailyPlan.java`
- `backend/src/main/java/com/gkstudy/plan/model/DailyPlanItem.java`
- `backend/src/main/java/com/gkstudy/plan/model/MaintenanceCandidate.java`
- `backend/src/main/java/com/gkstudy/plan/service/DailyPlanService.java`
- `backend/src/test/java/com/gkstudy/priority/ProblemPriorityEngineTest.java`
- `backend/src/test/java/com/gkstudy/plan/DailyPlanEngineTest.java`
- `backend/src/test/java/com/gkstudy/plan/DailyPlanServiceTest.java`

### 修改

- `backend/src/main/java/com/gkstudy/learningproblem/controller/LearningProblemController.java`
- `backend/src/main/java/com/gkstudy/learningproblem/mapper/LearningProblemMapper.java`
- `backend/src/main/java/com/gkstudy/learningproblem/model/LearningProblem.java`
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

- knowledge_point 新增 improvement_potential、transfer_value，用于集中计算提升收益和迁移价值。
- daily_plan_item 新增 learning_problem_id、knowledge_point_id、purpose、reason，保证计划任务可追溯。
- 初始化年均增长率、逻辑判断、定义判断、排列组合的考试重要度、提升收益和迁移价值验收数据。

schema.sql 是否已同步：

YES

init_data.sql 是否变化：

YES

upgrade.sql 当前状态：增量已在现有数据库执行，已同步 schema.sql，并在独立空库完成基线验证；文件已清理为说明注释。

---

## 5. 接口变化

新增接口：

- `GET /api/learning-problems/core`：返回当前用户最多 3 个核心问题。
- `GET /api/plan/today`：返回当天已有计划；不存在时生成默认 45 分钟计划。
- `POST /api/plan/generate`：按 20、45、60、90 分钟显式重建当天计划。

修改接口：

- 无。

---

## 6. 编译与测试

### 编译

命令：设置 JDK 11 后执行 `mvn clean test`

结果：

PASS

### 自动测试

命令：`mvn clean test`

通过：41

失败：0

结果：

PASS

覆盖：问题优先级排序、最弱不一定最高优先、VERIFYING 验证任务、低置信度 OBSERVING 保护、RESOLVED 排除、核心问题上限、20/45/60/90 分钟计划、今日计划幂等、显式重建、问题解决后候补补位、generation_reason，以及既有题库、答题、能力和 LearningProblem 回归测试。

---

## 7. 启动验证

Spring Boot：

PASS

数据库连接：

PASS

关键启动日志：最终代码连接独立空库后 `Started GkStudyApplication in 3.437 seconds`；实际请求能力与核心问题接口后 `HikariPool-1 - Start completed`。Bean、MyBatis 映射及 SQL 均无启动或执行错误。

---

## 8. 实际业务验证

本次实际执行的业务场景：

1. 准备年均增长率 MASTERY/CONFIRMED、逻辑判断 STABILITY/VERIFYING、排列组合 MASTERY/CONFIRMED、定义判断低置信度 OBSERVING，以及已解决问题和保持知识点。
2. 查询核心问题并生成 45 分钟计划，再重复查询当天计划。
3. 显式生成 20 分钟和 90 分钟计划，检查任务类型、目的、时长、顺序、关联问题和原因。
4. 将年均增长率问题改为 RESOLVED，并模拟下一天生成计划，检查候补补位。

实际结果：核心问题依次为逻辑判断 83.25、年均增长率 81.96、排列组合 64.15；年均增长率优先于能力更弱但收益较低的排列组合。低置信度 OBSERVING 未抢占首位，RESOLVED 未进入核心训练。

---

## 9. 核心链路验证

涉及核心学习闭环：

YES

若 YES：

```text
题目
→ 答题
→ AnswerRecord
→ Ability
→ LearningProblem
→ DailyPlan
```

验证结果：

PASS

具体数据：

* 答题记录：沿用第二至第四阶段已验证的真实 AnswerRecord，不修改原始学习数据。
* 能力变化：DailyPlan 读取 AbilityProfile 的能力状态与置信度；普通答题更新能力后不重建当天计划。
* 学习问题变化：VERIFYING、CONFIRMED、OBSERVING、RESOLVED 均参与实际排序与过滤验证。
* 计划变化：45 分钟计划为 3 项各 15 分钟，逻辑判断 VALIDATION、年均增长率 TRAINING、排列组合 TRAINING；重复 GET 返回同一计划。20 分钟仅保留最高收益的逻辑判断验证任务。年均增长率 RESOLVED 后，定义判断候补补位。90 分钟计划为 23/23/22/22 分钟，并包含保持训练 REVIEW。

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

说明：未新增生产依赖；优先级和计划算法分别集中在 ProblemPriorityEngine、DailyPlanEngine，沿用 Spring Boot 单体、MyBatis 和 MySQL。

---

## 12. 本次发现并修复的问题

1. 核心问题 GET 查询最初会顺带更新 priority_score；已拆分为只读查询与计划生成时持久化，避免只读接口产生写副作用。
2. Maven 首次启动在受限环境中无法写入工作区外的本地依赖缓存；授权现有缓存写入后使用同一命令复验，启动成功。
3. 空库核对脚本最初使用了错误的展示列名 exam_importance；按 schema 实际字段 importance 重查，结构和初始化值正确。

---

## 13. 尚未解决的问题

1. 现有本地 gk_study_helper 中保留第四、第五阶段的验收用户和测试数据；不影响功能、空库或自动测试验收。

---

## 14. 当前阻塞

无。

---

## 15. progress.md 更新

本次新增完成项：

* [x] ProblemPriorityEngine 与最多 3 个核心问题
* [x] VERIFYING、OBSERVING、RESOLVED 优先级规则
* [x] 20/45/60/90 分钟 DailyPlan 和可追溯任务
* [x] 当日计划幂等、显式重建和候补补位
* [x] 自动测试、真实业务、空库和启动验收

仍未完成：

* [ ] 小程序和管理后台页面（不在本阶段范围）
* [ ] AI、爬虫、申论、政治阅读、模考（不在本阶段范围）

---

## 16. 是否建议进入下一步

YES

原因：系统已能根据真实能力和学习问题稳定选出核心问题，并生成可解释、可追溯且当天稳定的学习计划。

---

## 17. 最终验收结论

PASS
