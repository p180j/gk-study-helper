# gk-study-helper

个人考公 AI 学习助手。第一阶段实现题目、练习、能力、学习问题与每日计划的核心闭环。

## 本地启动

1. 使用 Java 11、Maven 和 MySQL 8。
2. 依次执行 `sql/schema.sql`、`sql/init_data.sql`。
3. 通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 覆盖数据库配置。
4. 执行 `mvn -pl backend spring-boot:run`。
