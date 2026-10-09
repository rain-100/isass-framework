# Agent 工作说明

- Java 代码在类名没有冲突时，必须通过 `import` 引入类型并使用简单类名，禁止直接使用全限定类名；仅在同名类型冲突、确需消歧时允许使用全限定类名。此规则同样适用于测试代码。
- 框架自身的 JSON 读写与对象转换统一调用 `JsonUtil`，不得在调用方创建 `ObjectMapper`；需把 mapper 对象交给第三方 API 时，使用 `JsonUtil` 提供的共享实例。参见 [公共模块](isass-core-common/README.md)。

## CodeGraph

- 父目录中的 `isass/.codegraph/` 是本项目所用的工作区索引。定位或理解代码时，在使用 `rg`、`find` 或大范围读取文件之前，必须先使用 `codegraph_explore`（或 `codegraph explore`）。
- 编辑前必须阅读当前返回的源代码；只有 CodeGraph 无法回答的后续文本搜索才使用 `rg`。

## ChangeLog

- 任何影响代码、配置、依赖、生成产物、数据库迁移行为或面向用户文档的变更，都必须记录到 `docs/60.changelog/ChangeLog4.x.md`。
- 按能力合并记录，不逐方法、逐重命名累积；ChangeLog 和 roadmap 各不超过 2000 字符。
- ChangeLog 必须与实现处于同一个变更集中更新，不得作为后续独立补充。

## 构建验证

- 修改框架后，必须在仓库根目录执行 `mvn install`，以便下游项目可以依赖最新的本地框架构建。
- 如果依赖解析需要使用 Maven Central 而不是已配置的镜像，请在最终回复中说明使用的命令或 settings。

## 框架约定

- 框架稳定前，docs 只保留 `60.changelog/ChangeLog4.x.md` 和 `70.roadmap/roadmap.md`，不新增长篇设计、计划或使用文档。
- 各模块 `README.md` 简述职责、入口和关键边界；具体行为以实现和有效测试为准。AGENTS 只保留执行摘要与项目专有约束，不复制模块说明。
- `isass-nocode-generator` 负责生成 model、Criteria、mapper 及契约约定。应修改 `isass-nocode-generator/src/main/resources/templates/` 下的模板并重新生成使用方；不要把手工维护生成产物作为长期修复方案。
- 业务实体关系以 Java `EntityRelationDefinition` 清单为唯一手写来源，生成器输出类型化成员及共享关系元数据；声明和升级方式见生成器使用文档，不再从 DDL 关联标记生成关系。
- 实体领域归属、枚举、Java 类型覆盖和租户隔离开关由 `EntityModelDefinition` / `EntityFieldDefinition` 声明；DDL 保留物理结构与可读说明。两类声明各司其职，不扩展关系类承担模型配置。
- 面向应用的字段和 Criteria 使用 Java camelCase 属性及 lambda 引用。数据库列名属于 ORM 的职责范围；只有自动属性到列的映射确实存在歧义时，才添加显式元数据。
- NoCode 支持高级响应投影、关联查询与关联写入。其公开 Query 参数应保持 camelCase，新增行为简要更新所属模块 README。
- 共享 Redis key 使用 `<microservice>:<domain>:<feature>[:<id>]`。避免使用框架全局 key 前缀，也不要清理无关 key。
- 框架配置使用 `isass.<module>.<feature>...` 层级。新增可复用配置时，不得引入一次性的根前缀。
- 不要在框架模块中放置特定服务的初始化数据或业务规则。

## 统一规则入口

- 构建、DDD 与测试原则：`README.md`
- 数据库与迁移：`isass-database-core/README.md`
- 模型与关系生成：`isass-nocode-generator/README.md`
- NoCode、生命周期、关联与初始化：`isass-nocode-core/README.md`
- 查询与 ORM：`isass-database-mybatisplus/README.md`
- 服务入口与调用：`isass-entrypoint-registry/README.md`
- 内部微服务 HMAC：`isass-security-springsecurity/README.md`

修改前阅读所属模块 README 和当前源码；实现变化时简要更新 README 和 ChangeLog。测试应覆盖安全、隔离、事务、并发、协议或历史缺陷，不维护全量入口/类名快照。业务微服务只引用入口并记录服务专有差异。
