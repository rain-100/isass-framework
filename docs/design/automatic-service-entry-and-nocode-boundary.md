# 自动服务入口与 NoCode 边界设计

## 文档状态

- 状态：已按确认方案实施（破坏式重构，不提供旧 API、旧 URL 或旧合同兼容）
- 首次记录：2026-08-11
- 最近整理：2026-08-13
- 目的：定义自动 HTTP/gRPC 服务入口、DDD 应用服务、NoCode CRUD、API 文档和 Security 授权服务的边界
- 关联文档：[NoCode 级联、关联与树形 CRUD 设计](nocode-crud-scenario-implementation.md)

本文中的“确定”表示实施时必须遵守的设计；“建议”仅用于尚需通过代码验证的实现细节。本文不再保留已经
被最终方案替代的历史备选名称和重复讨论过程。

## 1. 背景与问题

OpenClaw 使用服务账号 API Key 调用 Asset 服务时，身份认证已经成功，认证主体也携带
`ROLE_APP_NORMAL_2085973951779274753`。Asset 启用 `ROLE` URL 策略后仍返回 `403`，原因是 Asset 进程中
没有可用的角色元数据查询实现，无法取得“访问目标 URL 需要哪些角色”，最终按默认规则拒绝请求。

`IAuthorizationService` 已经证明业务微服务可以只依赖一个服务接口，由运行时自动选择 BSP 本地实现或跨微服务实现，
不需要每个微服务编写 Feign、HttpExchange 或一次性适配器。API Key 与 JWT 授权上下文统一由该接口提供，分别发布为
`authorization/apiKeyContext` 和 `authorization/jwtContext`，不再新增独立的 BSP API Key 认证接口。

当前自动路由能力位于 `isass-nocode-*`，并依赖 Javadoc `@http`、Maven 插件及
`META-INF/isass/nocode-contract.json`。这使普通 DDD 应用服务、Security 服务与 NoCode CRUD 发生了不必要
的耦合，也存在接口、源码解析结果和运行时合同漂移的风险。

本次设计将“自动发布和调用服务入口”抽成独立能力，NoCode 只保留标准 CRUD、数据模型、Criteria、关联
与生命周期能力。

## 2. 已确定的总体设计

| 主题 | 结论 |
| --- | --- |
| 自动服务入口 | 基础接口命名为 `IEntrypoint` |
| 接口元数据 | 使用运行时 Java 注解，不再使用 Javadoc `@http` 和预生成合同 JSON |
| 标准 CRUD | `IService` 改名为 `ICrudService`，并直接继承 `IEntrypoint` |
| 应用服务 | `IApplicationService` 保留在 `isass-nocode-core`，并直接继承 `IEntrypoint` |
| Security 服务 | `IAuthorizationService` 直接继承 `IEntrypoint`，Security 不依赖 NoCode |
| 领域服务 | 可按需存在，但不继承 `IEntrypoint`，不直接发布远程入口 |
| URL | 自定义入口与 NoCode 入口使用不同的固定命名空间 |
| 参数 | 不使用业务 Path 参数；使用 Query、Body、Header、Form 和 File |
| 传输 | 同进程本地实现优先；远程传输使用全局顺序和服务级覆盖 |
| API 文档 | Smart-doc 负责手写 Controller，Entrypoint 元数据运行时转换后合并到唯一 `/v3/api-docs` |
| 代码生成器 | `isass-nocode-generator` 改为普通 `jar`，只保留 NoCode 源码生成 |
| 领域模型 | 生成模型迁移到 `domain.model`，删除无行为的 `XxxAgg` 空壳类 |
| 数据库关系 | 禁止数据库外键；关联和级联由 DDL 元数据及应用事务实现 |
| 运行时授权 | 入口所需权限来自当前进程的本地 Java 权限定义，主体权限通过统一授权上下文获得，不再查询 URL—角色映射 |

## 3. 分层与接口边界

### 3.1 接口关系

```text
IEntrypoint
├── ICrudService<E, C, PK>
│   └── NoCode 标准 CRUD 应用服务
├── IApplicationService
│   └── 显式业务应用服务
└── IAuthorizationService
    └── Security 公共授权应用服务

IDomainService（可选，独立于 IEntrypoint）
```

- `IEntrypoint` 只表示可以建立本地、HTTP 和 gRPC 入口，不包含 CRUD 或领域语义；
- `ICrudService` 提供围绕一个聚合的标准应用层用例；
- `IApplicationService` 表示 DDD 应用层中的显式业务用例，不能重新解释成标准 CRUD 接口；
- `IAuthorizationService` 在架构上也是应用服务，但为了保持 Security 不依赖 NoCode，直接继承
  `IEntrypoint`；
- `IDomainService` 只承载无法自然归属单个聚合的领域规则，不处理传输、当前登录用户、事务编排或远程调用。

推荐调用方向：

```text
HTTP / gRPC Adapter
  -> IApplicationService / ICrudService
    -> Aggregate Root / Domain Entity / Domain Service
      -> Repository 接口
        -> Infrastructure Repository 实现
```

禁止领域模型反向调用应用服务、远程微服务、Mapper、Spring Bean 或传输适配器。跨聚合和跨微服务协调由
应用服务完成。

### 3.2 NoCode 与自定义入口的识别

- 具体服务接口继承 `ICrudService`，即为 NoCode 标准入口；
- 具体服务接口继承 `IApplicationService` 或直接继承 `IEntrypoint`，即为自定义入口；
- registry 根据完整接口继承树识别，不能依赖 Bean 名、包名或额外枚举；
- `ICrudService` 后代只允许继承标准 CRUD 操作；审批、发布、重做等业务操作必须拆到独立应用服务；
- 同一入口混合 `ICrudService` 与其他互斥入口基类，或直接声明自定义操作时，应用启动失败。

### 3.3 领域模型与 Repository

取消 `AttFileAgg extends AttFile` 等无行为的空壳聚合类。生成代码按以下位置组织：

```text
domain.model          聚合根、聚合成员实体和值对象
domain.repository     Repository 接口
infrastructure        Mapper、MyBatis-Plus 和 Repository 实现
application.model     Command、Query、DTO、View 和跨聚合查询结果
```

第一阶段允许生成领域模型继续携带 `IEntity`、表名、审计字段等 NoCode 技术信息，避免同时维护领域模型与
持久化模型。生成文件由 Liquibase DDL 和生成器维护，不应人工修改。

### 3.4 禁止数据库外键

框架和代码生成器禁止 `FOREIGN KEY`、`REFERENCES`、`ON DELETE CASCADE` 和 `ON DELETE RESTRICT`。
`sample_group_id`、`att_file_id`、`parent_id` 等仍是普通关联列。

原因包括：

- 避免外键检查、锁传播和数据库级联影响批量写入；
- 避免跨库、分库分表和在线表结构变更受到约束；
- 允许跨数据中心同步乱序到达、重放和冲突修复；
- 跨微服务关系本来就不能由单库外键保证一致性。

