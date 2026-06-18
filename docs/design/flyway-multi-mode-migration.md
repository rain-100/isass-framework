# Flyway 多模式迁移设计文档

## 1. 背景与问题

在 Spring Boot 4.x 微服务架构中，每个服务独立管理自己的数据库 schema 版本。但在 **单体部署** 场景下，多个服务共享同一个 JVM 进程和同一个 DataSource，必须确保：

- 各服务的 Flyway 迁移文件（SQL）互不干扰
- 各服务的 schema history 表相互隔离
- 所有 Flyway bean 都被正确触发迁移

## 2. 核心设计

### 2.1 服务名隔离

每个服务在 `application.yml` 中定义自己的 Flyway 迁移路径和 schema history 表名，通过 **服务名前缀** 实现隔离：

| 服务 | 迁移文件位置 | 历史表名 |
|------|-------------|---------|
| attachment | `classpath:db/mysql/attachment/` | `attachment_flyway_schema_history` |
| user | `classpath:db/mysql/user/` | `user_flyway_schema_history` |

实现方式：`AbstractFlywayConfiguration.getServiceName()` 返回服务名，`createFlyway()` 将其拼接进 locations 和 table。

### 2.2 类结构

```
┌──────────────────────────────────────────────────┐
│              FlywayConfigurer (工具类)              │
│  PropertyMapper 将 FlywayProperties ~50 属性全量映射 │
│  到 FluentConfiguration                           │
└──────────────────────┬───────────────────────────┘
                       │ 调用
                       ▼
┌──────────────────────────────────────────────────┐
│         AbstractFlywayConfiguration (抽象基类)      │
│  createFlyway():                                  │
│  1. FlywayConfigurer.configure() 应用全量属性       │
│  2. 覆盖 locations → 追加 getServiceName()         │
│  3. 覆盖 table → 前缀 getServiceName()             │
 │  4. 清理模式：由 FlywayProperties 控制（clean-disabled /           │
 │     clean-on-validation-error，框架不硬编码）
│  5. 达蒙驱动检测                                   │
│  6. 应用 FlywayConfigurationCustomizer 列表         │
└──────────────────────┬───────────────────────────┘
                       │ 继承
                       ▼
┌──────────────────────────────────────────────────┐
│       XxxServiceFlywayConfiguration (服务侧)       │
│  @Bean Flyway xxxFlyway()                         │
│  注入 ObjectProvider<FlywayConfigurationCustomizer>│
└──────────────────────┬───────────────────────────┘
                       │ 注册
                       ▼
┌──────────────────────────────────────────────────┐
│        IsassFlywayAutoConfiguration               │
│  @Bean IsassFlywayMigrationInitializer            │
│  收集所有 Flyway bean → 逐一 migrate()             │
└──────────────────────────────────────────────────┘
```

### 2.3 关键类说明

| 类 | 所在模块 | 职责 |
|----|---------|------|
| `FlywayConfigurer` | `isass-database-core` | 工具类，用 `PropertyMapper` 将 `FlywayProperties` 全量映射到 `FluentConfiguration`（复用 SB4 相同的属性映射逻辑） |
| `AbstractFlywayConfiguration` | `isass-database-core` | 抽象基类，提供 `createFlyway()` 模板方法，实现服务名隔离 |
| `AttachmentServiceFlywayConfiguration` | 服务项目 | 具体子类，定义 `@Bean Flyway attachmentFlyway`，指定服务名 `"attachment"` |
| `IsassFlywayAutoConfiguration` | `isass-database-core` | 框架自动配置，创建 `IsassFlywayMigrationInitializer` |
| `IsassFlywayMigrationInitializer` | `isass-database-core` | 继承 SB4 的 `FlywayMigrationInitializer`，支持多个 Flyway bean 依次迁移 |

### 2.4 FlywayProperties 全量映射

`FlywayConfigurer` 解决了传统手动创建 Flyway bean 时 **大量属性丢失** 的问题。SB4 的 `FlywayAutoConfiguration` 内部使用 `PropertyMapper` 映射约 50 个属性到 `FluentConfiguration`，但该逻辑封装在私有内部类中无法直接复用。

`FlywayConfigurer` 采用相同的 `PropertyMapper` 模式，确保在自定义 bean 中也能获得与 SB4 自动配置完全一致的属性映射行为。

## 3. 微服务模式

