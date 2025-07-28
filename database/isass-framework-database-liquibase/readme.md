# ISASS Framework Database Liquibase

ISASS框架数据库Liquibase模块，提供基于Liquibase的数据库版本管理和迁移功能。

## 功能特性

- **数据库版本管理**: 自动化的数据库结构版本控制
- **数据库迁移**: 支持数据库升级和回滚操作
- **SQL脚本生成**: 可以生成变更SQL脚本用于手动执行
- **自定义变更**: 支持扩展自定义的数据库变更操作
- **Spring Boot集成**: 无缝集成到Spring Boot应用中

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>vip.isass</groupId>
    <artifactId>isass-framework-database-liquibase</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置启用

在`application.yml`中配置：

```yaml
isass:
  database:
    liquibase:
      enabled: true
```

### 3. 使用示例

```java
@Autowired
private LiquibaseService liquibaseService;

// 执行数据库升级
liquibaseService.update(dataSource, "db/changelog/db.changelog-master.xml");

// 回滚到指定标签
liquibaseService.rollback(dataSource, "db/changelog/db.changelog-master.xml", "v1.0");

// 生成SQL脚本
String sql = liquibaseService.generateSQL(dataSource, "db/changelog/db.changelog-master.xml");
```

## 核心组件

### LiquibaseService
提供Liquibase操作的核心服务类，包含升级、回滚、SQL生成等功能。

### LiquibaseAutoConfiguration
Spring Boot自动配置类，自动装配Liquibase相关Bean。

### AbstractIsassChange
自定义变更的抽象基类，支持扩展特定的数据库变更操作。

## 依赖关系

- `isass-framework-common`: 框架公共模块
- `isass-framework-database-core`: 数据库核心模块
- `liquibase-core`: Liquibase核心库
- `spring-context`: Spring上下文（可选）
- `spring-boot-autoconfigure`: Spring Boot自动配置（可选）

## 版本信息

- 当前版本: 4.0.0-SNAPSHOT
- 最低JDK版本: 17
- Spring Boot版本: 3.x

## 许可证

LGPL-3.0 License 