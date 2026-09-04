# 数据库表设计规范

## 结构变更边界

- 数据库结构统一由 Liquibase 维护。Liquibase 只维护表、字段、索引、约束和注释，不写业务数据，也不迁移历史业务数据。
- 静态初始化数据使用 `init/**/*.json` 或明确的 Java 初始化 DSL；已有环境的数据修复使用经确认的一次性 SQL 或专项程序。
- 禁止创建数据库外键及数据库级联约束，包括 `FOREIGN KEY`、`REFERENCES`、`ON DELETE CASCADE` 和 `ON DELETE RESTRICT`。引用完整性由应用校验、唯一索引、同一事务内的写入顺序、幂等同步和巡检保证。
- 唯一业务条件必须建立唯一索引，不能只依赖“先查询、后新增”。高频游标查询的附加过滤条件，原则上建立“等值过滤字段在前、`id` 在后”的联合索引。

## 表命名

所有业务表必须使用以下严格命名规则：

```text
{service}_{context}_{entity}
```

| 段 | 含义 | 示例 |
| --- | --- | --- |
| `service` | 服务标识 | `bsp`、`trade` |
| `context` | DDD 限界上下文标识 | `auth`、`attachment`、`order` |
| `entity` | 当前上下文内的实体标识 | `user`、`file`、`order_item` |

示例：

```text
bsp_auth_user
bsp_auth_role
bsp_attachment_file
bsp_attachment_icon
bsp_log_request_log
trade_order_order
trade_order_order_item
```

该规则没有省略例外：即使 `service` 与 `context` 名称相同，也必须完整保留三段，例如
`order_order_order`。这保证表名的归属和含义始终清晰、无歧义。

## 模型与持久化分层

- 生成的持久化领域模型放在 API 模块的 `domain.model.entity`，Repository 接口放在 service 模块的 `domain.repository`，Mapper 和 Repository 实现放在 `infrastructure`。
- `application.model` 可按需放置应用服务专属或跨聚合编排使用的 `entity`、`criteria`、`vo`、`dto`、`req`、`resp`、`enums` 等模型；数据库生成的 ORM 持久化实体仍统一放在 `domain.model.entity`，不在应用层复制。
- 不生成或保留没有领域行为的 `XxxAgg extends Xxx` 空壳类。一个表对应的生成模型本身就是该聚合的模型；组合多个聚合时由应用层模型或应用服务编排。
- 生成模型不得使用 ORM 注解。ORM 字段映射、非持久化关联属性排除和分页对象转换由基础设施层处理。
- 生成模型、Criteria、Repository、Mapper 和标准 CRUD Service 由 Liquibase DDL 与 NoCode 生成器维护。结构问题应修改 DDL、生成器解析逻辑或模板后重新生成，不能把手工修改生成文件作为长期方案。
- 通用聚合写入只保证同一微服务、同一数据源中的本地事务。跨聚合复杂用例由 `IApplicationService` 编排；跨微服务或跨数据源关系使用显式业务接口、消息、幂等和补偿。

## 内置列约定

生成器按列名启用统一能力：

| 列 | 生成能力 |
| --- | --- |
| 主键 | ID 实体 |
| `parent_id` 且与主键同类型 | `IParentIdEntity`、`parent` 和 `children` |
| `delete_flag` | 逻辑删除 |
| `tenant_id` | 默认启用租户实体，可由字段标记关闭 |
| 完整审计字段组 | 追踪实体 |
| `version` | 乐观锁 |

## Liquibase 注释 DSL

注释首先是面向人的说明；只有本节列出的方括号标记具有生成语义。新增标记必须同步修改生成器解析逻辑、有效测试和本文档。

### 表级关联

普通关联只写在表级 `remarks` 中，是从当前实体到目标实体的单向声明：

```text
[关联表-单体-目标实体; property=属性名; localKey=当前属性; targetKey=目标属性; cascadeDelete=false]
[关联表-列表-目标实体; property=属性名; localKey=当前属性; targetKey=目标属性; cascadeDelete=false]
```

- `目标实体` 使用去掉公共表名前缀后的 Java 实体名，例如 `SampleImage`，不是数据库表名。
- `property`、`localKey`、`targetKey` 和 `cascadeDelete` 均可省略。单体默认推断为 `targetId -> id`，列表默认推断为 `id -> currentEntityId`；属性名按目标实体 lowerCamel 及统一复数规则推断。
- `localKey` 与 `targetKey` 必须同时省略或同时填写。推断有歧义、字段类型不兼容或命名不符合约定时必须显式填写，生成器不能猜测后继续。
- `[关联表-列表-SampleImage; cascadeDelete=true]` 等价于 `property=sampleImages; localKey=id; targetKey=sampleGroupId; cascadeDelete=true`，并生成非持久化 `Collection<SampleImage> sampleImages`。
- `cascadeDelete` 默认是 `false`，只表示删除当前实体时沿声明方向删除目标记录。不得根据“主表、子表、中间表”等名称推断，也不得沿未声明的反向关系删除。
- 声明不会在目标实体生成反向属性，也不存在 `reverseProperty`。需要反向关系时，必须在目标表自己的表级注释中另行声明。

### 树形关系

`parent_id` 与主键类型一致时自动生成树形模型，不使用“添加父节点”或“添加子节点”标记。只有需要递归删除子孙节点时才在表级声明：

```text
[树结构-cascadeDelete=true]
```

树形级联默认是 `false`。删除子节点永远不能删除父节点；保存必须阻止把节点设为自己的父节点或子孙节点，删除必须检测循环和异常深度，失败时回滚。

### 字段级标记

| 标记 | 示例 | 生成效果 |
| --- | --- | --- |
| `[枚举--...]` | `[枚举--0:DRAFT:草稿;1:PUBLISHED:已发布]` | 按 `数据库值:Java枚举名:显示名` 生成字段专用枚举及序列化/解析代码 |
| `[javaType--...]` | `[javaType--List<String>]`、`[javaType--Map<String, Object>]` | 覆盖数据库类型的默认 Java 映射，常用于 JSON 列 |
| `[tenantEntity--false]` | 写在 `tenant_id` 字段备注中 | 把该列作为普通业务字段，不实现 `ITenantEntity`，也不应用租户 Criteria 特殊规则 |

Liquibase XML 属性中的泛型尖括号必须转义为 `&lt;` 和 `&gt;`。枚举数据库值必须与列类型、默认值和已有数据一致；只修改显示名时不得改变稳定的 Java 枚举名或数据库值。未列入本节的普通方括号文本没有生成语义。
