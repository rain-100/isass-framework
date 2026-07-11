# isass-framework-v4 V2 架构与完整调用链分析

> 基于 `isass-framework-v4` 框架 + `isass-service-attachment` 微服务实现，全面剖析 V2 分层架构、接口体系、调用链路与关键实现细节。

---

## 一、V2 架构总览

### 1.1 设计目标

V2 架构是 isass-framework-v4 提供的"无代码（NoCode）"快速 CRUD 开发范式。开发者只需**建好数据库表**，运行代码生成器（`V2MybatisPlusGenerator`）即可一键生成 Entity、Criteria、Repository、Service、Controller 等全部代码，框架自动暴露 40+ 标准 REST API 端点，覆盖增删改查、分页、条件查询、存在性判断等全部场景。

### 1.2 分层架构

```
┌────────────────────────────────────────────────────────────┐
│  外部 HTTP 请求                                              │
│  GET/POST/PUT/DELETE /{service}/v2/...                     │
└──────────────────────┬─────────────────────────────────────┘
                       │
┌──────────────────────▼─────────────────────────────────────┐
│  Layer 1: Controller (IV2Controller)        [web-springmvc]│
│  - Spring @RestController 注解层                              │
│  - 映射 40+ HTTP 端点到统一路由 `/v2/*`                        │
│  - 直接注入 IV2LocalService 实现                               │
└──────────────────────┬─────────────────────────────────────┘
                       │ getService().xxx()
┌──────────────────────▼─────────────────────────────────────┐
│  Layer 2: ServiceManager (IV2ServiceManager) [nocode-core] │
│  - @Primary @Service，组合模式/路由层                           │
│  - 自动注入 List<S> services 列表                              │
│  - 通过 V2ServiceManagerUtil 遍历 services 执行操作             │
│  - 优先级策略：LocalService > FeignService > 其他               │
└──────────────────────┬─────────────────────────────────────┘
                       │ V2ServiceManagerUtil.applyUntilNotNull()
┌──────────────────────▼─────────────────────────────────────┐
│  Layer 3: LocalService (IV2LocalService)     [nocode-core] │
│  - 业务逻辑层，继承 IV2Service                                   │
│  - 默认实现委托给 IV2Repository                                  │
│  - 可在此层添加自定义业务方法                                     │
└──────────────────────┬─────────────────────────────────────┘
                       │ getRepository().xxx()
┌──────────────────────▼─────────────────────────────────────┐
│  Layer 4: Repository (IV2Repository)       [nocode-core]   │
│  - 数据访问接口，默认为 UnsupportedOperationException           │
│  - 定义所有 CRUD 方法签名                                       │
└──────────────────────┬─────────────────────────────────────┘
                       │ V2WrapperUtil 转换 Criteria -> Wrapper
┌──────────────────────▼─────────────────────────────────────┐
│  Layer 5: V2MybatisPlusRepository  [database-mybatisplus]  │
│  - extends ServiceImpl<M, EDB> implements IV2Repository<E, C>│
│  - 使用 MyBatis-Plus 实现真正的 SQL 操作                         │
│  - Entity <-> EDB (DB Entity) 双向转换 (V2DbEntityConvert)      │
│  - 敏感字段自动过滤 (SensitiveDataProperty)                     │
└────────────────────────────────────────────────────────────┘
```

### 1.3 核心模块职责

> **nocode 设计原则**：`isass-nocode-core` 是 nocode 的整合层，同时依赖 Spring Web MVC 和 MyBatis-Plus，将两者封装为无代码编程体验。业务微服务只需依赖 `isass-nocode-core` 即可获得完整的 nocode 能力（Controller + Service + Repository + ORM），无需单独依赖 `isass-web-springmvc` 或 `isass-database-mybatisplus`。

| 模块 | 包路径 | 职责 |
|------|--------|------|
| `isass-nocode-core` | `vip.isass.framework.nocode.v2` | **nocode 整合层**：定义 V2 全部接口和实现（Service/ServiceManager/LocalService/Repository/Entity/Criteria），同时包含 Spring MVC Controller 和 MyBatis-Plus ORM 实现，业务只需依赖此模块 |
| `isass-web-springmvc` | `vip.isass.framework.web` | 提供底层 Spring MVC 能力（WebMvcConfigurer、拦截器、异常处理等），作为 nocode 的 MVC 基础设施 |
| `isass-database-mybatisplus` | `vip.isass.framework.database.mybatisplus` | 提供底层 MyBatis-Plus 能力（Mapper、TypeHandler、配置等），作为 nocode 的 ORM 基础设施 |
| `isass-service-attachment` | `vip.isass.attachment` | 附件微服务的 V2 完整实现示例 |

---

---

## 二、代码生成器（V2MybatisPlusGenerator）

### 2.1 概述

**位置：** `isass-framework-v4/isass-database-core/src/main/java/vip/isass/framework/database/core/generator/V2MybatisPlusGenerator.java`

V2MybatisPlusGenerator 是基于 MyBatis-Plus `FastAutoGenerator` 的代码生成引擎，通过 **Freemarker 模板**从数据库表结构生成完整的分层代码。核心逻辑是：**禁用 MyBatis-Plus 内置的所有模板（entity/controller/service/mapper），全部替换为自定义 Freemarker 模板**，确保生成的代码完全符合 isass V2 架构规范。

### 2.2 生成内容清单（每个表生成 10 个文件）

| # | 生成文件 | 输出包路径 | 模板文件 | 说明 |
|---|---------|-----------|---------|------|
| 1 | `V2{Entity}.java` | `api.model.entity` | `entity.java.ftl` | 业务实体（实现IV2Entity体系） |
| 2 | `V2{Entity}Criteria.java` | `api.model.criteria` | `criteria.java.ftl` | 查询条件（全字段链式构建器） |
| 3 | `V2{Entity}Db.java` | `db.model` | `entityDb.java.ftl` | 数据库实体（带@TableName/@TableId等MP注解） |
| 4 | `V2{Entity}Mapper.java` | `db.mapper` | `mapper.java.ftl` | MyBatis-Plus Mapper接口 |
| 5 | `V2{Entity}Mapper.xml` | `db.mapper.xml` | `mapper.xml.ftl` | MyBatis XML映射文件 |
| 6 | `V2{Entity}Repository.java` | `db.repository` | `repository.java.ftl` | 数据仓库（继承V2MybatisPlusRepository） |
| 7 | `IV2{Entity}Service.java` | `api.service` | `iSservice.java.ftl` | 服务接口（含URI常量 + 内嵌ServiceManager） |
| 8 | `V2{Entity}Service.java` | `service` | `localService.java.ftl` | 本地服务实现（实现IV2LocalService） |
| 9 | `V2{Entity}FeignService.java` | `api.feign` | `feignService.java.ftl` | Feign远程调用接口 |
| 10 | `V2{Entity}Controller.java` | `controller` | `controller.java.ftl` | REST控制器（实现IV2Controller接口） |

### 2.3 配置元数据 MybatisPlusGeneratorMeta

**文件位置：** `isass-database-core/src/main/java/vip/isass/framework/database/core/generator/MybatisPlusGeneratorMeta.java`

```java
@Getter @Setter @Accessors(chain = true)
public class MybatisPlusGeneratorMeta {
    private DbType dbType;               // 数据库类型（MYSQL, DM, KINGBASE_ES 等）
    private String dataSourceUserName;    // 数据库用户名
    private String dataSourcePassword;    // 数据库密码
    private String dataSourceUrl;         // JDBC连接URL
    private String schemaName;            // 数据库Schema名
    private String outputDir;             // 代码输出目录
    private String moduleName;            // Maven模块名（如 attachment）
    private String packageName;           // 基础包名（如 vip.isass）
    private String[] tablePrefix;         // 表前缀（生成类名时自动去除，如 att_）
    private String[] includeTables;       // 需要包含的表名（支持正则）
    private String[] excludeTables;       // 需要排除的表名（支持正则）
    private String controllerPrefix;      // Controller URL前缀（如 /attachment-service）
}
```

### 2.4 使用示例

**参考文件：** `isass-service-attachment/isass-service-attachment-service/src/test/java/vip/isass/attachment/generator/AttachmentMybatisPlusGenerator.java`

```java
MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta()
    .setOutputDir("./target/generator")
    .setDbType(DbType.MYSQL)
    .setDataSourceUrl("jdbc:mysql://host:3306/attachment?...")
    .setDataSourceUserName("root")
    .setDataSourcePassword("password")
    .setTablePrefix(new String[]{ModuleInfo.TABLE_PREFIX})        // 表前缀 att_icon -> 类名 Icon
    .setPackageName(ModuleInfo.GROUP_ID)
    .setModuleName(ModuleInfo.SERVICE_NAME)
    .setControllerPrefix(ModuleInfo.SERVICE_URL_PREFIX)
    .setIncludeTables(new String[]{"att_icon", "att_icon_group"});

V2MybatisPlusGenerator.generate(meta);
```

**执行方式：** 在微服务的 `src/test/java` 下创建 Generator 类，运行 `main()` 方法，生成的代码输出到 `target/generator`，人工确认后拷贝到对应源码目录。

### 2.5 生成器核心处理流程

```
1. V2MybatisPlusGenerator.generate(meta)
   │
2. 构建 BeansWrapper → 获取框架接口的静态模型
   ├─ IV2IdEntity 静态模型（常量字段名/列名）
   ├─ IV2ParentIdEntity 静态模型
   ├─ IV2LogicDeleteEntity 静态模型
   ├─ IV2TenantEntity 静态模型
   ├─ IV2TraceEntity 静态模型
   └─ IV2VersionEntity 静态模型
   │
3. FastAutoGenerator.create(url, user, pwd)
   │
4. globalConfig: 设置 author, outputDir, 禁用打开目录, 日期类型 TIME_PACK
   │
5. dataSourceConfig: 设置 schema + TypeConvertHandler
   │
6. strategyConfig: 过滤表 + 禁用内置模板
   ├─ .enableSkipView()           — 跳过视图
   ├─ .enableCapitalMode()        — 大写命名模式
   ├─ .addTablePrefix(...)         — 去除表前缀
   ├─ .addInclude/addExclude(...) — 表过滤
   ├─ .controllerBuilder().disable()  — 禁用内置controller模板
   ├─ .serviceBuilder().disable()     — 禁用内置service模板
   ├─ .mapperBuilder().disable()      — 禁用内置mapper模板
   ├─ .entityBuilder().disable()      — 禁用内置entity模板
   ├─ .versionPropertyName("version") — 乐观锁字段名
   └─ .logicDeletePropertyName("deleteFlag") — 逻辑删除字段名
   │
7. packageConfig: 设置包名 parent + moduleName
   │
8. injectionConfig: 注入自定义变量到模板
   ├─ controllerPrefix, moduleName, package 等路径变量
   ├─ 框架接口静态模型（idEntity, traceEntity, versionEntity 等）
   └─ 10 个 CustomFile 定义:
       ├─ entity.java.ftl       → api/model/entity/V2*.java
       ├─ criteria.java.ftl     → api/model/criteria/V2*Criteria.java
       ├─ entityDb.java.ftl     → db/model/V2*Db.java
       ├─ mapper.java.ftl       → db/mapper/V2*Mapper.java
       ├─ mapper.xml.ftl        → db/mapper/xml/V2*Mapper.xml
       ├─ repository.java.ftl   → db/repository/V2*Repository.java
       ├─ iSservice.java.ftl    → api/service/IV2*Service.java
       ├─ localService.java.ftl → service/V2*Service.java
       ├─ feignService.java.ftl → api/feign/V2*FeignService.java
       └─ controller.java.ftl   → controller/V2*Controller.java
   │
9. .templateEngine(new FreemarkerTemplateEngine())
   .execute()  — 执行代码生成
```

### 2.6 TypeConvertHandler — 数据库类型映射

**文件位置：** `isass-database-core/src/main/java/vip/isass/framework/database/core/generator/TypeConvertHandler.java`

自定义类型转换处理器，覆盖 MyBatis-Plus 默认映射，支持额外类型：

| 数据库类型 | Java 类型 | 说明 |
|-----------|-----------|------|
| `smallint[]` | `Short[]` | PostgreSQL smallint 数组 |
| `tinyint` | `Integer` | MySQL tinyint → Integer（而非 Boolean） |
| `tinyint[] / integer[]` | `Integer[]` | 整数数组 |
| `bigint[]` | `Long[]` | 长整数数组 |
| `numeric...[]` | `BigDecimal[]` | 高精度数值数组 |
| `boolean[]` | `Boolean[]` | 布尔数组 |
| `character...[] / text[]` | `Collection<String>` | 字符串集合 |
| `data[]` | `LocalDate[]` | 日期数组 |
| `timestamp...[]` | `LocalDateTime[]` | 时间戳数组 |
| `json / jsonb` | `JsonNode` | JSON/JSONB类型 |
| `json[] / jsonb[]` | `JsonNode[]` | JSON数组 |

### 2.7 Freemarker 模板体系

**模板根路径：** `isass-database-core/src/main/resources/v2Template/`

#### 2.7.1 EntityType.ftl — 内置字段识别

**位置：** `v2Template/segment/EntityType.ftl`

通过比对表字段名与框架常量，自动判断实体类型属性：

```
遍历表的所有字段:
  ├─ 对比 cfg.idEntity.ID_COLUMN_NAME       → isIdEntity = true
  ├─ 对比 cfg.parentIdEntity.PARENT_ID_COLUMN_NAME → isParentIdEntity = true
  ├─ 对比 cfg.logicDeleteEntity.DELETE_FLAG_COLUMN_NAME → isLogicDeleteEntity = true
  ├─ 对比 cfg.tenantEntity.TENANT_ID_COLUMN_NAME → isTenantEntity = true
  ├─ 对比 cfg.traceEntity 的 6 个字段 → isTraceEntity = true
  └─ 对比 cfg.versionEntity.VERSION_COLUMN_NAME → isVersionEntity = true
```

**内置字段名列表（不在 `randomEntity()` 中随机赋值，不生成 Criteria 条件）：**
```
"id"、"parent_id"、"delete_flag"、"tenant_id"、
"create_user_id"、"create_user_name"、"create_time"、
"modify_user_id"、"modify_user_name"、"modify_time"、"version"
```

#### 2.7.2 entity.java.ftl — 实体模板

生成内容包含：
- **Lombok 注解：** `@Getter @Setter @ToString @SuperBuilder @NoArgsConstructor @AllArgsConstructor`
- **多接口实现：** 根据识别结果 implements `IV2IdEntity`、`IV2VersionEntity`、`IV2TenantEntity`、`IV2LogicDeleteEntity`、`IV2TraceEntity`、`IV2ParentIdEntity`、`IV2Entity`
- **字段常量：** 每个字段生成 `{FIELD_NAME}` 和 `{FIELD_NAME}_COLUMN_NAME` 两个常量
- **Long 型Id字段：** 自动添加 `@JsonSerialize(using = ToStringSerializer.class)` 防止JS精度丢失
- **枚举类：** 字段注释中含 `[枚举--1:VALUE:描述;2:VALUE2:描述2]` 格式时，自动生成 Java Enum 内部类（带 `@JsonValue/@JsonCreator` 支持）
- **getIdColumnName()：** 主键列名非 `id` 时自动覆盖
- **randomEntity()：** 为所有非内置字段生成随机赋值（RandomUtil 或枚举随机）
- **main()：** 生成测试 main 方法

#### 2.7.3 criteria.java.ftl — 查询条件模板

生成内容包含：
- 继承 `V2FullTypeCriteria<V2Entity, V2EntityCriteria>`
- 实现相应的 Criteria 标记接口（`IV2IdCriteria`、`IV2TraceCriteria` 等）
- 每个非内置字段生成 3 组 setter + 1 个 getter：

| 条件类型 | 适用字段 | 生成方法 |
|---------|---------|---------|
| 所有类型 | 全部字段 | `set{f}(v), setOr{f}(v), set{f}NotEqual(v), set{f}In(Collection), set{f}NotIn(Collection)` + 变参重载版 |
| 字符串类型 | String | 额外 `set{f}Like(v), set{f}NotLike(v), set{f}StartWith(v)` |
| 数字类型 | Integer/Long/BigDecimal 等 | 额外 `set{f}LessThan(v), set{f}GreaterThan(v), set{f}Between(v1, v2)` |
| 集合类型 | Collection | 额外 `set{f}Contains(v)` |

#### 2.7.4 entityDb.java.ftl — 数据库实体模板

数据库实体继承业务实体（`extends V2{Entity}`），并实现 `IV2DbEntity<V2{Entity}, V2{Entity}Db>`，负责承载 ORM 注解：

- `@TableName("{TABLE_NAME}")` — 表名映射
- `@TableId(type = IdType.ASSIGN_ID)` — 主键策略（雪花算法）
- `@Version` — 乐观锁字段
- `@TableLogic` — 逻辑删除字段
- `@TableField(fill = FieldFill.INSERT)` — 创建时自动填充（createUserId, createUserName, createTime）
- `@TableField(fill = FieldFill.INSERT_UPDATE)` — 创建和更新时自动填充（modifyUserId, modifyUserName, modifyTime）

**字段定义策略：** 只保留需要 ORM 注解的字段定义（主键/版本/逻辑删除/追踪字段），其他字段通过继承 `V2{Entity}` 获得。

#### 2.7.5 repository.java.ftl — 数据仓库模板

生成最简 Repository 实现，仅声明泛型参数：
```java
@Repository
public class V2{Entity}Repository extends V2MybatisPlusRepository<
        V2{Entity}, V2{Entity}Db, V2{Entity}Criteria, V2{Entity}Mapper> {
}
```
所有 CRUD 均由父类 `V2MybatisPlusRepository` 的默认实现完成，无需编写任何代码。

#### 2.7.6 iSservice.java.ftl — 服务接口模板

生成内容包含：
- 接口继承 `IV2Service<V2{Entity}, V2{Entity}Criteria>`
- `URI_FIRST_PART = "{controllerPrefix}/{entity}"` 常量（如 `/attachment-service/icon`）
- **40+ 个 URI 常量**（ADD_URI、DELETE_BY_ID_URI、FIND_ALL_URI 等）和对应的 CMD 常量
- **内嵌 ServiceManager 类**：`@Primary @Service` 内部类，自动注入 `List<IV2{Entity}Service> services`

#### 2.7.7 localService.java.ftl — 本地服务实现模板

```java
@Service
public class V2{Entity}Service implements IV2{Entity}Service,
        IV2LocalService<V2{Entity}, V2{Entity}Criteria> {
    @Getter @Autowired
    private V2{Entity}Service service;        // 自身引用
    @Getter @Autowired
    private V2{Entity}Repository repository;  // 数据仓库
}
```

#### 2.7.8 controller.java.ftl — 控制器模板

```java
@RestController
@RequestMapping(IV2{Entity}Service.URI_FIRST_PART)
public class V2{Entity}Controller implements
        IV2{Entity}Service, IV2Controller<V2{Entity}, V2{Entity}Criteria> {
    @Getter @Autowired
    private V2{Entity}Service service;
}
```
Controller **同时实现** `IV2{Entity}Service` 和 `IV2Controller` 接口，由 `IV2Controller` 的接口默认方法自动暴露 40 个 `/v2/*` REST 端点。

#### 2.7.9 feignService.java.ftl — Feign 远程调用模板

```java
@FeignClient(
    name = "{serviceName}", contextId = "v2{Entity}FeignService",
    url = "${feign.{moduleName}.url:}", primary = false)
@RequestMapping(IV2{Entity}Service.URI_FIRST_PART)
public interface V2{Entity}FeignService extends
        IV2{Entity}Service, IV2FeignService<V2{Entity}, V2{Entity}Criteria> {
}
```

---

## 三、框架核心接口层级

### 3.1 IV2Service\<E, C\> — 顶层服务接口

**文件位置：** `isass-nocode-core/src/main/java/vip/isass/framework/nocode/v2/service/IV2Service.java`

定义了 40+ 个 CRUD 方法签名和对应的 HTTP 操作常量（OPERATOR + URI_*_PART）。

**泛型参数：**
- `E extends IV2Entity<E>` — 实体类型
- `C extends IV2Criteria<E, C>` — 查询条件类型

**功能分类（共 40 个方法）：**

#### 3.1.1 增 (Create) — 10 个方法
| 方法 | HTTP常量 | URI片段 | 说明 |
|------|----------|---------|------|
| `add(E)` | `POST` | `/v2` | 新增单条 |
| `addBatch(Collection<E>)` | `POST` | `/v2/batch` | 批量新增 |
| `addBatchByBatchSize(Collection<E>, int)` | `POST` | `/v2/batch/batchSize/{batchSize}` | 按批次大小批量新增 |
| `addIfAbsentByCriteria(E, C)` | `POST` | `/v2/absent/criteria` | 按条件不存在时新增 |
| `addIfAbsentByColumns(E, List<String>)` | `POST` | `/v2/absent/{uniqueColumns}` | 按唯一列不存在时新增 |
| `addBatchIfAbsentByCriteria(List<E>, C)` | `POST` | `/v2/batch/absent/criteria` | 批量按条件不存在时新增 |
| `addBatchIfAbsentByColumns(List<E>, List<String>)` | `POST` | `/v2/batch/absent/{uniqueColumns}` | 批量按唯一列不存在时新增 |
| `addOrUpdateByCriteria(E, C)` | `POST` | `/v2/add-update/criteria` | 按条件新增或更新 |
| `addOrUpdateByColumns(E, List<String>)` | `POST` | `/v2/add-update/{uniqueColumns}` | 按唯一列新增或更新 |
| `addOrUpdateBatchByColumns(List<E>, List<String>)` | `POST` | `/v2/add-update/batch/{uniqueColumns}` | 批量按唯一列新增或更新 |

#### 3.1.2 删 (Delete) — 3 个方法
| 方法 | HTTP常量 | URI片段 | 说明 |
|------|----------|---------|------|
| `deleteById(Serializable)` | `DELETE` | `/v2/id/{id}` | 按ID删除 |
| `deleteByIds(Collection<Serializable>)` | `DELETE` | `/v2/{ids}` | 批量按ID删除 |
| `deleteByCriteria(C)` | `DELETE` | `/v2/criteria` | 按条件删除 |

#### 3.1.3 改 (Update) — 6 个方法
| 方法 | HTTP常量 | URI片段 | 说明 |
|------|----------|---------|------|
| `updateById(E)` | `PUT` | `/v2` | 按ID更新（非空字段） |
| `updateAllColumnsById(E)` | `PUT` | `/v2/allColumns` | 按ID更新全部字段（含null） |
| `updateByIdOrException(E)` | `PUT` | `/v2/exception` | 按ID更新，不存在抛异常 |
| `updateByCriteria(E, C)` | `PUT` | `/v2/criteria` | 按条件更新 |
| `updateByCriteriaOrException(E, C)` | `PUT` | `/v2/criteria/exception` | 按条件更新，不存在抛异常 |
| `batchSave(BatchSave<E>)` | `POST` | `/v2/batchSave` | 批量保存（增+改+删） |

#### 3.1.4 查 (Read) — 21 个方法
| 方法 | HTTP常量 | URI片段 | 说明 |
|------|----------|---------|------|
| `getById(Serializable)` | `GET` | `/v2/{id}` | 按ID查询 |
| `getByIdOrException(Serializable)` | `GET` | `/v2/exception/{id}` | 按ID查询，不存在抛异常 |
| `getByCriteria(C)` | `GET` | `/v2/1/criteria` | 按条件查询一条 |
| `getByCriteriaOrWarn(C)` | `GET` | `/v2/warn/criteria` | 按条件查询，不存在warn |
| `getByCriteriaOrException(C)` | `GET` | `/v2/exception/criteria` | 按条件查询，不存在抛异常 |
| `findByCriteria(C)` | `GET` | `/v2/criteria` | 按条件查询列表 |
| `findPageByCriteria(C)` | `GET` | `/v2/page` | 按条件分页查询 |
| `findAll()` | `GET` | `/v2/all` | 查询全部 |
| `countByCriteria(C)` | `GET` | `/v2/count/criteria` | 按条件统计 |
| `countAll()` | `GET` | `/v2/count/all` | 全表统计 |
| `isPresentById(Serializable)` | `GET` | `/v2/present/{id}` | 按ID判断存在 |
| `isPresentByColumn(String, Object)` | `GET` | `/v2/present/{columnName}/{value}` | 按列值判断存在 |
| `isPresentByCriteria(C)` | `GET` | `/v2/present/criteria` | 按条件判断存在 |
| `isAbsentByColumn(String, Object)` | `GET` | `/v2/absent/{columnName}/{value}` | 按列值判断不存在 |
| `isAbsentByCriteria(C)` | `GET` | `/v2/absent/criteria` | 按条件判断不存在 |
| `exceptionIfPresentByCriteria(C)` | `GET` | `/v2/exception-if-present/criteria` | 存在则抛 AlreadyPresentException |
| `exceptionIfAbsentByCriteria(C)` | `GET` | `/v2/exception-if-absent/criteria` | 不存在则抛 AbsentException |

### 3.2 IV2LocalService\<E, C\> — 本地业务实现层

**文件位置：** `isass-nocode-core/src/main/java/vip/isass/framework/nocode/v2/service/IV2LocalService.java`

**继承关系：** `IV2LocalService extends IV2Service`

**关键抽象方法：**
- `IV2Repository<E, C> getRepository()` — 子类必须提供 Repository 实现
- `IV2Service<E, C> getService()` — 返回自身引用（用于 ServiceManager 路由）

**优先级常量：** `getOrder()` 返回 `ApiOrder.LOCAL_SERVICE`

**默认实现策略（全部委托给 Repository）：**
- `add()` → `getRepository().add(entity); return entity`
- `deleteById()` → `getRepository().deleteById(id)`
- `updateById()` → 检查 entity 是否为 `IV2IdEntity` 实例；调用 `getRepository().updateById(entity)`
- `getById()` → `getRepository().getEntityById(id)`
- `findByCriteria()` → `getRepository().findByCriteria(criteria)`
- `findPageByCriteria()` → `getRepository().findPageByCriteria(criteria)`

**安全校验（delete/update-by-criteria 触发）：**
- `exceptionIfHaveNoCondition(criteria)` — 校验 criteria 是否 instanceof `IV2WhereConditionCriteria`，并至少设置1个条件。防止误操作全表删除/更新。

**组合方法实现：**
- `batchSave(BatchSave)` → 批量新增 + 逐条更新 + 批量删除
- `addIfAbsentByCriteria()` → `isAbsentByCriteria()` 为 true 时执行 `add()`
- `addOrUpdateByCriteria()` → 先尝试 `updateByCriteria()`，影响行数为0时执行 `add()`

### 3.3 IV2ServiceManager\<E, C, S\> — 路由/组合层

**文件位置：** `isass-nocode-core/src/main/java/vip/isass/framework/nocode/v2/service/IV2ServiceManager.java`

**继承关系：** `IV2ServiceManager extends IV2Service`

**泛型参数：**
- `S extends IV2Service<E, C>` — 具体的 Service 接口类型

**关键抽象方法：**
- `List<S> getServices()` — 返回所有 Service 实现列表（通过 Spring `@Autowired` 自动注入）

**优先级常量：** `getOrder()` 返回 `ApiOrder.SERVER_MANAGER`

**核心路由逻辑（全部经由 V2ServiceManagerUtil）：**
- 每个 CRUD 方法都调用 `V2ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.xxx(...))`
- `consume()` — 用于 void 返回的方法（如 `updateByIdOrException`）
- `consumeWithoutException()` — 吞掉所有异常的消费

**提供的便捷方法：**
- `applyUntilNotNull(Function<S, V>)` — 遍历 services 找到第一个非null结果
- `consume(Consumer<S>)` — 遍历 services 执行消费直到成功
- `consumeWithoutException(Consumer<S>)` — 同上但忽略异常

### 3.4 V2ServiceManagerUtil — 路由引擎

**文件位置：** `isass-nocode-core/src/main/java/vip/isass/framework/nocode/v2/service/V2ServiceManagerUtil.java`

**核心方法：**

#### 3.4.1 `applyUntilNotNull(List<S> services, Function<S, V> function)`
```
遍历 services 列表，对每个 service 执行 function.apply(service)：
1. 跳过 IV2ServiceManager 实例（Manager自身不应被注入到services列表）
2. 跳过 getOrder() == ApiOrder.CONTROLLER 的实例（Controller不由Manager调用）
3. 遇到 IV2LocalService 标记 hasLocalService = true
4. 如果 hasLocalService == true 且 service 的 order == ApiOrder.FEIGN_SERVICE，跳过
5. 捕获 UnimplementedMethodException 并忽略，继续下一个
6. 第一个非null结果直接返回
7. 全部返回null时，返回null
```

**路由优先级策略：**
1. **跳过** Controller（`ApiOrder.CONTROLLER`）和 Manager（`IV2ServiceManager`）
2. **优先** LocalService（`ApiOrder.LOCAL_SERVICE`）
3. **跳过** FeignService（`ApiOrder.FEIGN_SERVICE`）当本地服务可用时
4. **忽略** 未实现的方法（`UnimplementedMethodException`）

#### 3.4.2 `consume(List<S> services, Consumer<S> consumer)`
遍历逻辑同上，但执行 consumer 后直接 return（执行一次即可成功）。

#### 3.4.3 `consumeWithoutException(List<S> services, Consumer<S> consumer)`
同上，但额外捕获所有 `Exception`，只打日志不中断。

### 3.5 IV2Controller\<E, C\> — HTTP 端点层

**文件位置：** `isass-web-springmvc/src/main/java/vip/isass/framework/web/structure/IV2Controller.java`

**继承关系：** `IV2Controller extends IV2Service`

**关键抽象方法：**
- `IV2LocalService<E, C> getService()` — 获取底层 LocalService 实现

**优先级常量：** `getOrder()` 返回 `ApiOrder.CONTROLLER`

**Spring MVC 注解映射（以 add 为例）：**
```java
@PostMapping(ADD_URI_SECOND_PART)   // "/v2"
default E add(@RequestBody E entity) {
    return getService().add(entity);
}
```

**关键特性：**
- 使用 `@ModelAttribute` 绑定 Criteria 参数（GET 请求的查询条件通过 QueryString 传递）
- 所有 `/v2/*` 路径为第二段，第一段由各服务的 Controller 通过 `@RequestMapping(URI_FIRST_PART)` 定义
- 最终完整 URL 格式：`{URI_FIRST_PART}/v2/...`

### 3.6 IV2Repository\<E, C\> — 数据访问接口

**文件位置：** `isass-nocode-core/src/main/java/vip/isass/framework/nocode/v2/repository/IV2Repository.java`

**所有默认方法均抛出 `UnsupportedOperationException`**，必须由具体数据库实现覆盖。

**方法清单（与 IV2Service 对应，无 "ifAbsent" 系列）：**
- 增：`add`, `addBatch`, `addBatch(batchSize)`, `addOrUpdate`, `addIfAbsentByCriteria`, `addIfAbsentByColumns`
- 删：`deleteById`, `deleteByIds`, `deleteByCriteria`
- 改：`updateById`, `updateAllColumnsById`, `updateByCriteria`
- 查：`getEntityById`, `getByIdOrException`, `getByCriteria`, `getByCriteriaOrWarn`, `getByCriteriaOrException`, `findByCriteria`, `findPageByCriteria`, `findAll`, `countByCriteria`, `countAll`, `isPresentById`, `isPresentByColumn`, `isPresentByCriteria`, `exceptionIfPresentByCriteria`, `exceptionIfAbsentByCriteria`

**工具方法：**
- `getIdColumnName(Class<?>)` — 缓存并返回实体类的 ID 列名

---

## 四、MyBatis-Plus 实现层

### 4.1 V2MybatisPlusRepository\<E, EDB, C, M\>

**文件位置：** `isass-database-mybatisplus/src/main/java/vip/isass/framework/database/mybatisplus/plus/V2MybatisPlusRepository.java`

**继承关系：** `extends ServiceImpl<M, EDB> implements IV2Repository<E, C>`

**泛型参数：**
- `E extends IV2Entity<E>` — 接口层实体（业务实体）
- `EDB extends IV2DbEntity<E, EDB>` — 持久层实体（DB实体）
- `C extends IV2Criteria<E, C>` — 查询条件
- `M extends IMapper<EDB>` — MyBatis-Plus Mapper

**核心设计 — Entity/EDB 双层模型：**
```
业务层: V2Attachment implements IV2IdEntity, IV2TraceEntity, ...
         ↓ V2DbEntityConvert.convertToDbEntity()
持久层: V2AttachmentDb (需要实现 IV2DbEntity，包含 @TableName, @TableId 等MP注解)
         ↓ edb.convertToEntity()
业务层: V2Attachment
```

**关键实现细节：**

#### 4.1.1 新增操作 (`add`)
```java
public boolean add(E entity) {
    EDB edb = V2DbEntityConvert.convertToDbEntity(entity);  // 实体转换
    super.save(edb);                                          // MyBatis-Plus save
    fillV2EntityProperties(entity, edb);                     // 回写自动填充字段
    return true;
}
```
`fillV2EntityProperties` 回写数据库自动填充字段到 entity：
- ID（自增/雪花）
- 追踪字段（createTime, createUserId, modifyTime, ...）
- 父级ID
- 逻辑删除标记
- 版本号
- 租户ID

#### 4.1.2 ID 类型转换
删除和查询操作会自动处理 String/Long ID 类型转换：
```java
TableInfo tableInfo = TableInfoHelper.getTableInfo(edbClass);
if (tableInfo != null && Number.class.isAssignableFrom(tableInfo.getKeyType())) {
    realId = Long.parseLong(id.toString());
}
```

#### 4.1.3 敏感字段自动过滤
`findByWrapper` 和 `findPageByWrapper` 自动过滤 `SensitiveDataProperty.PROPERTIES` 中的敏感字段（如密码等），除非查询显式指定了 `select` 列。

#### 4.1.4 updateAllColumnsById
与 `updateById`（只更新非空字段）不同，此方法将 null 字段也写入 SQL：
```java
for (TableFieldInfo tableFieldInfo : tableInfo.getFieldList()) {
    Object value = map.get(tableFieldInfo.getProperty());
    if (value != null) continue;
    FieldFill fieldFill = tableFieldInfo.getFieldFill();
    if (fieldFill != FieldFill.DEFAULT) continue; // 跳过自动填充字段
    updateWrapper.set(tableFieldInfo.getColumn(), null);
}
```

### 4.2 V2WrapperUtil — Criteria → MyBatis-Plus Wrapper 转换器

**文件位置：** `isass-database-mybatisplus/src/main/java/vip/isass/framework/database/mybatisplus/plus/V2WrapperUtil.java`

**核心方法：**

#### 4.2.1 `getQueryWrapper(IV2Criteria)` → `QueryWrapper<E>`
按类型判断并处理：
1. `IV2SelectColumnCriteria` → `wrapper.select(...)` 设置查询列
2. `IV2WhereConditionCriteria` → 遍历 `whereConditions` 列表，逐个调用 `V2MybatisPlusWhereCondition.apply(wc, wrapper)` 构建 WHERE 条件
3. `IV2PageCriteria` → 暂不处理（分页在 Repository 层通过 `IPage` 参数处理）
4. `IV2OrderByCriteria` → 解析 `orderBy` 字符串（格式：`col1 ASC, col2 DESC`）并设置排序

#### 4.2.2 `getEdbQueryWrapper(IV2Criteria)` → `QueryWrapper<EDB>`
直接 cast `getQueryWrapper(criteria)` 的结果（EDB 类型与 E 类型共享 Wrapper 结构）。

#### 4.2.3 `getUpdateWrapper(IV2Criteria)` → `UpdateWrapper<EDB>`
仅处理 `IV2WhereConditionCriteria` 的条件部分。

### 4.3 V2MybatisPlusWhereCondition — WHERE 条件构建器

按 `V2WhereCondition` 中的操作类型（EQUALS, LIKE, IN, GT, LT, BETWEEN 等）逐一应用到 `AbstractWrapper`。

---

## 五、附件微服务 (isass-service-attachment) V2 实现

### 5.1 项目结构

```
isass-service-attachment/
├── isass-service-attachment-api/          (API 模块)
│   └── src/main/java/vip/isass/attachment/api/
│       ├── model/
│       │   ├── entity/
│       │   │   ├── V2Attachment.java      — 附件实体
│       │   │   ├── V2Icon.java            — 图标实体
│       │   │   └── V2IconGroup.java       — 图标分组实体
│       │   └── criteria/
│       │       ├── V2AttachmentCriteria.java  — 附件查询条件
│       │       ├── V2IconCriteria.java        — 图标查询条件
│       │       └── V2IconGroupCriteria.java   — 图标分组查询条件
│       └── service/
│           ├── IV2AttachmentService.java   — 附件服务接口(含ServiceManager)
│           ├── IV2IconService.java         — 图标服务接口(含ServiceManager)
│           └── IV2IconGroupService.java    — 图标分组服务接口(含ServiceManager)
│
└── isass-service-attachment-service/     (Service 实现模块)
    └── src/main/java/vip/isass/attachment/
        ├── controller/
        │   ├── V2AttachmentController.java   — 附件控制器
        │   ├── V2IconController.java         — 图标控制器
        │   └── V2IconGroupController.java    — 图标分组控制器
        └── service/
            ├── V2AttachmentService.java       — 附件本地实现
            ├── V2IconService.java             — 图标本地实现
            └── V2IconGroupService.java        — 图标分组本地实现
```

### 5.2 实体继承链

#### 5.2.1 V2Attachment
```
V2Attachment implements:
  ├── IV2IdEntity<String, V2Attachment>       — ID类型=String，ID列名=id
  ├── IV2VersionEntity<V2Attachment>           — 乐观锁版本字段
  ├── IV2TenantEntity<String, V2Attachment>    — 租户ID（String类型）
  ├── IV2LogicDeleteEntity<V2Attachment>       — 逻辑删除标记
  ├── IV2TraceEntity<String, V2Attachment>     — 创建/修改用户+时间追踪
  └── IV2Entity<V2Attachment>                  — 基础实体接口
```

#### 5.2.2 V2Icon
```
V2Icon implements:
  ├── IV2IdEntity<Long, V2Icon>       — ID类型=Long（自增）
  ├── IV2TraceEntity<String, V2Icon>  — 追踪字段（用户ID为String）
  └── IV2Entity<V2Icon>
```

#### 5.2.3 V2IconGroup
```
V2IconGroup implements:
  ├── IV2IdEntity<Long, V2IconGroup>       — ID类型=Long（自增）
  ├── IV2TraceEntity<String, V2IconGroup>  — 追踪字段
  └── IV2Entity<V2IconGroup>
  包含枚举 GroupType: PUBLIC(1), FAVORITE(2), CUSTOM(3), CREATION(4)
```

### 5.3 查询条件继承链

每个 Criteria 类遵循统一模式：
```
V2*Criteria extends V2FullTypeCriteria<E, C>    (提供所有条件类型的构建方法)
  implements IV2IdCriteria<PK, E, C>             (ID字段的条件方法)
  implements IV2TraceCriteria<PK, E, C>          (追踪字段的条件方法)
  implements IV2Criteria<E, C>                   (标记接口)

V2AttachmentCriteria 额外实现:
  implements IV2VersionCriteria<E, C>             (版本字段条件方法)
  implements IV2TenantCriteria<PK, E, C>          (租户字段条件方法)
```

**每个字段的查询条件五级支持：**

以 `V2Attachment.originalFileName` 为例：

| 分类 | 方法 | 示例 |
|------|------|------|
| **通用** | `setOriginalFileName(String)` | 等于 |
| **通用** | `setOrOriginalFileName(String)` | OR等于 |
| **通用** | `setOriginalFileNameNotEqual(String)` | 不等于 |
| **通用** | `setOriginalFileNameIn(Collection<String>)` | IN |
| **通用** | `setOriginalFileNameNotIn(Collection<String>)` | NOT IN |
| **字符串专属** | `setOriginalFileNameLike(String)` | LIKE |
| **字符串专属** | `setOriginalFileNameNotLike(String)` | NOT LIKE |
| **字符串专属** | `setOriginalFileNameStartWith(String)` | LIKE 'xxx%' |

**数字类型字段额外支持：**
- `setFileSizeLessThan(Long)`
- `setFileSizeGreaterThan(Long)`
- `setFileSizeBetween(Long, Long)`

### 5.4 服务接口模式 — 内部类 ServiceManager

**以 IV2AttachmentService 为例：**

```java
public interface IV2AttachmentService extends IV2Service<V2Attachment, V2AttachmentCriteria> {

    String URI_FIRST_PART = "/attachment-service/attachment";

    // === 为每个操作定义完整 URI 和 CMD 常量 ===
    String ADD_URI = URI_FIRST_PART + ADD_URI_SECOND_PART;  // "/attachment-service/attachment/v2"
    String ADD_CMD = ADD_OPERATOR + " " + ADD_URI;           // "POST /attachment-service/attachment/v2"
    // ... 40+ 个 URI/CMD 常量 ...

    // === @Primary @Service 内部类 — ServiceManager ===
    @Primary
    @Service
    class V2AttachmentServiceManager implements
            IV2AttachmentService,
            IV2ServiceManager<V2Attachment, V2AttachmentCriteria, IV2AttachmentService> {

        @Getter
        @Autowired(required = false)
        private List<IV2AttachmentService> services;  // 自动注入所有 IV2AttachmentService 实现

        // 自定义业务方法在 ServiceManager 中也可定义默认实现
    }
}
```

**设计目的：**
- ServiceManager 与 Service 接口同文件定义，确保接口与路由层强绑定
- `@Primary` 确保 Spring 注入 `IV2AttachmentService` 时优先使用 Manager（而非 LocalService）
- `@Autowired(required = false)` 允许服务列表为空（防止启动失败）
- CMD 常量用于 API 文档生成和权限控制配置

### 5.5 Controller 实现模式

#### 5.5.1 标准 CRUD Controller

代码生成器模板 `controller.java.ftl` 生成的标准 Controller 实现 `IV2Controller` 接口，自动获得全部 40 个 `/v2/*` REST 端点：

```java
@RestController
@RequestMapping(IV2IconService.URI_FIRST_PART)
public class V2IconController implements
        IV2IconService, IV2Controller<V2Icon, V2IconCriteria> {
    @Getter @Autowired
    private V2IconService service;
}
```

Controller 同时实现业务 Service 接口和 `IV2Controller`，40 个端点由 `IV2Controller` 的接口 default 方法自动映射。

#### 5.5.2 业务 Controller（V2AttachmentController）
除了标准 CRUD 外，还包含文件上传/下载/秒传/预览等自定义端点：
- `POST /attachment-service/upload` — 文件上传
- `POST /attachment-service/upload/param` — 带参数上传
- `POST /attachment-service/instantTransmission` — 秒传
- `POST /attachment-service/upload/zip` — 压缩包上传
- `GET /attachment-service/download/{attachmentId}` — 文件下载
- `GET /attachment-service/download/pack` — 打包下载
- `GET /attachment-service/preview/{attachmentId}` — 文件预览
- `DELETE /attachment-service/attachment/bizType/{bizType}/bizId/{bizId}` — 按业务对象删除

#### 5.5.3 自定义业务端点（V2IconGroupController）
```java
@DeleteMapping("/{id}/icons")
public Boolean deleteWithIcons(@PathVariable("id") Long id) {
    return service.deleteWithIcons(id);          // 级联删除分组+图标
}

@GetMapping("/allIcons/groupByIconGroup")
public List<IconVo> findAllIconGroupByIconGroup() {
    return service.findAllIconGroupByIconGroup(); // 分组查询
}
```

---

## 六、完整调用链追踪

### 6.1 示例1：`GET /attachment-service/icon/v2/all` — 查询所有图标

```
1. HTTP Request
   GET /attachment-service/icon/v2/all
        │
2. Spring MVC 路由到 V2IconController
   @RequestMapping(IV2IconService.URI_FIRST_PART)  → "/attachment-service/icon"
        │
3. IV2Controller.findAll()
   @GetMapping(FIND_ALL_URI_SECOND_PART)  // "/v2/all"
   → getService().findAll()
        │
4. V2ServiceManagerUtil.applyUntilNotNull(getServices(), IV2Service::findAll)
   → 遍历 services 列表:
      ├─ 跳过 V2IconServiceManager (instanceof IV2ServiceManager)
      ├─ 执行 V2IconService.findAll()
      │   └─ IV2LocalService.findAll()
      │       └─ getRepository().findAll()
      │           └─ V2MybatisPlusRepository.findAll()
      │               └─ findByWrapper(null)
      │                   └─ list(null).stream().map(convertToEntity).collect(toList())
      │                       (自动过滤敏感字段)
      └─ 跳过 FeignService (已返回非null结果)
   → 返回 List<V2Icon>
```

### 6.2 示例2：`POST /attachment-service/attachment/v2` — 新增附件

```
1. HTTP Request
   POST /attachment-service/attachment/v2
   Body: V2Attachment JSON
        │
2. Spring MVC 路由到 V2AttachmentController
        │
3. IV2Controller.add(@RequestBody E entity)
   @PostMapping(ADD_URI_SECOND_PART)  // "/v2"
   → getService().add(entity)
        │
4. V2ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.add(entity))
   → 遍历 IV2AttachmentService 实现列表:
      ├─ 跳过 V2AttachmentServiceManager
      ├─ 执行 V2AttachmentService.add()
      │   └─ IV2LocalService.add(E entity):
      │       ├─ V2DbEntityConvert.convertToDbEntity(entity) → EDB
      │       ├─ super.save(edb) → MyBatis-Plus INSERT
      │       ├─ fillV2EntityProperties(entity, edb):
      │       │   ├─ 回写 ID (自增/雪花)
      │       │   ├─ 回写 createTime, createUserId, modifyTime 等追踪字段
      │       │   └─ 回写 version, tenantId, deleteFlag 等
      │       └─ return entity
      └─ 跳过 FeignService
   → 返回 E (含自动填充字段)
```

### 6.3 示例3：`DELETE /attachment-service/icon/{id}/icons` — 级联删除图标分组

```
1. HTTP Request
   DELETE /attachment-service/iconGroup/{id}/icons
        │
2. V2IconGroupController.deleteWithIcons(@PathVariable("id") Long id)
   → service.deleteWithIcons(id)
        │
3. V2IconGroupService.deleteWithIcons(Long iconGroupId)
   @Transactional(rollbackFor = Exception.class)
   ├─ v2IconService.deleteByCriteria(new V2IconCriteria().setIconGroupId(iconGroupId))
   │   │
   │   ├─ IV2LocalService.deleteByCriteria(C criteria)
   │   │   └─ exceptionIfHaveNoCondition(criteria)  // 校验条件非空
   │   │   └─ getRepository().deleteByCriteria(criteria)
   │   │       └─ V2MybatisPlusRepository.deleteByCriteria()
   │   │           └─ deleteByWrapper(V2WrapperUtil.getEdbQueryWrapper(criteria))
   │   │               └─ V2WrapperUtil → processWhereConditionCriteria → V2MybatisPlusWhereCondition.apply
   │   │                   └─ super.remove(wrapper)  // MyBatis-Plus DELETE
   │
   └─ deleteById(iconGroupId)
       └─ 同上链路
```

### 6.4 示例4：`GET /attachment-service/iconGroup/allIcons/groupByIconGroup` — 分组查询图标

```
1. HTTP Request
   GET /attachment-service/iconGroup/allIcons/groupByIconGroup
        │
2. V2IconGroupController.findAllIconGroupByIconGroup()
   → service.findAllIconGroupByIconGroup()
        │
3. V2IconGroupService.findAllIconGroupByIconGroup()
   ├─ findAll()  // 查询所有图标分组
   │   └─ IV2LocalService.findAll()
   │       └─ getRepository().findAll()
   │           └─ V2MybatisPlusRepository.findAll()
   │               └─ findByWrapper(null)
   │
   ├─ v2IconService.findAll()  // 查询所有图标
   │   └─ 同上链路
   │
   └─ Stream 处理:
       iconGroups.stream()
         .map(ig → {
            过滤匹配 iconGroupId 的图标
            按 orderNum 排序
            返回 IconVo(iconGroup, icons)
         })
         .collect(toList())
```

---

## 七、框架接口层次关系图（UML）

```
                        ┌──────────────────────┐
                        │   IV2Service<E, C>   │  (40个CRUD方法签名 + HTTP常量)
                        └──────────┬───────────┘
                                   │
          ┌────────────────────────┼────────────────────────┐
          │                        │                         │
┌─────────▼──────────┐  ┌─────────▼──────────────┐  ┌──────▼──────────┐
│ IV2LocalService    │  │ IV2ServiceManager      │  │ IV2Controller   │
│ <E, C>             │  │ <E, C, S>              │  │ <E, C>          │
│                    │  │                        │  │                 │
│ + getRepository()  │  │ + getServices()        │  │ + getService()  │
│ + getService()     │  │                        │  │                 │
│                    │  │ (通过V2ServiceManager-  │  │ (@RestController│
│ (委托Repository)   │  │  Util路由到services)   │  │ 注解所有端点)   │
└────────┬───────────┘  └────────────────────────┘  └─────────────────┘
         │
         │ getRepository()
         │
┌────────▼─────────────────────────────────────────────┐
│              IV2Repository<E, C>                      │
│  (默认全部抛出 UnsupportedOperationException)         │
└────────┬─────────────────────────────────────────────┘
         │ implements
         │
┌────────▼─────────────────────────────────────────────┐
│  V2MybatisPlusRepository<E, EDB, C, M>               │
│  extends ServiceImpl<M, EDB>                          │
│                                                       │
│  - Entity ↔ EDB 转换 (V2DbEntityConvert)              │
│  - Criteria → Wrapper 转换 (V2WrapperUtil)            │
│  - 自动回写填充字段 (fillV2EntityProperties)          │
│  - 敏感字段过滤 (SensitiveDataProperty)               │
│  - ID 类型转换 (String ↔ Long)                        │
└──────────────────────────────────────────────────────┘
```

---

## 八、关键设计模式

### 8.1 策略模式 — Service 路由
`IV2ServiceManager` 持有一组 `IV2Service` 实现，通过 `V2ServiceManagerUtil` 按优先级策略选择执行：
- **Local 优先：** 本地有实现时跳过 Feign 远程调用
- **容错：** 捕获 `UnimplementedMethodException` 继续尝试下一个
- **短路：** 第一个非null结果就返回

### 8.2 组合模式 — ServiceManager
`IV2ServiceManager` 既是 `IV2Service`，又持有 `List<S> services`，对外表现为单一服务，内部路由到多个实现。

### 8.3 模板方法模式 — IV2LocalService
父接口定义算法骨架（先校验条件 → 再委托 Repository），子类只需提供 `getRepository()` 即可获得全部 CRUD 能力。

### 8.4 桥接模式 — Entity/EDB 双层模型
业务层 Entity 和持久层 EDB（带 MyBatis-Plus 注解）解耦：
- Entity 不包含 ORM 注解，保持纯净
- EDB 包含 `@TableName`、`@TableId`、`@TableField` 等注解
- `V2DbEntityConvert` 负责双向转换

### 8.5 构建器模式 — Criteria
`V2FullTypeCriteria` 基类提供所有条件的链式构建方法，通过 `V2WrapperUtil` 转换为 MyBatis-Plus `QueryWrapper`/`UpdateWrapper`。

---

## 九、API 端点汇总

以 `IV2AttachmentService` 为例，完整端点路径：

| HTTP方法 | URI | 说明 |
|----------|-----|------|
| `POST` | `/attachment-service/attachment/v2` | 新增附件 |
| `POST` | `/attachment-service/attachment/v2/batch` | 批量新增 |
| `POST` | `/attachment-service/attachment/v2/batch/batchSize/{batchSize}` | 按批次大小新增 |
| `POST` | `/attachment-service/attachment/v2/absent/criteria` | 条件不存在时新增 |
| `POST` | `/attachment-service/attachment/v2/absent/{uniqueColumns}` | 按唯一列不存在时新增 |
| `POST` | `/attachment-service/attachment/v2/batch/absent/criteria` | 批量条件不存在时新增 |
| `POST` | `/attachment-service/attachment/v2/batch/absent/{uniqueColumns}` | 批量按唯一列不存在时新增 |
| `POST` | `/attachment-service/attachment/v2/add-update/criteria` | 条件新增或更新 |
| `POST` | `/attachment-service/attachment/v2/add-update/{uniqueColumns}` | 按唯一列新增或更新 |
| `POST` | `/attachment-service/attachment/v2/add-update/batch/{uniqueColumns}` | 批量按唯一列新增或更新 |
| `POST` | `/attachment-service/attachment/v2/batchSave` | 批量保存(增+改+删) |
| `DELETE` | `/attachment-service/attachment/v2/id/{id}` | 按ID删除 |
| `DELETE` | `/attachment-service/attachment/v2/{ids}` | 批量按ID删除 |
| `DELETE` | `/attachment-service/attachment/v2/criteria` | 按条件删除 |
| `PUT` | `/attachment-service/attachment/v2` | 按ID更新(非空字段) |
| `PUT` | `/attachment-service/attachment/v2/allColumns` | 按ID更新全部字段 |
| `PUT` | `/attachment-service/attachment/v2/exception` | 按ID更新(不存在抛异常) |
| `PUT` | `/attachment-service/attachment/v2/criteria` | 按条件更新 |
| `PUT` | `/attachment-service/attachment/v2/criteria/exception` | 按条件更新(不存在抛异常) |
| `GET` | `/attachment-service/attachment/v2/{id}` | 按ID查询 |
| `GET` | `/attachment-service/attachment/v2/exception/{id}` | 按ID查询(不存在抛异常) |
| `GET` | `/attachment-service/attachment/v2/1/criteria` | 按条件查询一条 |
| `GET` | `/attachment-service/attachment/v2/warn/criteria` | 按条件查询(warn) |
| `GET` | `/attachment-service/attachment/v2/exception/criteria` | 按条件查询(异常) |
| `GET` | `/attachment-service/attachment/v2/criteria` | 按条件查询列表 |
| `GET` | `/attachment-service/attachment/v2/page` | 分页查询 |
| `GET` | `/attachment-service/attachment/v2/all` | 查询全部 |
| `GET` | `/attachment-service/attachment/v2/count/criteria` | 按条件统计 |
| `GET` | `/attachment-service/attachment/v2/count/all` | 全表统计 |
| `GET` | `/attachment-service/attachment/v2/present/{id}` | 按ID判断存在 |
| `GET` | `/attachment-service/attachment/v2/present/{columnName}/{value}` | 按列值判断存在 |
| `GET` | `/attachment-service/attachment/v2/present/criteria` | 按条件判断存在 |
| `GET` | `/attachment-service/attachment/v2/absent/{columnName}/{value}` | 按列值判断不存在 |
| `GET` | `/attachment-service/attachment/v2/absent/criteria` | 按条件判断不存在 |
| `GET` | `/attachment-service/attachment/v2/exception-if-present/criteria` | 存在则抛异常 |
| `GET` | `/attachment-service/attachment/v2/exception-if-absent/criteria` | 不存在则抛异常 |

---

## 十、附件微服务特有自定义端点

| HTTP方法 | URI | 说明 |
|----------|-----|------|
| `POST` | `/attachment-service/upload` | 上传附件（multipart） |
| `POST` | `/attachment-service/upload/param` | 带参数上传附件 |
| `POST` | `/attachment-service/instantTransmission` | 附件秒传 |
| `POST` | `/attachment-service/upload/zip` | 上传zip压缩包 |
| `GET` | `/attachment-service/download/{attachmentId}` | 下载附件 |
| `GET` | `/attachment-service/download/pack` | 打包下载多个附件 |
| `GET` | `/attachment-service/preview/{attachmentId}` | 预览附件 |
| `DELETE` | `/attachment-service/attachment/bizType/{bizType}/bizId/{bizId}` | 按业务对象删除附件 |
| `DELETE` | `/attachment-service/iconGroup/{id}/icons` | 级联删除图标分组 |
| `GET` | `/attachment-service/iconGroup/allIcons/groupByIconGroup` | 按分组查询图标树 |

---

## 十一、安全机制

### 11.1 条件删除/更新安全校验
`IV2LocalService` 的 `exceptionIfHaveNoCondition()` 方法确保按条件操作时必须传至少一个条件，防止误全表操作。

### 11.2 敏感字段过滤
`V2MybatisPlusRepository.findByWrapper()` 自动过滤 `SensitiveDataProperty.PROPERTIES` 中的字段，除非 SQL 已显式指定 `select` 列。

### 11.3 乐观锁
`IV2VersionEntity` 接口提供 `version` 字段，MyBatis-Plus `updateById` 操作自动使用乐观锁更新。

### 11.4 ID 类型安全
`IV2IdEntity<Pk>` 泛型限定主键类型，`getRepository()` 中的类型转换确保 String 和 Long 类 ID 的正确处理。
