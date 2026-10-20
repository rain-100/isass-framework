# isass-database-core

数据库与 Repository 契约。提供 ORM 无关 Repository、表前缀注册及 Liquibase 命名规则；Spring Boot 下的 Liquibase 装配位于 `isass-adapter-springboot`。

- 每个服务使用一个 Liquibase master，资源路径、迁移历史表和锁表名称由 `LiquibaseServiceNaming` 按服务隔离；开发期结构调整与发布期增量迁移须明确区分。
- 受保护写入先在事务内锁定当前记录，再判范围和字段权限；未实现锁/条件授权的适配器必须明确拒绝。

[返回模块目录](../README.md)

## 启动自动建库

Spring Boot 在创建数据源和执行 Liquibase 前，通过 `DatabaseInitializerSpringStarter` 调用本模块的 `DatabaseInitializerManager`。已配置主数据源且数据库不存在时，由框架尝试自动创建数据库，之后由 Liquibase 初始化表结构；无需先手工创建空库。

- 需引入数据库模块并配置完整的 `spring.datasource.dynamic.datasource.master.url`、`username`、`password`；`spring.datasource.autoCreate=false` 会关闭该入口，未设置时默认启用。连接账号须具备目标数据库的建库权限。
- 启动前核对实际生效的主数据源地址、数据库名及运行环境；已能连接的数据库直接跳过建库，不删除或覆盖已有库。
- 首次初始化或经单独授权删库后，通过 IDEA Debug 启动服务触发上述流程，核实目标库、Liquibase 结果和表结构后再运行代码生成器；生成器本身不负责建库。
- SQL 工具不允许手工建库不代表服务自动建库不可用。自动建库失败时，检查启动日志、数据源配置、网络和建库权限；不得清除迁移校验和或手工补表绕过初始化，自动建库也不替代删库授权。

# 数据库表设计规范

## 结构变更边界

- 数据库结构统一由 Liquibase 维护。本框架约定 changelog 只维护表、字段、索引、约束和注释，不写业务数据，也不迁移历史业务数据；这不是 Liquibase 产品能力的限制。
- 静态初始化数据使用 `init/**/*.json` 或明确的 Java 初始化 DSL；已有环境的数据修复使用经确认的一次性 SQL 或专项程序。
- 禁止创建数据库外键及数据库级联约束，包括 `FOREIGN KEY`、`REFERENCES`、`ON DELETE CASCADE` 和 `ON DELETE RESTRICT`。引用完整性由应用校验、唯一索引、同一事务内的写入顺序、幂等同步和巡检保证。
- 唯一业务条件建议建立唯一索引，不能只依赖“先查询、后新增”。高频游标查询的附加过滤条件，原则上建立“等值过滤字段在前、`id` 在后”的联合索引。
- `*-init.xml` 中每张表的 `createIndex` 必须紧跟对应的 `createTable`，并位于该表的其他 change 以及下一张 `createTable` 之前；禁止把多个表的索引集中堆放在 changeSet 末尾。`createIndex` 是与 `createTable` 同级的 Liquibase change，不能嵌套到 `createTable` 内部。

## 表命名

所有业务表必须使用以下严格命名规则：

```text
{service}_{context}_{entity}
```

| 段 | 含义 | 示例 |
| --- | --- | --- |
| `service` | 服务标识 | `bsp`、`trade` |
| `context` | DDD 限界上下文标识 | `auth`、`attachment`、`order` |
| `entity` | 限界上下文内的实体标识 | `user`、`role_permission`、`order_item` |

示例：

```text
bsp_auth_user
bsp_auth_role
bsp_file_att_file
bsp_file_icon
bsp_log_request_log
trade_order_order
trade_order_order_item
```

该规则没有省略例外：即使 `service` 与 `context` 名称相同，也必须完整保留三段。实体名称可以包含下划线，但还是建议尽量简化，名称不要太长。
例如 `trade_order_order_item`。表名只表达稳定的服务和限界上下文所有权；

