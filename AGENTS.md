# Agent 工作说明

## CodeGraph

- 父目录中的 `isass/.codegraph/` 是本项目所用的工作区索引。定位或理解代码时，在使用 `rg`、`find` 或大范围读取文件之前，必须先使用 `codegraph_explore`（或 `codegraph explore`）。
- 编辑前必须阅读当前返回的源代码；只有 CodeGraph 无法回答的后续文本搜索才使用 `rg`。

## ChangeLog

- 任何影响代码、配置、依赖、生成产物、数据库迁移行为或面向用户文档的变更，都必须记录到 `docs/60.changelog/ChangeLog4.x.md`。
- 条目应简洁，并与现有 ChangeLog 风格保持一致。
- ChangeLog 必须与实现处于同一个变更集中更新，不得作为后续独立补充。

## 构建验证

- 修改框架后，必须在仓库根目录执行 `mvn install`，以便下游项目可以依赖最新的本地框架构建。
- 如果依赖解析需要使用 Maven Central 而不是已配置的镜像，请在最终回复中说明使用的命令或 settings。

## 框架约定

- 框架使用文档放在 `docs/usage/<topic>/`；架构与设计记录放在 `docs/design/`；开发计划与规范放在 `docs/superpowers/`。
- 共享机制的完整规则只在框架 `docs/usage/` 维护；框架和业务项目的 `AGENTS.md` 只保留执行摘要、项目专有规则和文档链接。规则归属见 `docs/usage/agent/rule-ownership.md`。
- `isass-nocode-generator` 负责生成 model、Criteria、mapper 及契约约定。应修改 `isass-nocode-generator/src/main/resources/templates/` 下的模板并重新生成使用方；不要把手工维护生成产物作为长期修复方案。
- 面向应用的字段和 Criteria 使用 Java camelCase 属性及 lambda 引用。数据库列名属于 ORM 的职责范围；只有自动属性到列的映射确实存在歧义时，才添加显式元数据。
- NoCode 支持高级响应投影和关联查询。其公开 Query 参数应保持 camelCase，任何新增行为都要记录到 `docs/usage/nocode/`。
- 共享 Redis key 使用 `<microservice>:<domain>:<feature>[:<id>]`。避免使用框架全局 key 前缀，也不要清理无关 key。
- 框架配置使用 `isass.<module>.<feature>...` 层级。新增可复用配置时，不得引入一次性的根前缀。
- 不要在框架模块中放置特定服务的初始化数据或业务规则。

## 统一规则入口

- 服务模块与 DDD 分层：`docs/usage/architecture/service-ddd.md`
- 数据库、Liquibase 注释 DSL 与生成模型：`docs/usage/database/table-design.md`
- Liquibase 运行和变更边界：`docs/usage/database/liquibase-multi-mode-migration.md`
- NoCode 生成器：`docs/usage/database/nocode-mybatis-plus-generator.md`
- NoCode 八个正式入口、统一执行与生命周期：`docs/usage/nocode/crud-lifecycle.md`
- 关联查询与关联写入：`docs/usage/nocode/association-query.md`
- 初始化 JSON、导入导出与 ID 分配：`docs/usage/nocode/initialization-data.md`
- Entrypoint 服务调用：`docs/usage/nocode/service-client.md`
- 内部微服务 HMAC：`docs/usage/security/internal-service-hmac.md`
- 业务价值测试：`docs/usage/testing/business-value-tests.md`

修改上述共享机制前必须阅读对应文档；实现变化时同步更新文档和 ChangeLog。业务微服务不得复制这些规则正文，只引用对应入口并记录服务专有差异。
