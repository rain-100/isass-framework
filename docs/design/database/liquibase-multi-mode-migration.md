# Liquibase 多模式迁移设计文档

## 1. 背景与问题

在 Spring Boot 4.x 微服务架构中，每个服务可以独立管理自己的数据库变更；在单体部署场景下，多个服务共享同一个 JVM 进程和同一个 DataSource，必须确保：

- 各服务的 Liquibase changelog 互不干扰
- 各服务的 `DATABASECHANGELOG` 与 `DATABASECHANGELOGLOCK` 表相互隔离
- 微服务模式只执行当前服务的 changelog
- 单体模式执行被扫描到的所有服务 changelog

## 2. 核心设计

每个服务定义一个 `SpringLiquibase` bean。Spring Boot adapter 提供 `AbstractLiquibaseConfiguration` 作为服务侧配置基类，database-core 提供纯 Java `LiquibaseServiceNaming`，统一完成：

- `spring.liquibase.*` 标准属性映射
- changelog 路径追加服务名
- Liquibase history/lock 表追加服务名前缀
- 复用 `SpringLiquibase` 自身的初始化生命周期执行迁移

## 3. 类结构

```
┌──────────────────────────────────────────────────┐
│             LiquibaseConfigurer                   │
│  将 LiquibaseProperties 映射到 SpringLiquibase      │
└──────────────────────┬───────────────────────────┘
                       │ 调用
                       ▼
┌──────────────────────────────────────────────────┐
│       AbstractLiquibaseConfiguration              │
│  createLiquibase():                               │
│  1. LiquibaseConfigurer.configure() 应用属性        │
│  2. 覆盖 changeLog → 插入 getServiceName()          │
│  3. 覆盖 DATABASECHANGELOG 表名前缀服务名            │
│  4. 覆盖 DATABASECHANGELOGLOCK 表名前缀服务名        │
└──────────────────────┬───────────────────────────┘
                       │ 继承
                       ▼
┌──────────────────────────────────────────────────┐
│     XxxServiceLiquibaseConfiguration              │
│  @Bean SpringLiquibase xxxLiquibase()              │
└──────────────────────────────────────────────────┘
```

## 4. 路径隔离

推荐配置：

```yaml
spring:
  liquibase:
    change-log: classpath:/db/changelog/db.changelog-master.yaml
```

服务名为 `attachment` 时，框架实际使用：

```text
classpath:/db/changelog/attachment/db.changelog-master.yaml
```

兼容规则：

- `change-log` 写目录，如 `classpath:/db/changelog`，框架使用 `{目录}/{serviceName}/db.changelog-master.yaml`
- `change-log` 写文件，如 `classpath:/db/changelog/db.changelog-master.xml`，框架在文件名前插入服务名目录

## 5. 表隔离

默认 Liquibase 表：

| 标准配置 | 服务名 | 实际表名 |
| --- | --- | --- |
| `DATABASECHANGELOG` | `attachment` | `attachment_DATABASECHANGELOG` |
| `DATABASECHANGELOGLOCK` | `attachment` | `attachment_DATABASECHANGELOGLOCK` |

如果项目配置自定义表名，框架同样只追加服务名前缀。

## 6. 微服务模式

单个服务进程只扫描当前服务包，只会创建一个 `SpringLiquibase` bean。该 bean 初始化时执行当前服务 changelog。

## 7. 单体模式

单体应用通过 `scanBasePackages` 扫描多个服务包，每个服务配置类贡献一个 `SpringLiquibase` bean。Spring 容器创建这些 bean 时逐个执行各自 changelog，所有服务共享 DataSource，但 changelog 和 history/lock 表隔离。

## 8. 与 Spring Boot 4 LiquibaseAutoConfiguration 的关系

Spring Boot 默认 `LiquibaseAutoConfiguration` 只在缺少 `SpringLiquibase` bean 时创建默认 bean。服务侧声明自己的 `SpringLiquibase` bean 后，Boot 默认 bean 会被抑制，框架的服务级 bean 接管迁移。

`SpringLiquibase` 自身实现 `InitializingBean`，因此不需要类似 Flyway 的额外 migration initializer。额外 initializer 会造成重复执行风险。