引用完整性由应用层校验、全局唯一 ID、同事务写入顺序、幂等同步、逻辑删除或墓碑以及离线巡检共同保证。

## 4. Entrypoint 定义

### 4.1 基础接口与服务信息

`IEntrypoint` 保持纯 Java 和协议无关：

```java
public interface IEntrypoint {
}
```

具体服务接口使用 `@EntrypointInfo` 明确服务、上下文和资源：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntrypointInfo {
    String serviceName();
    String contextName();
    String resourceName();
}
```

```java
@EntrypointInfo(
        serviceName = "asset-service",
        contextName = "sample",
        resourceName = "sampleGroup")
public interface ISampleGroupService
        extends ICrudService<SampleGroup, SampleGroupCriteria, Long> {
}
```

`PK` 表示实体主键类型并继承 `Serializable`。`C` 除更新、分页和排序能力外，还必须具备类型安全的
`id/idIn` Criteria 能力，使通用默认方法能够通过 `setId`、`setIdIn` 构造条件。

`resourceName` 是对外资源名，不限于数据库实体，也可以是 `taskExecution`、`authorization`、
`apiKeyAuthentication` 等名词化业务能力。迁移后不再通过包名或 `ServiceInfo` 推断服务归属。

### 4.2 操作信息

```java
/** 定义一个可通过 HTTP、gRPC 和客户端代理访问的服务操作。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntrypointOperation {

    /** URL 和远程寻址使用的稳定操作名称。 */
    String operationName();

    /** OpenAPI summary 和中文接口名。 */
    String displayName();

    /** OpenAPI description。 */
    String description() default "";

    /** OpenAPI、Knife4j 和管理端展示顺序，数值越小越靠前。 */
    int displayOrder() default 0;

    /** HTTP 请求方法。 */
    HttpMethod httpMethod();
}
```

`displayName` 必填；`description` 可空；相同 `displayOrder` 按 `operationName` 和 HTTP Method 稳定排序。
这三个展示字段都不参与路由、服务发现或权限唯一标识。

只有标注 `@EntrypointOperation` 的方法才注册 HTTP/gRPC 路由并进入 OpenAPI。方法是否为 Java `default`
与是否发布远程入口是两个独立维度：

- 带 `@EntrypointOperation` 的默认方法仍是正式入口；远程代理必须发起远程调用，不能在客户端执行
  `InvocationHandler.invokeDefault`。服务端和本地 Bean 可以继承其默认方法体；
- 未标注入口注解的默认方法仅供 Java 便捷调用，远程代理才在客户端通过
  `InvocationHandler.invokeDefault` 执行，并由它调用某个正式入口。

### 4.3 URL 规则

自定义应用服务入口：

```text
/{serviceName}/{contextName}/{resourceName}/{operationName}
```

NoCode 标准入口：

```text
/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}
```

各段统一使用 lowerCamelCase，`serviceName` 保留现有短横线服务名。`resourceName` 使用单数名词或名词
短语；`operationName` 可以是名词或动词。自定义业务的 GET 查询入口优先使用返回资源或状态的名词，
不在 URL 中重复 Java 方法名里的 `get`、`find` 等查询动词，例如获取 RSA 公钥使用 `publicKey`；
命令型操作使用 `publish`、`claim` 等动词。Java 方法仍可使用 `getRsaPublicKey()` 等便于理解的命名。
NoCode 的 `one`、`list`、`page`、`cursorPage` 等标准 `operationName` 不适用这项自定义入口命名建议，
保持标准接口名称不变。

```text
/asset-service/nocode/sample/sampleGroup/page
/asset-service/nocode/sample/sampleGroup/cursorPage
/asset-service/sample/taskExecution/claim
/bsp-service/auth/authentication/publicKey
/bsp-service/auth/authorization/jwtContext
```

`/{serviceName}/nocode/**` 是可稳定识别的 NoCode 命名空间，用于路由、OpenAPI 分组、审计、限流和第二层
数据鉴权。`nocode` 是保留路径段。

### 4.4 参数注解

核心入口不能依赖 Spring MVC 注解。确定使用：

```java
@interface QueryParam { String value() default ""; }
@interface BodyParam {}
@interface HeaderParam { String value(); }
@interface FormFieldParam { String value(); }
@interface FormFileParam { String value(); }
```

| 注解 | 用途 |
| --- | --- |
| `@QueryParam` | Query 标量、数组、集合或一层对象 |
| `@BodyParam` | JSON 请求体 |
| `@HeaderParam` | 请求头；认证头仍由 Security 统一处理 |
| `@FormFieldParam` | 表单或 Multipart 普通字段 |
| `@FormFileParam` | Multipart 文件或文件流 |

一个操作最多一个 `@BodyParam`。简单 Query 参数必须显式填写名称；对象 Query 参数不填写名称并自动展开
一层可读属性。Header、FormField 和 FormFile 必须显式填写名称。

对象 Query 的双向规则：

- 客户端只序列化非 `null` 的直接 JavaBean 属性；`false`、`0` 和空字符串仍参与序列化；
- 服务端按 Java camelCase 名称绑定到可写属性；未提供的属性保持默认值或 `null`；
- 不递归展开嵌套对象；复杂嵌套结构使用 `@BodyParam`；
- 多个 Query 对象或标量之间发生属性重名时，应用启动失败；
- 静态、transient、不可读、不可写或明确忽略的属性不参与绑定。

数组和集合采用 OpenAPI `style=form, explode=true`，使用同名重复参数：

```text
?ids=1&ids=2&ids=3
```

不接受 `ids=1,2,3` 或 `ids[]=1&ids[]=2`。数组和 `List` 保留顺序与重复值，`Set` 按首次出现顺序去重；
每个成员独立编码和转换。空集合不生成参数，若业务必须区分“未提供”与“显式空集合”，应改用 Body。

### 4.5 禁止业务 Path 参数

实体 ID、字段值、Criteria、分页和批量参数只能位于 Query 或 Body，不定义 `PathParam`。这样 URL 权限资源
可以稳定保存 `HTTP Method + 固定 Path`，不需要把真实 ID 还原成模板路径。

```text
旧：GET    /{id}             -> 新：GET    /page?id=123&pageSize=1
旧：DELETE /id/{id}          -> 新：DELETE /deleteBatch?id=123
旧：DELETE /{ids}            -> 新：少量 ID 使用 deleteBatch 的重复 Query，大量 ID 使用 superCud Body
```

NoCode 鉴权顺序为：

```text
身份认证
  -> 固定 HTTP Method + URL 角色鉴权
    -> 校验已注册的 NoCode context/resource/operation
      -> 租户、应用、字段和数据范围二次鉴权
        -> ICrudService
```

第二层 NoCode 鉴权必须位于传输无关调用链，不能只存在于 HTTP Filter。未知入口直接拒绝。API Key 和 Token
只能通过 Header 传输。

## 5. 运行时架构

### 5.1 统一元数据

HTTP、gRPC、客户端代理、OpenAPI、鉴权和 NoCode 二次鉴权共享同一个 `ServiceDefinitionRegistry`。入口
元数据至少包含：

- 服务、上下文、资源和操作标识；
- 参数名称、来源和 Java 类型；
- 返回类型和文件流方向；
- HTTP Method；
- 幂等性、重试限制和错误语义；
- 是否存在当前进程本地实现。

HTTP 和 gRPC 只负责把统一元数据映射为各自协议，不要求业务分别实现 Controller、HTTP Client、gRPC
Service 或 gRPC Client。

### 5.2 Maven 模块

```text
isass-entrypoint-core
├── IEntrypoint、入口和参数注解
├── ServiceDefinition 等协议无关元数据
└── 传输、服务端注册和错误语义 SPI

isass-entrypoint-registry
├── 扫描本地 Bean 和显式扫描包中的入口接口
├── 解析继承方法、泛型及注解
├── 校验并建立 ServiceDefinitionRegistry
├── 判断本地实现和远程代理
└── 编排已安装的传输提供者

isass-entrypoint-http
├── HTTP 服务端路由
├── HTTP 客户端传输
└── 参数、文件流、超时和错误绑定

isass-entrypoint-grpc
├── gRPC 服务端注册
├── gRPC 客户端传输
└── unary/streaming、状态码和超时处理
```

依赖方向：

```text
registry -> core
http     -> core
grpc     -> core
```

registry 通过 core SPI 发现 HTTP/gRPC 提供者，不能直接依赖具体传输模块；core 保持纯 Java，registry 可以
依赖 Spring Context，但不能依赖 Spring MVC、gRPC、OpenAPI、Smart-doc 或 NoCode。

### 5.3 本地实现与远程代理

- 存在本地实现 Bean 时，注册协议服务端，依赖注入直接获得本地实现；
- 只有接口、没有本地实现时，registry 创建一个实现同一接口的 JDK 远程代理；
- HTTP 和 gRPC 不分别注册相互竞争的业务 Bean，而是由统一代理选择传输；
- BeanDefinition 阶段必须阻止同一接口同时注册本地实现和远程代理；
- registry 快照明确记录 `localImplementation`，API 文档只展示当前进程的本地入口。

### 5.4 传输选择

采用“全局默认顺序 + 服务级覆盖”，不提供操作级覆盖：

```yaml
isass:
  entrypoint:
    client:
      transport-order:
        - HTTP
      services:
        bsp-service:
          transport-order:
            - GRPC
            - HTTP
```

调用规则：

1. 同进程本地实现永远优先；
2. 有服务级配置时使用服务级顺序，否则使用全局顺序；
3. 未配置全局顺序时默认只使用 HTTP；
4. 按顺序选择已安装且支持当前操作的传输；
5. 只有请求发出前已经确定当前传输不可用，才尝试下一种传输；
6. 请求发出后，不因超时或未知结果跨协议重试；
7. 协议内部重试必须同时满足操作幂等性和协议重试策略；
8. 没有可用传输时返回明确错误。

服务级配置键必须使用 `@EntrypointInfo.serviceName`。某操作因文件流等能力不受某协议支持时，由传输能力
判断跳过，不增加操作级配置。

## 6. NoCode 边界

### 6.1 标准 CRUD 入口

处理类型只有三种：

- **升级**：保留为正式 NoCode 远程操作，可以重命名并调整参数、返回值，必须标注
  `@EntrypointOperation`；它可以是抽象方法，也可以是最终委托给 `superCud` 的接口默认实现；
- **改为默认实现**：继续提供 Java 便捷方法，但不标注入口注解，不生成 HTTP/gRPC 路由和 OpenAPI；
- **删除**：从 `ICrudService` 公共接口移除；确有业务需要时放入 Repository 或显式应用服务。

标准写能力分为“Java 便捷方法”“面向前端的专项 API”和“超级增删改请求”三层，全部统一为
`SuperCudReq` 并进入同一个本地执行器：

```text
create / createBatch / createIfAbsent / update / updateBatch / delete / deleteBatch
  -> 构造只包含对应操作的 SuperCudReq
    -> superCud

一次提交多种增删改
  -> 构造包含多个操作分组的 SuperCudReq
    -> superCud
      -> CrudWriteExecutor.superCud
```

`SuperCudReq` 不是旧 `BatchSave` 的简单重命名类型。它包含 `addEntities`、`addByFields`、
`updateEntities`、`updateCriteria`、`deleteIds` 和 `deleteCriteria`，以覆盖标准 NoCode 的全部写能力。一次请求
可以同时填充任意多个操作分组。前端可以先在本地完成多次新增、修改、删除，再把归并后的最终变更集一次
提交；服务端在同一个本地事务中验证并执行全部分组。专项方法只是构造单一分组请求的稳定 API，不维护
第二套写入逻辑。

以下表格严格按照当前 `IService` 的方法顺序记录迁移结论。

#### 6.1.1 增

| 方法 | 处理 | 原因 | 关键实现 |
| --- | --- | --- | --- |
| `add(E entity)` | 改为默认实现 | 单体新增是超级增删改中单元素新增的特例，无需单独发布前端入口。 | 重命名为未标注入口注解的 `create`：先调用 `superCud(SuperCudReq.add(entity))`，再返回由持久化层回填后的输入实体。 |
| `addBatch(Collection<E> entities)` | 升级 | 批量新增是前端需要的正式入口，但实际写入应统一进入同一个执行器。 | 重命名为带入口注解的默认方法 `createBatch`。<br>`@EntrypointOperation(operationName="createBatch", displayName="增-批量", description="批量新增数据", displayOrder=101, httpMethod=HttpMethod.POST)`<br>`default Long createBatch(@BodyParam Collection<E> entities) { return superCud(SuperCudReq.addAll(entities)).addedCount(); }` |
| `addBatchByBatchSize(Collection<E> entities, int batchSize)` | 删除 | `batchSize` 是 Repository、驱动和运行环境的执行参数，不应由远程调用方控制。 | 从 CRUD 接口删除；Repository 根据框架配置和数据库能力分批执行，业务需要特殊批次时使用显式应用服务。 |
| `addIfAbsentByCriteria(E entity, C criteria)` | 改为默认实现 | 微服务初始化、内置参数和字典等内部场景需要幂等新增，但前端无需独立入口。 | 重命名为未标注入口注解的 `createIfAbsent`，调用方用字符串或 getter Lambda 指定 `addByFields`，方法返回是否实际新增。字段唯一性属于业务约束，框架不强制检查索引。 |
| `addIfAbsentByColumns(E entity, List<String> uniqueColumns)` | 删除 | 不再保留旧的独立方法；条件新增统一由 `superCud` 管理生命周期。 | Java 便捷方法使用 getter Lambda，传输请求使用 Java 属性名；唯一性和索引由业务负责。 |
| `addBatchIfAbsentByCriteria(List<E> entities, C criteria)` | 删除 | Criteria 中的固定值不能自动对应批次内每个实体。 | 使用 `SuperCudReq.addEntities + addByFields`；执行器遍历实体并按同一组属性分别生成不存在条件。 |
| `addBatchIfAbsentByColumns(List<E> entities, List<String> uniqueColumns)` | 删除 | 任意列名、逐条探测和部分成功语义不适合作为标准 CRUD 合同。 | 移到专用导入/同步应用服务，明确事务模式、冲突策略和逐条结果。 |
| `addOrUpdateByCriteria(E entity, C criteria)` | 删除 | “先更新、未命中再新增”在并发下不原子，Criteria 还可能匹配多条，新增时也没有稳定业务键。 | 使用显式应用服务；若未来增加标准 upsert，必须由 DDL 唯一键和数据库原子 upsert 支撑。 |
| `addOrUpdateByColumns(E entity, List<String> uniqueColumns)` | 删除 | 客户端动态选择唯一列不安全，且不同数据库的 upsert 语义不同。 | 使用命名唯一键的显式业务入口，不保留任意 `uniqueColumns`。 |
| `addOrUpdateBatchByColumns(List<E> entities, List<String> uniqueColumns)` | 删除 | 同时存在动态列、逐条 upsert、批量部分成功和事务边界不清等问题。 | 由导入/同步应用服务定义命名唯一键、批量结果、幂等和事务策略。 |

#### 6.1.2 删

| 方法 | 处理 | 原因 | 关键实现 |
| --- | --- | --- | --- |
| `deleteById(Serializable id)` | 改为默认实现 | 单体删除是超级增删改中单 ID 删除的特例，无需单独发布前端入口。 | 重命名为未标注入口注解的 `delete`：`return superCud(SuperCudReq.delete(id)).deletedCount() == 1;`。 |
| `deleteByIds(Collection<Serializable> ids)` | 改为默认实现 | 多 ID 删除已经被 Criteria 的 `idIn` 覆盖，不需要第二个远程合同。 | `return deleteBatch(newCriteria().setIdIn(ids)) > 0;`。只在 Java 层提供；远程调用使用 `deleteBatch?idIn=...`。 |
| `deleteByCriteria(C criteria)` | 升级 | Criteria 是前端批量删除的统一范围表达。 | 重命名为带入口注解的默认方法 `deleteBatch`。<br>`@EntrypointOperation(operationName="deleteBatch", displayName="删-批量", description="根据查询条件批量删除数据", displayOrder=401, httpMethod=HttpMethod.DELETE)`<br>`default Long deleteBatch(@QueryParam C criteria) { return superCud(SuperCudReq.deleteByCriteria(criteria)).deletedCount(); }`<br>没有有效 Where 条件时必须拒绝。 |

#### 6.1.3 改

| 方法 | 处理 | 原因 | 关键实现 |
| --- | --- | --- | --- |
| `updateById(E entity)` | 改为默认实现 | 单实体按 ID 更新是超级增删改中单元素更新的特例。 | 重命名为 `update(E entity)`：`return superCud(SuperCudReq.update(entity)).updatedCount() > 0;`。 |
| `updateAllColumnsById(E entity)` | 改为默认实现 | 与普通更新的唯一区别是普通字段的 `null` 是否写入数据库，不需要独立远程入口。 | 重命名为 `updateAllColumns(E entity)`：`return update(entity, newCriteria().setNullValueMode(WRITE_NULL));`；默认普通更新使用 `IGNORE_NULL`。 |
| `updateByIdOrException(E entity)` | 改为默认实现 | “未更新则抛异常”是返回结果之上的 Java 便捷语义。 | 合并为 `requireUpdate(entity, newCriteria())`；内部调用 `update`，结果为 `0` 时抛 `AbsentException`。 |
| `updateByCriteria(E entity, C criteria)` | 升级 | 前端需要正式 Criteria 更新入口。 | 重命名为带入口注解的默认方法 `updateBatch`，Body 永远是集合。<br>`@EntrypointOperation(operationName="updateBatch", displayName="改-批量", description="根据实体 ID 或查询条件批量修改数据", displayOrder=201, httpMethod=HttpMethod.PUT)`<br>`default Long updateBatch(@BodyParam Collection<E> entities, @QueryParam C criteria) { return superCud(SuperCudReq.updateByCriteria(entities, criteria)).updatedCount(); }` |
| `updateByCriteriaOrException(E entity, C criteria)` | 改为默认实现 | 与正式更新只有零影响行时抛异常的差异。 | 合并为 `requireUpdate(entity, criteria)`；内部调用 `update(entity, criteria)`。 |
| `batchSave(BatchSave<E> batchSave)` | 升级 | 它升级为兼容全部标准增删改能力的统一变更集入口，并负责一个本地事务。 | 重命名为 `superCud` 并返回汇总影响数量。<br>`@EntrypointOperation(operationName="superCud", displayName="超级增删改", description="在一个事务中执行新增、修改和删除", displayOrder=202, httpMethod=HttpMethod.POST)`<br>`SuperCudResult superCud(@BodyParam SuperCudReq<E, C> req);`<br>`ILocalCrudService` 的默认实现调用独立 `CrudWriteExecutor.superCud(service, req)`。 |

#### 6.1.4 查

| 方法 | 处理 | 原因 | 关键实现 |
| --- | --- | --- | --- |
| `getById(Serializable id)` | 改为默认实现 | ID 查询是统一 Criteria 单体查询的便捷形式，不需要独立远程操作。 | `return getOne(newCriteria().setId(id));`。 |
| `getByIdOrException(Serializable id)` | 改为默认实现 | “不存在则抛异常”可以复用统一单体查询。 | `return requireOne(newCriteria().setId(id));`。 |
| `getByCriteria(C criteria)` | 改为默认实现 | 单体查询可以复用正式 `page` 入口，避免同时发布 `one` 和 `page` 两套相同查询能力。 | 重命名为 `getOne`；复制 Criteria 后设置 `pageNum=1`、`pageSize=1`、`searchCountFlag=false`，调用 `page` 并返回第一条。 |
| `getByCriteriaOrWarn(C criteria)` | 删除 | 是否记录警告是调用方和可观测性策略，不应改变标准数据访问合同；当前名称也不能证明结果唯一。 | 调用 `getOne` 后由业务层按上下文记录日志；需要唯一结果时使用 `requireOne` 并依靠唯一条件。 |
| `getByCriteriaOrException(C criteria)` | 改为默认实现 | 它是 `getOne` 加“不存在则抛异常”的便捷语义。 | 重命名为 `requireOne`；调用 `getOne(criteria)`，返回 `null` 时抛 `AbsentException`。 |
| `findByCriteria(C criteria)` | 改为默认实现 | 小规模列表可以复用 `page`，不需要独立远程入口。 | 重命名为 `list`；在 Criteria 副本上设置 `pageNum=1`、`pageSize=9999`、`searchCountFlag=false` 后调用 `page`。 |
| `findPageByCriteria(C criteria)` | 升级 | 普通分页是所有常规查询和多个默认便捷方法的权威远程入口，且业务契约不能依赖具体 ORM。 | 重命名为 `page`。<br>`@EntrypointOperation(operationName="page", displayName="查-分页列表", description="根据查询条件返回分页列表", displayOrder=301, httpMethod=HttpMethod.GET)`<br>`Page<E> page(@QueryParam C criteria);`；MyBatis-Plus Repository 在基础设施边界把 `IPage` 转为 `Page`。 |
| `findAll()` | 删除 | 无条件返回全部数据没有稳定上限，容易造成内存、网络和数据库风险；改成限制 9999 条又会悄悄改变原语义。 | 调用方明确选择 `list(newCriteria())`、`page`、`cursorPage` 或导出任务。 |
| `countByCriteria(C criteria)` | 升级 | Criteria 可以统一表达条件统计和全表统计。 | 重命名为 `count`，返回 `Long`。<br>`@EntrypointOperation(operationName="count", displayName="查-数量", description="根据查询条件统计数据数量", displayOrder=303, httpMethod=HttpMethod.GET)`<br>`Long count(@QueryParam C criteria);` |
| `countAll()` | 改为默认实现 | 全部数量是空 Criteria 的普通统计，不需要独立远程操作。 | `return count(newCriteria());`。 |
| `isPresentById(Serializable id)` | 改为默认实现 | ID 是否存在是统一 `exists` 的便捷形式。 | 重命名为 `existsById`：`return exists(newCriteria().setId(id));`。 |
| `isPresentByColumn(String propertyName, Object value)` | 删除 | 字符串属性名不类型安全，会暴露持久化属性并绕过 Criteria 的白名单和类型转换。 | 使用 `exists(new XxxCriteria().setXxx(value))`；不提供任意属性名入口。 |
| `isPresentByCriteria(C criteria)` | 升级 | 是否存在是高频且可优化为 `SELECT 1 ... LIMIT 1` 的正式查询能力。 | 重命名为 `exists`。<br>`@EntrypointOperation(operationName="exists", displayName="查-是否存在", description="判断是否存在符合查询条件的数据", displayOrder=304, httpMethod=HttpMethod.GET)`<br>`boolean exists(@QueryParam C criteria);` |
| `isAbsentByColumn(String propertyName, Object value)` | 删除 | 与 `isPresentByColumn` 有相同的字符串属性风险，且否定语义可由 `exists` 推导。 | 使用类型安全 Criteria 后直接调用 `!exists(criteria)`。 |
| `isAbsentByCriteria(C criteria)` | 删除 | 它只是 `!exists(criteria)`，保留另一个 `absent` 词根会使存在性方法命名不统一。 | 调用方直接使用 `!exists(criteria)`；需要断言记录不存在时使用 `requireAbsent(criteria)`。 |
| `exceptionIfPresentByCriteria(C criteria)` | 改为默认实现 | “存在则抛异常”是 Java 业务前置校验，不需要远程合同。 | 重命名为 `requireAbsent`；`exists(criteria)` 为 `true` 时抛 `AlreadyPresentException`。它不能代替数据库唯一约束。 |
| `exceptionIfAbsentByCriteria(C criteria)` | 改为默认实现 | “不存在则抛异常”是 `exists` 上的便捷校验。 | 重命名为 `requireExists`；`exists(criteria)` 为 `false` 时抛 `AbsentException`。 |

现有 36 个 CRUD 方法的处理统计为：升级 7 个、改为默认实现 17 个、删除 12 个；再新增一个正式
`cursorPage`，最终形成八个正式远程入口。

### 6.2 新增的方法和类型

当前 `IService` 没有以下能力，需要新增：

| 方法或类型 | 类型 | 原因 | 关键实现 |
| --- | --- | --- | --- |
| `cursorPage(C criteria, PK cursorId, Long pageSize)` | 升级 | 大数据量查询需要避免 offset 深分页。 | `@EntrypointOperation(operationName="cursorPage", displayName="查-游标分页", description="按 ID 指定方向从游标之后查询下一页", displayOrder=302, httpMethod=HttpMethod.GET)`<br>`CursorPage<E, PK> cursorPage(@QueryParam C criteria, @QueryParam("cursorId") PK cursorId, @QueryParam("pageSize") Long pageSize);` |
| `newCriteria()` | 默认实现基础能力 | `getById`、`deleteByIds`、`countAll` 等泛型默认方法需要创建具体 Criteria，又不能恢复反射式 `criteriaClass()`。 | `ICrudService` 声明未标注入口注解的 `C newCriteria();`；生成的 `IXxxService` 提供 `default XxxCriteria newCriteria() { return new XxxCriteria(); }`。 |
| `requireUpdate(E entity, C criteria)` | 默认实现 | 合并两个 `OrException` 更新方法。 | 调用 `update(entity, criteria)`；影响数量为 `0` 时抛 `AbsentException`。 |
| `CursorPage<E, PK>` | 返回类型 | 表达无总数查询的游标结果。 | 包含 `records`、`nextCursorId` 和 `hasMore`。 |
| `Page<E>` | 返回类型 | 普通分页结果不能让应用层、远程入口和 OpenAPI 强耦合 MyBatis-Plus。 | 包含 `records`、`pageNum`、`pageSize`、`total` 和 `pageCount`；ORM 分页对象只存在于基础设施内部。 |
| `SuperCudReq<E, C>` | 请求实体 Record | 取代旧 `BatchSave`，以同一批次统一匹配规则组合标准写能力。 | 包含 `addEntities`、`addByFields`、`updateEntities`、`updateCriteria`、`deleteIds`、`deleteCriteria`；Builder 支持 getter Lambda，传输仍使用属性名字符串。 |
| `SuperCudResult` | 返回类型 Record | 大批量保存无需回传实体副本或逐项结果。 | 只包含 `addedCount`、`updatedCount`、`deletedCount` 三个汇总影响数量。 |
| `NullValueMode` | 更新策略 | 取代独立 `updateAllColumnsById` 入口，明确普通字段的 `null` 写入语义。 | `IGNORE_NULL` 为默认值，`WRITE_NULL` 表示把请求中显式提交的 `null` 写入数据库；由 `IUpdateCriteria.nullValueMode` 传递。 |

升级后的 `ICrudService` 正式入口共八个：`createBatch`、`superCud`、`deleteBatch`、`updateBatch`、`page`、
`cursorPage`、`count` 和 `exists`。`create`、`createIfAbsent`、`delete` 等其他保留方法均为未标注
`@EntrypointOperation` 的 Java 默认方法。

### 6.3 非 CRUD 成员的处理

当前 `IService` 中的元数据和反射成员不继续暴露为业务服务方法：

| 方法 | 处理 | 原因 | 关键实现 |
| --- | --- | --- | --- |
| `getOrder()` | 删除 | 新架构不再通过多个 `IService` 的顺序选择实现。 | 本地实现固定优先；远程顺序由 `EntrypointTransportProvider` 和 `transport-order` 配置管理。 |
| `static resolveServiceTypeArgs(Class<?>)` | 删除 | 泛型解析属于 registry 基础设施，不属于业务接口。 | 移入 `isass-entrypoint-registry` 的 `EntrypointTypeResolver`。 |
| `private serviceTypeArgs()` | 删除 | 它只服务于接口内反射元数据方法，迁移后没有保留价值。 | registry 启动时一次解析并缓存到 `CrudServiceDefinition`。 |
| `entityClass()` | 删除 | 实体类型是注册表元数据，不应通过业务代理调用。 | `CrudServiceDefinition.entityType`。 |
| `criteriaClass()` | 删除 | Criteria 类型是注册表元数据；默认方法改用生成的 `newCriteria()`。 | `CrudServiceDefinition.criteriaType`；Java 便捷方法使用 `newCriteria()`。 |
| `entity()` | 删除 | 名称推断不稳定，并与非实体资源语义冲突。 | 由 `@EntrypointInfo.resourceName` 代替。 |
| `service()` | 删除 | 不能继续依赖包路径推断微服务。 | 由 `@EntrypointInfo.serviceName` 代替。 |

### 6.4 其他接口和迁移建议

- `IService` 重命名为 `ICrudService`；正式发布前删除旧接口，不同时注册新旧路由；
- `ILocalService` 重命名为 `ILocalCrudService`，连接统一写/查询执行器和 Repository，并提供八个正式
  CRUD 入口；
- 删除 `IServiceManager`。一个 Entrypoint 只能有一个权威本地实现；远程协议选择由统一代理和传输提供者
  负责，不能继续按多个 Service 的 `getOrder()` 逐个委托；
- `IRepository` 保留本地持久化能力，可以包含批次大小、Wrapper 和数据库专用优化，但绝不生成远程入口；
- `BatchSave` 被请求实体 Record `SuperCudReq` 取代；新增、修改分别使用一套批次级匹配规则，结果使用仅含
  三类汇总影响数量的 `SuperCudResult`，迁移后删除旧类型；
- `SuperCudReq` 与旧 `BatchSave` 一样放在 `isass-nocode-core` 的 `nocode.entity` 包，作为可传输的请求
  实体；它不实现持久化 `IEntity`，不对应数据库表，也不是领域聚合；
- `SuperCudReq` 允许所有分组都为空。空变更集是合法的幂等 no-op，返回三个零值计数，便于协作编辑器在
  没有净变更时仍安全执行“保存”；
- 保留基于 Java 属性名的 `createIfAbsent`，Java Builder 可使用 getter Lambda；框架说明非唯一字段和并发
  重复风险，但不在代码层强制唯一索引；`AddOrUpdate` 仍不属于标准 CRUD；
- `superCud` 不直接把事务、权限和生命周期散落在 Service 默认方法中。`ILocalCrudService` 的
  `superCud` 默认实现调用独立 Spring Bean `CrudWriteExecutor.superCud(service, req)`；事务、数据
  权限、关联写入、CRUD 生命周期、审计和事件统一位于该执行器；
- `page/cursorPage/count/exists` 统一构造 `CrudQueryReq` 并调用独立 Spring Bean
  `CrudQueryExecutor.query(service, req)`；查询条件复制、数据范围、关联装配、观测和失败回调统一位于
  该执行器；
- 不能只给 Service 的 `superCud` 方法配置 Spring AOP 后依赖 `this.superCud(...)` 自调用，因为同一
  Bean 内部调用会绕过代理。需要切面的能力应拦截独立 `CrudWriteExecutor`，或直接成为执行器内部步骤；
- 写/查询生命周期监听器都作为 Spring Bean 自动收集，不允许使用静态注册表；写生命周期区分事务内
  `beforeExecute/afterExecute` 与真实事务完成后的 `afterCommit/afterRollback`；
- `deleteBatch`、无 ID 的 `updateBatch` 必须要求至少一个有效 Where 条件；真正全表操作只能使用受控的
  管理应用服务；
- `count` 返回 `Long`，批量增删改返回影响数量或结构化结果，不再使用无法区分具体结果的统一 `Boolean`；
- `list` 最多返回 `9999` 条，超过上限使用 `page`、`cursorPage` 或导出任务；
- 关联保存、`IUpdateCriteria`、`MERGE/REPLACE`、`NullValueMode`、删除级联和游标分页的执行细节只在
  [NoCode 级联、关联与树形 CRUD 设计](nocode-crud-scenario-implementation.md) 中维护。

### 6.5 代码生成器职责

`isass-nocode-generator` 从 `maven-plugin` 改为普通 `jar`，只保留：

- 领域模型、Criteria、Repository、Mapper 和 CRUD Service 源码生成；
- MyBatis-Plus 元数据、类型转换器、Freemarker 模板和生成器测试；
- 关联、级联和 CRUD 生命周期元数据生成。

删除：

- `generate-nocode-contract`、`GenerateNocodeContractMojo`、QDox `ContractGenerator`；
- `generate-smart-doc-config`、`GenerateSmartDocConfigMojo`、`SmartDocConfigGenerator`；
- 只为上述 Goal 服务的 Maven Plugin、QDox 和其他依赖；
- 业务 POM 中相应执行配置。

不新建 `isass-entrypoint-generator`。入口定义由运行时注解直接解析。当前重构也不提供静态 `.proto`、离线
OpenAPI 或诊断 JSON 导出工具；未来出现明确非 Java 客户端或离线交付需求时，再单独评审显式 CLI，不能
重新绑定 Maven 生命周期。

旧 `META-INF/isass/nocode-contract.json` 没有兼容读取期。HTTP、gRPC、代理和 OpenAPI 全部切换到
`ServiceDefinitionRegistry` 后，在同一变更集删除生成器、加载器、DTO、资源和 POM 配置。应用启动时发现
旧合同资源应明确失败，防止旧业务包产生重复路由。

## 7. API 文档

### 7.1 文档来源

```text
手写 Spring MVC Controller
  -> 各服务 resources/openapi/smart-doc.json
  -> Smart-doc 官方 Maven 插件
  -> META-INF/isass/openapi/{serviceName}/openapi.json

当前进程本地 IEntrypoint
  -> ServiceDefinitionRegistry
  -> isass-apidoc-openapi3 转换为 OpenAPI

两类文档
  -> OpenApiDocumentAssembler 合并
  -> ServiceDocsController 返回唯一 /v3/api-docs
```

registry 只提供协议无关只读元数据，不生成 OpenAPI JSON，也不依赖 OpenAPI 类型。Controller 只负责暴露
端点和缓存最终快照，资源发现、筛选、转换和合并由 `OpenApiDocumentAssembler` 完成。

### 7.2 服务隔离与运行模式

Smart-doc 产物按服务名隔离：

```text
META-INF/isass/openapi/asset-service/openapi.json
META-INF/isass/openapi/bsp-service/openapi.json
META-INF/isass/openapi/user-service/openapi.json
```

运行时通过 `classpath*:META-INF/isass/openapi/**/openapi.json` 枚举：

| 模式 | 静态文档 | Entrypoint |
| --- | --- | --- |
| `isass.boot.microservice.enabled=true` | 只选 `spring.application.name` 对应文档 | 只选同名服务且有本地实现的入口 |
| `isass.boot.microservice.enabled=false` | 合并 classpath 中全部服务文档 | 合并当前进程全部本地入口 |

不增加 `ServiceDocsScopeProvider`。微服务模式缺少 `spring.application.name`、缺少同名文档或发现同服务多份
文档时，应明确报告构建或配置错误。

### 7.3 合并规则

- `HTTP Method + 完整 Path` 冲突时失败，不能静默覆盖；
- Schema 使用稳定的全限定类型标识或等价可重复策略，同名异构时失败；
- Smart-doc 提供手写 Controller，Entrypoint 转换器只追加正式入口；
- Entrypoint 使用 `displayOrder` 排序，手写 Controller 沿用 Smart-doc 顺序；
- 合并完成后统一设置服务名、版本、安全方案、服务器地址和 Knife4j 扩展；
- 生产环境在启动完成后形成不可变快照，开发环境可以在 registry 变化后失效缓存。

## 8. Security 授权服务

### 8.1 公共接口归属

`IAuthorizationService` 及其简单传输类型位于 Security 模块，并直接继承 `IEntrypoint`：

- `ApiKeyAuthenticationRequest`、`ApiKeyAuthenticationResult`；
- `PrincipalAuthorizationContext`；
- `FindAccessibleResourceRequest`、`AuthorizationResource`；
- `FindMenuRequest`、`MenuTree`。

BSP 进程使用本地实现，其他微服务由 Entrypoint registry 注入远程代理；不再保留
`IBspApiKeyAuthenticationService`、`IRoleCodeService`、角色服务 Manager 或 BSP 适配器。

当前 `findAccessibleResources` 返回 BSP 数据库实体 `AuthResource`，需要改为 Security 中的不可变公共类型
`AuthorizationResource`，由 BSP 应用层转换。公共类型不能暴露 BSP 持久化模型。

```java
@EntrypointInfo(
        serviceName = "bsp-service",
        contextName = "auth",
        resourceName = "authorization")
