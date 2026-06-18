# Liquibase 多模式迁移使用指南

## 1. 添加新服务

### 1.1 创建 Liquibase 配置类

在服务模块中创建 `vip.isass.xxx.liquibase` 包，添加配置类：

```java
package vip.isass.attachment.liquibase;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import vip.isass.framework.database.core.liquibase.AbstractLiquibaseConfiguration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class
})
public class XxxServiceLiquibaseConfiguration extends AbstractLiquibaseConfiguration {

    @Override
    protected String getServiceName() {
        return "xxx";
    }

    @Bean
    public SpringLiquibase xxxLiquibase(LiquibaseProperties properties, DataSource dataSource,
                                        ResourceLoader resourceLoader) {
        return createLiquibase(properties, dataSource, resourceLoader);
    }
}
```

### 1.2 创建 changelog 文件

推荐目录结构：

```text
src/main/resources/db/changelog/xxx/
  ├── db.changelog-master.yaml
  └── changes/
      ├── 1.0.0-init.yaml
      └── 3.4.0.1-update.yaml
```

`xxx` 必须与 `getServiceName()` 返回值一致。

### 1.3 配置 application.yml

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:/db/changelog/db.changelog-master.yaml
    database-change-log-table: DATABASECHANGELOG
    database-change-log-lock-table: DATABASECHANGELOGLOCK
    contexts: dev
    label-filter: mysql
```

框架会把示例中的 `change-log` 解析为：

```text
classpath:/db/changelog/xxx/db.changelog-master.yaml
```

并把两张 Liquibase 管理表解析为：

```text
xxx_DATABASECHANGELOG
xxx_DATABASECHANGELOGLOCK
```

## 2. 单体模式

将多个服务模块的配置类扫描到同一个 Spring Boot 应用：

```java
@SpringBootApplication(scanBasePackages = {
    "vip.isass.attachment",
    "vip.isass.user"
})
```

不需要额外配置。每个服务的 `XxxServiceLiquibaseConfiguration` 都会创建自己的 `SpringLiquibase` bean，并在 Spring 初始化阶段执行自己的 changelog。

## 3. 配置参考

支持 `spring.liquibase.*` 标准属性，包括：

| 属性 | 说明 |
| --- | --- |
| `spring.liquibase.change-log` | changelog 基础路径或基础文件，框架自动插入服务名 |
| `spring.liquibase.database-change-log-table` | history 表基础名，框架自动前缀服务名 |
| `spring.liquibase.database-change-log-lock-table` | lock 表基础名，框架自动前缀服务名 |
| `spring.liquibase.contexts` | 运行上下文 |
| `spring.liquibase.label-filter` | label 过滤 |
| `spring.liquibase.default-schema` | 默认 schema |
| `spring.liquibase.liquibase-schema` | Liquibase 管理表所在 schema |
| `spring.liquibase.drop-first` | 迁移前是否 drop schema |
| `spring.liquibase.clear-checksums` | 是否清理 checksum |
| `spring.liquibase.rollback-file` | 输出 rollback SQL 文件 |

## 4. 常见问题

### changelog 不在预期目录

检查：

- `spring.liquibase.change-log` 是否写为基础目录或基础 master 文件
- `getServiceName()` 是否与资源目录名一致
- changelog 文件扩展名是否为 `.xml`、`.yaml`、`.yml`、`.json` 或 `.sql`

### 单体模式下某个服务未执行

确认该服务的 `XxxServiceLiquibaseConfiguration` 被 `@ComponentScan` 扫描到，并且 `spring.liquibase.enabled` 没有被设置为 `false`。

### 重复执行迁移

不要再额外声明手工调用 `SpringLiquibase.afterPropertiesSet()` 的 initializer。`SpringLiquibase` 自身会在 bean 初始化阶段执行迁移。