## 模型与持久化分层

- 以下路径均省略服务包前缀。生成的持久化领域模型放在 API 模块的 `{context}/domain/{domain}[/{subdomain}]/domain/model/entity`，Repository 接口放在 service 模块的 `{context}/domain/{domain}[/{subdomain}]/domain/repository`，Mapper 和 Repository 实现放在对应领域或子域的 `infrastructure`。
- 应用模型按所属业务能力放在 `{context}/domain/{domain}[/{subdomain}]/application/{capability}/model`，同一领域内的跨子域能力放在 `{context}/domain/{domain}/application/{capability}/model`，跨领域能力放在 `{context}/application/{capability}/model`；不得把上下文根的 `application.model` 用作公共模型仓库。数据库生成的 ORM 持久化实体仍统一放在 `domain.model.entity`，不在应用层复制。
- 生成模型不得使用 ORM 注解。ORM 字段映射、非持久化关联属性和分页对象转换由基础设施层处理。
- 生成模型、Criteria、Repository、Mapper 和标准 CRUD Service 骨架以 Liquibase 管理的数据库结构、Java 模型/关系声明及 NoCode 生成模板为依据；标准 CRUD Service 固定保留在 `{context}/application/{domain}[/{subdomain}]/service`，领域模型、Repository 和持久化实现仍位于 `{context}/domain/{domain}[/{subdomain}]`。结构问题应修改 DDL、Java 声明或生成器解析逻辑/模板后重新生成，不能把手工修改生成文件作为长期方案；已有 Service 的手写业务扩展默认不覆盖。
- 通用聚合写入只保证同一微服务、同一数据源中的本地事务。跨聚合复杂用例由应用服务编排；生成的
  `I{Entity}Service` 本身就是应用服务，当用例的业务主体属于该实体或聚合时可以直接承担编排，不需要因为
  调用了其他聚合而另建 Service。跨微服务或跨数据源关系使用显式业务接口、消息、幂等和补偿。

## 内置列约定

生成器根据数据库主键元数据和约定列名启用统一能力：

| 列 | 生成能力 |
| --- | --- |
| 数据库元数据标记的主键 | `IIdEntity` 及 ID Criteria；无主键表仅生成 `IEntity` 与普通 Criteria，不虚构 `id` |
| `parent_id` 且生成的 Java 类型与主键一致 | `IParentIdEntity`、`parent` 和 `children`，要求实体有主键 |
| `delete_flag` | 逻辑删除 |
| `tenant_id` | 默认启用租户实体，可由 `EntityModelDefinition.withoutTenantIsolation()` 关闭 |
| `create_user_id`、`create_user_name`、`create_time`、`modify_user_id`、`modify_user_name`、`modify_time` 中任一列 | `ITraceEntity`，不要求完整审计字段组 |
| `version` | 乐观锁 |

## 模型语义与 DDL 的边界

DDL 是物理表、列类型、默认值、非空、索引及中文说明的唯一来源。领域包路径、实体关系、业务枚举、JSON 的 Java 类型与租户开关属于应用语义，由 Java 声明生成；不再增加 DDL 注释 DSL。解析器仅兼容清理展示文本中的旧关联、树级联标记，不据此生成关系或模型配置。

### 领域归属

每个参与生成的实体必须有唯一的 `EntityModelDefinition`，表名继续决定稳定的服务和上下文：

```java
EntityModelDefinition.of("User", "identity");
EntityModelDefinition.of("Permission", "authorization", "permission");
```

例如 `bsp_auth_user` 的备注为 `用户(平台账户)`，使用上述 User 声明，生成到
`auth/domain/identity/domain/model/entity/User.java`。`domain` 必须是一个以小写字母开头、只包含小写字母和数字的 Java
包名段。声明缺失、重复或格式非法时生成器失败，不回退到默认领域。