public interface IAuthorizationService extends IEntrypoint {
}
```

BSP 提供唯一权威本地实现 `AuthorizationApplicationService`。BSP 进程直接使用本地 Bean；Asset 等业务
微服务获得自动 HTTP/gRPC 客户端代理，不再编写 `BspRoleCodeService` 等适配器。

### 8.2 统一授权上下文与本地权限解析

运行时不再远程查询“某个 URL 需要哪些角色”。调用主体与入口要求分别从两个稳定来源获得：

```text
JWT 请求
  -> JWT 建立 USER 主体
  -> 首次鉴权时转发同一 JWT 调用 BSP jwtContext
  -> 获得角色和 permissionCodes

API Key 请求
  -> 请求体调用 BSP apiKeyContext 验证完整 API Key
  -> 建立 APPLICATION 主体并携带完整授权上下文

业务请求
  -> 当前进程 EntrypointPermissionResolver 匹配 HTTP Method + Path
  -> DynamicPermissionAuthorizationManager 比较 required permissionCodes
```

确定以下规则：

1. `/bsp-service/auth/authorization/apiKeyContext` 与 `jwtContext` 在 Web Security 层加入 `publicUrls`，但不是
   匿名业务能力；实现方法分别校验完整 API Key，或要求当前请求已经建立 USER 主体；
2. 用户 JWT 向下游传播时不得叠加微服务 API Key，避免一个请求出现两个主体；
3. API Key 放在 `ApiKeyAuthenticationRequest` 请求体中，不放入 URL；
4. 入口所需权限由当前进程本地 `EntrypointPermissionResolver` 提供，只包含当前进程的本地实现；
5. 没有权限映射的普通业务入口默认拒绝，`ROLE_SUPER_DEV` 保留超级开发者旁路；
6. `DynamicPermissionAuthorizationManager` 比较权限编码，不再比较 URL 对应的角色编码；
7. 删除 `findRoleCodesByUri`、`DynamicRoleAuthorizationManager`、旧 URL—角色缓存和内部授权查询角色；
8. 不增加独立的内部服务授权管理器；后续确有服务间业务入口时，按正常应用权限授权。

## 9. 目标依赖关系

```text
isass-entrypoint-core
├── isass-entrypoint-registry
├── isass-entrypoint-http
├── isass-entrypoint-grpc
├── isass-security-*
└── isass-nocode-core
    └── isass-nocode-generator（开发期源码生成）

