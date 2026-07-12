# super core

## 更新日志

### 4.0.0

#### feat

- **OpenAPI 增强 SPI**：`isass-web-springmvc` 新增 `OpenApiEnhancerSpi`，`ServiceDocsController` 在增强器存在时输出运行时增强文档，不存在时保持原始 smart-doc 输出。
- **架构升级**：支持 Maven 4 风格构建，引入 `root="true"` 属性和 `modelVersion 4.1.0`。
- **模块重构**：新增 `isass-framework-dependencies` (BOM) 和 `isass-framework-parent` (Parent POM)，提供更加灵活的项目继承与依赖管理方式。
- **包名重构**：全量迁移包名，从 `vip.isass.core` 和 `vip.isass.kernel` 统一变更为 `vip.isass.framework`，保持与框架命名的统一。
- **Jakarta EE 适配**：全面适配 Jakarta EE 10，切换相关注解包名。
- **Spring Boot 升级**：深度适配自定义 Spring Boot 4.0.5 版本，将所有自动配置从 `spring.factories` 迁移至新的 `AutoConfiguration.imports` 机制。
- **MQ 系统重构**：
    - 引入新的 `IMqConsumer` 接口和 `MqMessageContext`。
    - 实现 MQ 消费者生命周期的全自动托管，移除冗余的初始化模板代码。
    - 优化配置项，统一使用 `mq.` 前缀。
- **MQ 多源重构**：
    - 引入 `DynamicMqProperties`、`MqSourceProperties`、`IMqFactory`、`IMqProducer`、`IMqMessageHandler` 和 `MqManager`。
    - 新增 `MqPublisher`，支持默认 `primary` 源和显式 source 发送。
    - Spring Event 与 Kafka 0.11 接入 source-oriented factory 模型，业务代码无需依赖具体 MQ 产品。
    - MQ 源必须显式配置 `type`，移除 `factory-class` 和 source 名称自动匹配。
    - 新增 `isass-mq-redisstream` 与 `isass-mq-redispubsub` 模块，直接基于 Redisson 实现 Redis Stream 和 Redis Pub/Sub MQ 源。
- **健康检查优化**：适配 Spring Boot 4.x 的模块化健康检查体系，更新 `HealthIndicator` 相关实现。
- **数据库初始化优化**：回归 `v4-old` 的 `DatabaseInitializerManager` 过程式逻辑，移除 `DatabaseInitializer` 接口及 SPI 扩展机制，统一管理各数据库方言。
- **数据库迁移工具替换**：将 Flyway 替换为 Liquibase。
    - `isass-database-core` 移除 Flyway 依赖，保留 Liquibase 服务级命名规则。
    - Spring Boot Liquibase 桥接由 `isass-adapter-springboot` 提供，业务服务按需依赖 `spring-boot-starter-liquibase`。
    - 新增 `LiquibaseServiceNaming` 服务级命名规则，Spring Boot adapter 提供 `AbstractLiquibaseConfiguration` 抽象配置基类与 `LiquibaseConfigurer` 配置工具类。
    - 服务侧通过声明独立 `SpringLiquibase` bean 实现微服务/单体两种启动模式。
    - 单体模式下多个服务 bean 在 Spring 初始化阶段分别执行，changelog 与 Liquibase 管理表通过服务名隔离。
- **达梦 Liquibase 兼容**：新增 `isass-database-dameng` 模块，引入 `com.github.mengweijin:db-migration-dameng-liquibase`，使用开源扩展支持达梦数据库，并将 Liquibase 版本锁定为 `5.0.3`。
- **模块合并**：
    - `isass-framework-nocode-*` 核心功能合并至 `isass-framework-common` 的 `core.structure` 目录下。
    - `isass-framework-database-mybatisplus-mysql` 和 `postgresql` 合并至 `isass-framework-database-mybatisplus`，支持多数据库自动配置。
    - 移除了冗余的 `isass-framework-database-mysql` 和 `isass-framework-database-postgresql` 包装模块，改为直接引用 JDBC 驱动。
    - `isass-framework-mq-redisstream` 和 `redispubsub` 合并至 `isass-framework-database-redis`。
    - `isass-framework-build` 模块保留并持续优化（曾短暂更名为 `isass-framework-deploy`）。

