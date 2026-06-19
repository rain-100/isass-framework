# super core

## 更新日志

### 4.0.0

#### feat

- **架构升级**：支持 Maven 4 风格构建，引入 `root="true"` 属性和 `modelVersion 4.1.0`。
- **模块重构**：新增 `isass-framework-dependencies` (BOM) 和 `isass-framework-parent` (Parent POM)，提供更加灵活的项目继承与依赖管理方式。
- **包名重构**：全量迁移包名，从 `vip.isass.core` 和 `vip.isass.kernel` 统一变更为 `vip.isass.framework`，保持与框架命名的统一。
- **Jakarta EE 适配**：全面适配 Jakarta EE 10，切换相关注解包名。
- **Spring Boot 升级**：深度适配自定义 Spring Boot 4.0.5 版本，将所有自动配置从 `spring.factories` 迁移至新的 `AutoConfiguration.imports` 机制。
- **MQ 系统重构**：
    - 引入新的 `IMqConsumer` 接口和 `MqMessageContext`。
    - 实现 MQ 消费者生命周期的全自动托管，移除冗余的初始化模板代码。
    - 优化配置项，统一使用 `mq.` 前缀。
- **健康检查优化**：适配 Spring Boot 4.x 的模块化健康检查体系，更新 `HealthIndicator` 相关实现。
- **数据库初始化优化**：回归 `v4-old` 的 `DatabaseInitializerManager` 过程式逻辑，移除 `DatabaseInitializer` 接口及 SPI 扩展机制，统一管理各数据库方言。
- **数据库迁移工具替换**：将 Flyway 替换为 Liquibase。
    - `isass-database-core` 移除 Flyway 依赖，改为依赖 `spring-boot-starter-liquibase` 与 `liquibase-core`。
    - 新增 `AbstractLiquibaseConfiguration` 抽象配置基类与 `LiquibaseConfigurer` 配置工具类。
    - 服务侧通过声明独立 `SpringLiquibase` bean 实现微服务/单体两种启动模式。
    - 单体模式下多个服务 bean 在 Spring 初始化阶段分别执行，changelog 与 Liquibase 管理表通过服务名隔离。
- **达梦 Liquibase 兼容**：引入 `com.github.mengweijin:db-migration-dameng-liquibase`，使用开源扩展支持达梦数据库，并将 Liquibase 版本锁定为 `5.0.3`。
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

#### docs

- 在 README 中补充模块命名规范，明确 `isass-分类-模块名` 格式。
