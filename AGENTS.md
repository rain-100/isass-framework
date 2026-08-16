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
- `isass-nocode-generator` 负责生成 model、Criteria、mapper 及契约约定。应修改 `isass-nocode-generator/src/main/resources/templates/` 下的模板并重新生成使用方；不要把手工维护生成产物作为长期修复方案。
- 面向应用的字段和 Criteria 使用 Java camelCase 属性及 lambda 引用。数据库列名属于 ORM 的职责范围；只有自动属性到列的映射确实存在歧义时，才添加显式元数据。
- NoCode 支持高级响应投影和关联查询。其公开 Query 参数应保持 camelCase，任何新增行为都要记录到 `docs/usage/nocode/`。
- 共享 Redis key 使用 `<microservice>:<domain>:<feature>[:<id>]`。避免使用框架全局 key 前缀，也不要清理无关 key。
- 框架配置使用 `isass.<module>.<feature>...` 层级。新增可复用配置时，不得引入一次性的根前缀。
- 不要在框架模块中放置特定服务的初始化数据或业务规则。

## 源文件读取优化

源文件开头可能包含版权或许可证声明。

当任务与许可证无关时：

- 将开头的版权或许可证注释视为法律样板内容，避免在上下文中反复加载相同文本。
- 优先从第一个有实际意义的源代码声明处开始定向读取，再按需扩大范围。
- 不要假设固定行数可以覆盖文件头或实现内容。
- 除非任务明确要求，否则绝不能删除、修改、移动或格式化法律声明。

处理许可证、版权、再分发、来源、第三方代码或合规工作时，必须完整阅读源文件头、仓库许可证，以及所有 NOTICE 或第三方许可证文件。