#### optimize

- 优化 `IV2LocalService` 接口，增加 `getService()` 必需方法以提升类型安全性。
- 统一 Maven 编译配置，由父 POM 统一管控 `maven.compiler.release` (Java 25)。
- 清理根项目 POM，使其专注于模块聚合管理。

#### migrate

- **Jackson 3.x 迁移**：将自定义代码从 Jackson 2.x (`com.fasterxml.jackson.*`) 迁移至 Jackson 3.x (`tools.jackson.*`)：
    - `JsonUtil`：重写 `ObjectMapper` 构造为 `JsonMapper.builder()` 构建器模式；`Feature` 枚举替换为 `JsonReadFeature`/`StreamWriteFeature`；`JsonProcessingException` → `JacksonException`；`jsonNode.elements()` → `jsonNode.iterator()`。
    - 序列化器/反序列化器：`JsonSerializer` → `ValueSerializer`，`JsonDeserializer` → `ValueDeserializer`，`SerializerProvider` → `SerializationContext`，移除 `throws IOException`。
    - 转换器：`StdConverter` 导入路径迁移至 `tools.jackson.databind.util.StdConverter`。
    - MyBatis-Plus 集成：`JacksonTypeHandler` → `Jackson3TypeHandler` 适配 Jackson 3 ObjectMapper；`CreatorProperty` 构造器迁移至 `CreatorProperty.construct()` 工厂方法；`ValueInstantiators.findValueInstantiator` 适配新签名（`BeanDescription.Supplier` + `modifyValueInstantiator`）。
    - 保留 `com.fasterxml.jackson.core:jackson-databind:2.21.2` 编译依赖，用于 Spring Data Redis 等第三方库的向后兼容；在 `JsonUtil` 中新增 `LEGACY_MAPPER`（Jackson 2.x ObjectMapper）供旧 API 调用。

#### test

- `ExceptionAdvice` 集成测试扩展至 19 个用例，覆盖 UnifiedException（有/无 status、有/无 cause）、core 映射（IllegalArgumentException/AbsentException/AlreadyPresentException/UnsupportedOperationException/IOException/FileNotFoundException/DateTimeException）、未映射异常回退 UNDEFINED、showDetailError 开关控制。验证：`mvn -pl isass-web-springmvc -am test -Dtest=ExceptionAdviceTest -Dmaven.javadoc.skip=true`。
- `StatusMessageEnum` 评估结论：JWT_TOKEN_ERROR/UN_LOGIN/TOKEN_EXPIRED/TOKEN_ILLEGAL 因 core-common（JwtUtil/LoginUserUtil）和 web-springmvc（IsassErrorController）跨模块引用保留，符合 `docs/design/exception-code-architecture.md` 设计约束，无需拆分。

#### docs

- **零代码文档校正**：`smart-doc` 使用说明改为无版本 nocode 合同、`IXxxService` 与统一动态 HTTP 路由；移除将 OpenAPI 地址 `/v3/api-docs` 误解为 nocode 接口版本的表述。
- **前端零代码说明**：新增 `docs/usage/nocode/frontend-api-usage.md`，说明标准 CRUD 路径、参数绑定、文件流、自定义业务接口与高级响应投影。
- 在 README 中补充模块命名规范，明确 `isass-分类-模块名` 格式。

#### refactor

- **移除旧 EDB 双实体模型**：删除未被使用的 `IDbEntity`、`DbEntityConvert` 及 `WrapperUtil` 的 EDB QueryWrapper 方法；当前 nocode 实体直接作为 MyBatis-Plus 实体持久化。
- **单一零代码体系**：移除无版本历史 nocode 与 V2 实现，将原 V3 合同、实体、Criteria、ORM、HTTP/gRPC transport、生成器统一为无版本 `isass-nocode-*` API；标准 HTTP 路由收敛为 `/{serviceName}/{entityName}`，自定义业务方法保留所属服务的确定路径。
- **合同与文档**：构建期资源统一为 `META-INF/isass/nocode-contract.json` 与 `*-nocode.proto`；OpenAPI 投影统一使用“零代码接口”分组，不再保留 V3 路径、类型或模板命名。