isass-apidoc-openapi3
├── isass-entrypoint-core
└── isass-entrypoint-registry

bsp-service
└── 实现 Security 定义的 IAuthorizationService
```

必须满足：

- entrypoint core 不依赖 Spring、Security、NoCode、BSP 或数据库；
- Security 不依赖 NoCode；
- NoCode 可以依赖 entrypoint，但 entrypoint 不知道实体和 Criteria；
- registry 不反向依赖 HTTP、gRPC 或 OpenAPI 实现；
- BSP 只负责实现 Security 公共授权入口。

## 10. 实施结果

以下六个阶段已在同一个破坏式变更集中完成。迁移过程中没有保留旧类型别名、旧动态路由、旧合同 JSON
读取或新旧 URL 双注册；BSP 与 Asset 已直接迁移到新接口，以便编译和运行时尽早暴露遗漏。

### 阶段 1：回归基线

- 固化现有本地优先、HTTP/gRPC、文件流、参数、泛型响应和错误映射行为；
- 固化 API Key 跨微服务认证；
- 增加授权角色查询失败的回归用例；
- 固化旧合同、旧 Path 参数和关联查询行为，仅用于迁移对照；
- 增加默认方法、重复 Query 参数、游标分页和禁止跨协议重试测试。

### 阶段 2：建立 Entrypoint 模块

- 新建 core、registry、http、grpc 四个模块；
- 实现注解解析、继承方法解析、元数据校验和本地/远程代理；
- 实现 HTTP/gRPC 服务端和客户端传输；
- 实现全局及服务级传输选择。

### 阶段 3：切换注册表和 API 文档

- 将 HTTP、gRPC、代理、鉴权和 OpenAPI 全部切换到 `ServiceDefinitionRegistry`；
- 各服务使用 Smart-doc 官方插件并生成按服务隔离的 OpenAPI；
- 实现运行模式筛选和唯一 `/v3/api-docs`；
- 同一变更集删除旧合同生成、加载、资源和 POM 配置；
- 将 `isass-nocode-generator` 改为只负责源码生成的普通 `jar`。

### 阶段 4：迁移 NoCode

- `IService` 改名为 `ICrudService`，不保留双运行时入口；
- 按第 6 章方法清单只发布八个正式 CRUD 入口，其余方法迁移为 Java 默认实现或删除；
- `batchSave` 升级为超级增删改事务入口 `superCud`，增加 `SuperCudReq/SuperCudResult`；
- 增加 `newCriteria()` 生成实现、`NullValueMode` 和基于实体字段的 `createIfAbsent`，删除旧的独立
  `IfAbsent` 及通用 `AddOrUpdate`；
- `ILocalService` 改名为 `ILocalCrudService`，删除 `IServiceManager` 和基于 `getOrder()` 的多实现委托；
- 新增独立 `CrudWriteExecutor` 和 `CrudQueryExecutor`，让全部写方法经过同一个事务与治理执行器，全部
  查询方法经过同一个查询治理执行器；
- 修改生成器模板并机械迁移消费端；
- 模型迁移到 `domain.model`，删除 `XxxAgg`；
- 路径迁移到固定 NoCode 命名空间，移除业务 Path 参数；
- 实施关联、级联、更新模式和游标分页专项方案。

实现中未引入源码过渡别名，也未同时注册新旧 URL、合同或远程操作。

### 阶段 5：迁移 Security

- 迁移 `IAuthorizationService`、统一授权上下文和公共传输类型；
- 新增 `AuthorizationResource`、`ApiKeyAuthenticationRequest/Result` 和 `PrincipalAuthorizationContext`；
- 使用本地 `EntrypointPermissionResolver` 提供入口—权限映射；
- 删除 `findRoleCodesByUri`、旧 URL—角色链路和一次性授权查询适配器；
- 验证 JWT 与 API Key 最终都建立唯一主体并获得完整权限编码。

### 阶段 6：真实业务验证

- 使用 OpenClaw 服务账号调用 Asset；
- 验证 API Key 身份和业务角色；
- 验证 Asset 使用当前用户 JWT 或 API Key 授权上下文完成权限判断；
- 验证腾讯提示词迁移权限、标签读取和写入不再返回 `403`；
- 验证 BSP 本地调用不经过远程代理。

## 11. 风险与验证重点

- 未标注 `@EntrypointOperation` 的默认方法必须由客户端代理本地执行；带入口注解的默认方法必须发起
  远程请求，不能因其具有默认方法体而在客户端执行；
- BeanDefinition 注册顺序不能让远程代理覆盖本地实现；
- 请求发出后的超时不能跨协议重试非幂等操作；
- Query 对象重名、数组编码、文件流和泛型响应需要往返测试；
- OpenAPI 合并必须检测路径和 Schema 冲突；
- 生成器生成代码时，必须要防止业务代码被覆盖后无法恢复，运行代码生成器前，微服务必须先提交并推送一次代码；
- `apiKeyContext` 与 `jwtContext` 虽由 Web Security 放行，实现内部仍必须验证凭证或 USER 主体；
- JWT 下游传播不得附加 API Key，且一个请求只能形成一个确定主体；
- 本地入口缺少权限映射时必须默认拒绝；
- 关联写入、`REPLACE`、乐观锁、越权 ID、事务回滚和循环展开按专项文档测试。

## 12. 验收标准

- 普通应用服务和 Security 服务不依赖 NoCode 即可获得本地、HTTP 和 gRPC 能力；
- `ICrudService` 是 NoCode 唯一类型标识，标准入口和自定义入口不能混用；
- URL、操作、参数和显示信息全部来自运行时 Java 注解；
- NoCode 路径固定且没有业务 Path 参数；
- NoCode 初始化数据使用 `/{serviceName}/nocode/system/initialization/{operationName}` 基础设施路径，
  由运行时 Entrypoint 元数据识别实体归属，不读取旧合同；
- 未标注的方法不生成路由或 OpenAPI；
- 正式 CRUD 入口只有 `createBatch`、`superCud`、`deleteBatch`、`updateBatch`、`page`、`cursorPage`、
  `count` 和 `exists`；
- `getById/getOne/list/requireOne/existsById/update/requireUpdate/requireExists/requireAbsent` 等便捷能力在
  客户端执行默认实现，并只调用上述正式入口；
- `createIfAbsent` 接受 Java 属性名或 getter Lambda；业务负责选择唯一字段并按并发要求建立唯一索引，
  框架不在代码层强制索引；旧的独立 `IfAbsent`、通用 `AddOrUpdate` 和字符串属性查询不再属于标准 CRUD；
- `create`、`createIfAbsent`、`update` 和 `delete` 是未标注入口注解的 Java 默认方法；`createBatch`、
  `updateBatch` 和 `deleteBatch` 是正式专项入口；它们都构造 `SuperCudReq` 并调用 `superCud`；
- `superCud` 通过独立 `CrudWriteExecutor` 在一个本地事务中完成全部写入并返回三类汇总数量，不依赖
  Service 自调用触发 Spring AOP；
- `page/cursorPage/count/exists` 通过独立 `CrudQueryExecutor` 执行，`getById/getOne/list/requireOne`
  等便捷方法复用正式查询入口；
- `CrudWriteLifecycleListener` 和 `CrudQueryLifecycleListener` 由 Spring 自动收集；同一 Service 重入时
  不重复触发生命周期，跨 Service 调用仍执行被调用服务的完整生命周期；
- `IUpdateCriteria` 分别使用 `updateMode` 控制关联合并、`nullValueMode` 控制普通字段空值写入；
- HTTP/gRPC/代理/OpenAPI 不再读取 `nocode-contract.json`；
- 四个 Entrypoint 模块职责清晰且无循环依赖；
- 本地实现优先，远程传输按全局或服务级顺序选择，请求发出后不跨协议重试；
- Smart-doc 和本地 Entrypoint 合并为唯一 `/v3/api-docs`；
- `isass-nocode-generator` 只负责 NoCode 源码生成；
- 生成模型位于 `domain.model`，不再生成空 `XxxAgg`；
- 数据库无外键，关联和树形行为符合专项文档；
- `IAuthorizationService` 公共模型不暴露 BSP 数据库实体；
- `findRoleCodesByUri`、旧 URL—角色缓存与 `DynamicRoleAuthorizationManager` 已删除；
- OpenClaw 真实业务请求能够完成 API Key 身份认证、权限编码判断和 NoCode 数据鉴权。