`subdomain` 是可选的一级子域，同样必须是小写 Java 包名段。例如
`EntityModelDefinition.of("Role", "authorization", "role")` 生成到
`auth/domain/authorization/role/domain/model/entity/Role.java`。不支持多层子域；
`application`、`domain`、`infrastructure`、`interfaces` 等技术层名称不能作为子域名。未声明子域时继续使用原领域目录。

实体需要调整领域或子域时，修改 Java 声明并重新生成。数据库表从一个限界上下文迁移到
另一个限界上下文需要变更表名，重新生成代码，但需要注意不能随意覆盖该实体手写的代码。

### 业务实体关系

业务关系的唯一手写来源是传给生成器的 Java `EntityRelationDefinition` 清单，不再写入 DDL 备注。
表及列由 DDL 维护；领域归属和枚举使用独立模型声明，关系成员及 `EntityAssociation` 由关系声明生成。
NoCode 查询、关联写入、ORM 属性识别和数据权限等组件复用生成的关系元数据，禁止各自维护关系键。

声明需明确源实体、目标实体、关系属性、双方的 Java 关联键和单个/集合类型；级联默认关闭。`one(...)` 生成单个实体；多项关联按 `many(...)` 或 `list(...)` 声明分别生成 `Collection<T>` 或 `List<T>`，不得丢失声明的集合类型。`List` 不隐式添加 SQL 排序，业务顺序需在查询中明确指定。
生成前检查未知实体、重复或覆盖字段的成员、缺失键和不兼容键类型。关联属性只生成在声明的源实体上；另一端如需关联属性，必须另行显式声明。
不提供 `writable` / `readOnly()` 配置；能否嵌套新增、修改或删除，由关联对象对应操作的权限及业务校验决定，不能因声明关系而绕过授权。
跨微服务或数据源关系不因元数据声明而获得通用关联写入能力。

关系声明边界见[模型与关系生成](../isass-nocode-generator/README.md)，运行时权限与事务行为见[NoCode 模块](../isass-nocode-core/README.md)。

### 树形关系

实体有主键且 `parent_id` 生成的 Java 类型与主键一致时，自动生成树形模型：`parent` 为单个实体，`children` 为 `List<T>`。该内置识别依赖 `parent_id`，不会把任意父级引用字段自动识别为树。需要递归删除子孙节点时，通过生成器的
`treeCascadeDeleteEntities` 显式指定实体，不再使用 DDL 树标记；非层级实体配置该能力时生成失败。
树形级联默认关闭。删除子节点永远不能删除父节点；保存必须阻止把节点设为自己的父节点或子孙节点，
删除必须检测循环和异常深度，失败时回滚。

### 字段生成语义

| Java 声明 | 示例 | 生成效果 |
| --- | --- | --- |
| `EntityFieldDefinition.enumeration` | `enumeration("status", new EnumValue(0, "DRAFT", "草稿"))` | 生成内置枚举，显式维护 `Integer` 编码、Java 枚举名及显示名，不使用枚举序号 |
| `EntityFieldDefinition.type` | `type("members", "Map<String, Object>")` | 覆盖默认 Java 类型，常用于 JSON 列 |
| `EntityModelDefinition.withoutTenantIsolation` | `of("TenantApp", "app").withoutTenantIsolation()` | tenantId 为业务字段，不实现 `ITenantEntity` 或租户特殊 Criteria |

JSON 列默认映射为 Jackson 3 的 `JsonNode`；需要 `List<String>`、`Map<String, Object>` 等类型时，通过 `EntityFieldDefinition.type` 显式声明。Java 类型覆盖不改变数据库列类型。

枚举数据库值必须与列类型、默认值和已有数据一致；只修改显示名时不得改变稳定的 Java 枚举名或数据库值。属性改名需要同步模型声明与既有授权引用；数据库列名不复制到这些声明中。模型与字段声明边界见[生成器模块](../isass-nocode-generator/README.md)。