```
┌──────────────────────────────────┐
│     attachment-service (JVM)      │
│                                   │
│  application.yml:                 │
│    spring.flyway.locations:       │
│      classpath:db/mysql           │
│    spring.flyway.table:           │
│      flyway_schema_history        │
│                                   │
│  @Bean Flyway attachmentFlyway    │
│    locations → db/mysql/attachment│
│    table → attachment_flyway_... │
│                                   │
│  IsassFlywayMigrationInitializer  │
│    → attachmentFlyway.migrate()   │
└──────────────────────────────────┘
```

- 单个 Spring Boot 进程，单个 DataSource
- 只有一个 `@Bean Flyway`
- 服务名隔离后 locations/table 唯一
- SB4 的 `FlywayAutoConfiguration.FlywayConfiguration.flyway()` 被自定义 `@Bean` 抑制

## 4. 单体模式

```
┌───────────────────────────────────────────────────┐
│              monolithic-app (JVM)                   │
│                                                     │
│  ┌──────────────────┐  ┌──────────────────┐         │
│  │ attachment-service│  │   user-service   │  ...   │
│  │                   │  │                  │         │
│  │ @Bean Flyway     │  │ @Bean Flyway     │         │
│  │  attachmentFlyway│  │  userFlyway      │         │
│  │  locations:      │  │  locations:      │         │
│  │   /attachment    │  │   /user          │         │
│  │  table:          │  │  table:          │         │
│  │   attachment_... │  │   user_...       │         │
│  └────────┬─────────┘  └────────┬────────┘         │
│           │                     │                  │
│           ▼                     ▼                  │
│  ┌─────────────────────────────────────────────┐   │
│  │        IsassFlywayMigrationInitializer        │   │
│  │  flyways.forEach(flyway -> flyway.migrate())  │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│        DataSource (共享，但表空间隔离)               │
└───────────────────────────────────────────────────┘
```

- 多个服务共享同一个 JVM，同一个 DataSource
- 每个服务模块都贡献自己的 `@Bean Flyway`
- `IsassFlywayAutoConfiguration` 通过 `List<Flyway>` 注入收集所有 bean
- `IsassFlywayMigrationInitializer` 逐一执行 `flyway.migrate()`

## 5. 与 SB4 FlywayAutoConfiguration 的关系

| 组件 | SB4 默认行为 | 框架行为 |
|------|-------------|---------|
| `FlywayAutoConfiguration` 外层 | 激活（若条件满足） | 激活，`StringOrNumberToMigrationVersionConverter` 仍由 SB4 提供 |
| `FlywayConfiguration.flyway()` | 创建默认 Flyway bean | 被自定义 `@Bean` 抑制（`@ConditionalOnMissingBean(Flyway.class)`） |
| `FlywayMigrationInitializer` | 由 SB4 创建 | 由 `IsassFlywayAutoConfiguration` 创建的 `IsassFlywayMigrationInitializer` 替代，支持多 Flyway bean |
| `FlywayConfigurationCustomizer` | 用于供应商扩展（Oracle/PostgreSQL/SQL Server） | 服务可以通过 `FlywayConfigurationCustomizer` bean 进一步自定义 FluentConfiguration |

## 6. 初始化顺序

```
1. DataSourceAutoConfiguration
   → DataSource bean 创建

2. DatabaseAutoConfiguration
   → 注册 FlywayProperties（@EnableConfigurationProperties）

3. AttachmentServiceFlywayConfiguration
   → @Bean Flyway attachmentFlyway
   → 使用 FlywayConfigurer 应用全量属性
   → 服务名隔离（locations/table）
   → 应用所有 FlywayConfigurationCustomizer beans

4. IsassFlywayAutoConfiguration
   → @Bean IsassFlywayMigrationInitializer
   → 注入 List<Flyway>（包含所有服务的 Flyway bean）
   → afterPropertiesSet() → 逐一 migrate()
```

## 7. 数据库类型支持

框架通过不同的 mybatisplus 子模块支持多数据库：

| 数据库 | 模块 | 驱动 |
|--------|------|------|
| MySQL | `isass-database-mybatisplus` (mysql 子包) | `com.mysql.cj.jdbc.Driver` |
| PostgreSQL | `isass-database-mybatisplus` (postgresql 子包) | `org.postgresql.Driver` |
| Dameng | `isass-database-core` (optional) | `dm.jdbc.driver.DmDriver` |

Flyway 迁移文件目录遵循 `classpath:db/{dbType}/{serviceName}/` 模式，通过 `spring.flyway.locations` 配置驱动决定。