- **V3 文件流传输重构**：`V3FileStream` 改为以 `writeTo(OutputStream)` 为主的单次消费数据源；HTTP 适配器使用 `StreamingResponseBody` 直接消费存储源流，不再通过虚拟线程与 Pipe 中转。业务代码可按需通过 `openInputStream()` 获得读取式兼容流。
  - V3 文件端点在响应提交前直接返回空响应体的 HTTP 状态：文件不存在为 `404`，参数错误为 `400`，服务器异常为 `5xx`，不再包装为 `Resp` JSON。
  - 新增 `V3FileNotFoundException` 作为传输无关的文件资源不存在语义；传输已开始后的异常保留服务端日志并中断响应，避免写入无效 JSON。
  - `isass-nocode-grpc` 新增 V3 文件 server-streaming：首帧传输文件元数据，后续以 64 KiB 原始字节块发送；客户端恢复为 `V3FileStream`，避免文件内容聚合为 `byte[]` 或 JSON/Base64。
- **动态 V3 transport 与文档链路**：`IV3XxxService` 是唯一业务契约；构建期生成 `v3-contract.json` 和 proto，运行时由单个 HTTP/gRPC 适配器暴露全部接口，不再生成实体 V3 Controller。
  - OpenAPI 增强器根据契约生成命名 Schema、Javadoc 字段描述、统一 path、请求/响应 `oneOf` 和 Criteria 映射。
  - 业务调用优先级为本地实现、gRPC、HTTP；非幂等请求发出后禁止跨协议重试。
  - 增强结果以双检锁懒加载缓存；`ServiceDocsController` 通过 `OpenApiEnhancerSpi` 可选接入，未安装文档服务时保持原始输出。
  - 新增 `/v3/api-docs/swagger-config` 双模式分组，以及 `/doc.html`、`/services/{serviceName}/doc.html` Knife4j UI 路由。

#### fix

- **V3 逻辑删除元数据修正**：`V3TableMetaRegistrar` 运行时识别逻辑删除字段后，补齐 MyBatis-Plus `TableFieldInfo` 的逻辑删除标记及未删除/已删除值 `0/1`，避免查询错误生成 `delete_flag = null`。
- **V3 表元数据注册时机修正**：`V3TableMetaRegistrar` 的 `populate()` 仅在 `v3ServiceRegistry` Bean 构造时联动触发，与 MyBatis-Plus `SqlSessionFactory` 构建 `TableInfo` 的顺序无任何约束，导致 MP 实际查询时回调 `postTableInfo` 的 `metaMap` 仍为空，最终实体类名直接转表名（如 `V3Icon` → `v3_icon`），抛出 `Table 'attachment.v3_icon' doesn't exist`。
  - `V3TableMetaRegistrar` 改为实现 `BeanDefinitionRegistryPostProcessor`，在 BDRPP 阶段（任何业务 Bean 实例化之前）通过 `ClassPathScanningCandidateComponentProvider` + `AssignableTypeFilter(IV3Entity.class)` 扫描 `vip.isass` 包下所有 `IV3Entity` 实现类，按接口契约（`IV3IdEntity`/`IV3TraceEntity`/`IV3LogicDeleteEntity`/`IV3VersionEntity`/`IV3TenantEntity`/`IV3ParentIdEntity`）镜像出表元数据，保证 MP `TableInfo` 构建时元数据已就绪。
  - 表名解析优先级调整为：1) 实体类上的 `@TableName`；2) 实体覆盖的 `IV3Entity#tableName()`；3) `V3TablePrefixUtil` 注册前缀 + `StrUtil.toUnderlineCase(entityName)`（兜底，并修复多词实体名 `iconGroup` 之前未转 `icon_group` 的隐患）。
  - `entity.java.ftl` 模板新增 `@Override tableName()` 返回 `${table.name}` 字面量，api 模块实体无需任何 MyBatis-Plus 注解即可被 MP 正确识别。
  - `V3AutoConfiguration` 移除 `V3TableMetaRegistrar.populate(registry)` 静态联动调用，避免与 `SqlSessionFactory` 创建时序耦合。
  - 同步手工补全 `isass-service-attachment` 的现有 `V3Icon` / `V3IconGroup` 实体的 `tableName()` 覆盖（重新跑生成器时由新模板自动生成）。
