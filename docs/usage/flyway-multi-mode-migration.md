# Flyway 多模式迁移 — 使用指南

## 1. 添加新服务（微服务模式）

### 1.1 创建 Flyway 配置类

在服务模块中创建 `vip.isass.xxx.flyway` 包，添加配置类：

```java
package vip.isass.attachment.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import vip.isass.framework.database.core.flyway.AbstractFlywayConfiguration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class
})
public class XxxServiceFlywayConfiguration extends AbstractFlywayConfiguration {

    @Override
    protected String getServiceName() {
        return "xxx"; // 必须与本服务的数据库 migration 目录名一致
    }

    @Bean
    public Flyway xxxFlyway(FlywayProperties properties, DataSource dataSource,
                             ResourceLoader resourceLoader,
                             ObjectProvider<FlywayConfigurationCustomizer> customizers) {
        return createFlyway(properties, dataSource, resourceLoader, customizers.orderedStream().toList());
    }
}
```

### 1.2 创建 Migration SQL 文件

目录结构：
```
src/main/resources/db/mysql/xxx/
  ├── V1.0.0__init.sql
  ├── V3.4.0.1__update.sql
  └── ...
```

`xxx` 必须与 `getServiceName()` 返回值一致。

### 1.3 配置 application.yml

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/mysql
    table: flyway_schema_history
    baseline-on-migrate: true
    baseline-version: 0.1
    community-db-support-enabled: true
    encoding: UTF-8
    out-of-order: true
    # 可选：clean-on-validation-error 需通过 FlywayConfigurationCustomizer 配置
    clean-disabled: false
```

- `locations` 写基础路径（如 `classpath:db/mysql`），框架自动追加服务名（如 `/xxx`）
- `table` 写基础表名（如 `flyway_schema_history`），框架自动前缀服务名（如 `xxx_flyway_schema_history`）
- `clean-disabled: false` 允许 clean（仅 dev 环境，见 3.2 节）

### 1.4 添加 Flyway 数据库驱动依赖

在 `isass-core-dependencies` BOM 中对应的数据库模块已包含 Flyway 驱动。若使用 MySQL，确保 `flyway-mysql` 在 classpath 上：

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

## 2. 单体模式

### 2.1 聚合所有服务

将多个服务模块的配置类扫描到同一个 Spring Boot 应用：

```java
@SpringBootApplication(scanBasePackages = {
    "vip.isass.attachment",
    "vip.isass.user",
    // ...
})
```

### 2.2 每个服务 Flyway bean 独立

不需要额外配置——每个服务的 `XxxServiceFlywayConfiguration` 已定义自己的 `@Bean Flyway`。框架的 `IsassFlywayAutoConfiguration` 自动注入所有 Flyway bean，逐一执行迁移。

## 3. 配置参考

### 3.1 FlywayProperties 全部可用属性

参考 `application.yml` 中 `spring.flyway.*` 的所有标准属性。`FlywayConfigurer` 将其全量映射到 `FluentConfiguration`，包括但不限于：

| 属性 | 类型 | 说明 |
|------|------|------|
| `spring.flyway.locations` | List<String> | 迁移文件位置（框架自动追加服务名） |
| `spring.flyway.table` | String | 历史表名（框架自动前缀服务名） |
| `spring.flyway.baseline-on-migrate` | boolean | 是否自动 baseline |
| `spring.flyway.baseline-version` | String | baseline 版本号 |
| `spring.flyway.community-db-support-enabled` | Boolean | 社区版数据库支持（MySQL 必须为 true） |
| `spring.flyway.encoding` | Charset | SQL 文件编码 |
| `spring.flyway.connect-retries` | int | 连接重试次数 |
| `spring.flyway.connect-retries-interval` | Duration | 重试间隔 |
| `spring.flyway.default-schema` | String | 默认 schema |
| `spring.flyway.schemas` | List<String> | schemas |
| `spring.flyway.out-of-order` | boolean | 允许乱序迁移 |
| `spring.flyway.validate-on-migrate` | boolean | 迁移前校验 |
| `spring.flyway.clean-disabled` | boolean | 禁用 clean |
| `spring.flyway.batch` | Boolean | 批处理模式 |

### 3.2 框架的覆盖行为

以下行为由 `AbstractFlywayConfiguration` 自动覆盖（仅与隔离相关的属性）：

| 属性 | 覆盖值 | 说明 |
|------|--------|------|
| `locations` | `spring.flyway.locations + "/" + getServiceName()` | 隔离各服务的迁移文件目录 |
| `table` | `getServiceName() + "_" + spring.flyway.table` | 隔离各服务的 schema history 表 |

其余所有属性（`clean-disabled`、`clean-on-validation-error` 等）均由 `FlywayProperties` 控制，通过 `application.yml` 配置，**框架不做硬编码覆盖**。

#### 开发环境推荐配置

```yaml
# application-dev.yml
spring:
  flyway:
    clean-disabled: false  # 允许 clean（仅 dev）
```

> `cleanOnValidationError` 不在 `FlywayProperties` 中，无法通过 `application.yml` 配置。若需启用，请使用 `FlywayConfigurationCustomizer` + `@Profile("dev")`：
> ```java
> @Configuration
> @Profile("dev")
> public class DevFlywayCustomizer implements FlywayConfigurationCustomizer {
>     @Override
>     public void customize(FluentConfiguration config) {
>         config.cleanOnValidationError(true);
>     }
> }
> ```

## 4. 常见问题

### 4.1 Flyway 校验失败

```
ERROR: Validate failed: Detected failed migration to version ...
```

框架默认 `spring.flyway.clean-disabled=true`，不会自动 clean。若开发环境需要自动修复，配置 `spring.flyway.clean-disabled=false`，并通过 `FlywayConfigurationCustomizer` 启用 `cleanOnValidationError`（见 3.2 节）。或手动 truncate history 表后重跑。

### 4.2 迁移文件不在预期目录

检查：
- `spring.flyway.locations` 是否设置为基础路径（不含服务名）
- `getServiceName()` 返回值是否与子目录名一致
- `community-db-support-enabled` 是否为 `true`（Flyway 10+ CE 必需）

### 4.3 单体模式下某些服务的 Flyway 未执行

确认每个服务模块的 Flyway 配置类都被 `@ComponentScan` 扫描到。框架的 `IsassFlywayMigrationInitializer` 日志会打印每个 Flyway bean 的迁移状态。

### 4.4 MySQL 8+ 驱动兼容性

确保使用 `com.mysql.cj.jdbc.Driver`，且连接 URL 包含 `nullDatabaseMeansCurrent=true` 和 `allowPublicKeyRetrieval=true`。
