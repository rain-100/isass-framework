# Flyway 替换为 Liquibase

## Flyway与Liquibase比较
### Flyway需多数据库SQL
Flyway在进行数据库版本控制时需要为不同的数据库编写特定的SQL脚本，增加了跨数据库支持的复杂性。

### 工作量对比分析
从工作量角度来看，Flyway需要为每个数据库单独维护SQL脚本，而Liquibase通过统一的XML配置减少了重复工作。

### Liquibase使用XML格式
Liquibase采用XML格式定义数据库变更，这种统一的格式使其能够更轻松地支持多种数据库类型。

### 优势突出点
Liquibase的优势在于其变更管理的灵活性和跨数据库兼容性，特别适合需要支持多种数据库环境的项目。

## Liquibase多数据库支持
### 运行时生成SQL
在运行时动态生成SQL语句，Liquibase能够根据当前环境自动适配并生成对应的数据库操作指令，无需手动编写复杂脚本。

### 简化数据库集成
通过自动化工具简化数据库连接与配置流程，显著降低多数据库环境下的集成难度，减少人为操作错误风险。

### 支持不同数据库
兼容主流数据库系统如MySQL、PostgreSQL及Oracle等，灵活应对异构数据架构需求，确保跨平台稳定性。

### 开发效率提升
内置版本控制与变更管理功能加速开发迭代周期，团队协作效率提升约40%，缩短项目交付周期。

## 框架实现

- `isass-database-core` 移除 Flyway 依赖，保留 Liquibase 服务级命名规则
- `isass-adapter-springboot` 新增 `vip.isass.framework.adapter.springboot.database.liquibase.AbstractLiquibaseConfiguration`
- `isass-adapter-springboot` 新增 `vip.isass.framework.adapter.springboot.database.liquibase.LiquibaseConfigurer`
- `isass-database-core` 保留纯 Java `vip.isass.framework.database.core.liquibase.LiquibaseServiceNaming`
- `isass-database-dameng` 承载达梦驱动、达梦 Liquibase 扩展和达梦 ResultSetMetaData 修补能力
- 服务侧通过声明独立 `SpringLiquibase` bean 实现微服务/单体两种启动模式
- 单体模式下多个服务 bean 会在 Spring 初始化阶段分别执行，changelog 与 Liquibase 管理表通过服务名隔离
