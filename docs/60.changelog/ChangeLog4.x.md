# super core

## 更新日志

### super.4.0.0

#### feat

- **架构升级**：支持 Maven 4 风格构建，引入 `root="true"` 属性和 `modelVersion 4.1.0`。
- **模块重构**：新增 `isass-framework-dependencies` (BOM) 和 `isass-framework-parent` (Parent POM)，提供更加灵活的项目继承与依赖管理方式。
- **包名重构**：全量迁移包名，从 `vip.isass.core` 统一变更为 `vip.isass.framework`，保持与框架命名的统一。
- **Jakarta EE 适配**：全面适配 Jakarta EE 10，切换相关注解包名。
- **Spring Boot 升级**：深度适配自定义 Spring Boot 4.0.5 版本。
- **MQ 系统重构**：
    - 引入新的 `IMqConsumer` 接口和 `MqMessageContext`。
    - 实现 MQ 消费者生命周期的全自动托管，移除冗余的初始化模板代码。
    - 优化配置项，统一使用 `mq.` 前缀。
- **健康检查优化**：适配 Spring Boot 4.x 的模块化健康检查体系，更新 `HealthIndicator` 相关实现。
- **模块合并**：
    - `isass-framework-nocode-*` 核心功能合并至 `isass-framework-common` 的 `core.structure` 目录下。
    - `isass-framework-mq-redisstream` 和 `redispubsub` 合并至 `isass-framework-database-redis`。
    - `isass-framework-build` 升级并更名为 `isass-framework-deploy`。

#### optimize

- 优化 `IV2LocalService` 接口，增加 `getService()` 必需方法以提升类型安全性。
- 统一 Maven 编译配置，由父 POM 统一管控 `maven.compiler.release` (Java 25)。
- 清理根项目 POM，使其专注于模块聚合管理。
