# Spring Boot Adapter Boundary

## 目标

`isass-adapter-springboot` 是 isass 在 Spring Boot 运行时下的统一适配层。它负责把 isass 的纯 Java 核心能力、功能模块能力、SPI 扩展点装配成 Spring Bean。

总原则：

- `isass-core-*` 是最低层框架模块，必须解耦 Spring。
- 其他 `isass-*` 模块尽全力解耦 Spring。短期无法解耦的模块需要明确标记原因、边界和后续迁移路线。
- Spring Boot、Micronaut、Solon 等运行时适配逻辑应进入对应 adapter，而不是反向污染 core 或功能模块。

## 模块边界

业务微服务按需依赖功能模块：

- 需要 Spring Boot 运行时装配：依赖 `isass-adapter-springboot`
- 需要低代码能力：依赖 `isass-nocode-core`、`isass-nocode-*` 对应模块
- 需要 Web 能力：依赖 `isass-web-*` 对应模块
- 需要 Security 能力：依赖 `isass-security-*` 对应模块
- 需要数据库：依赖 `isass-database-*` 对应模块
- 需要 MQ：依赖 `isass-mq-*` 对应模块
- 需要网络能力：依赖 `isass-net-*` 对应模块

因此，一个只使用 Spring Event MQ 的业务微服务，应依赖 `isass-adapter-springboot` 和 `isass-mq-springevent`。`isass-adapter-springboot` 通过 classpath 条件或 SPI 发现该功能模块后，才启用对应的 Spring Boot 装配；未依赖的 database、net、web 等功能不会被自动带入。

## 自动配置规则

优先方向是 Java 原生 SPI：

- core/feature 模块提供纯 Java 接口、实现和 SPI 贡献描述。
- `isass-adapter-springboot` 读取 SPI 或 classpath 条件，把对应能力注册成 Spring Bean。
- 未来 `isass-adapter-micronaut`、`isass-adapter-solon` 可以复用同一套 SPI 元数据，只重写运行时注册逻辑。
- 已存在于功能模块中的 Spring Boot auto-configuration imports 是迁移对象，不是长期目标。

`isass-adapter-springboot` 可以使用 optional 依赖或条件装配支持功能模块，但不能让业务微服务因为依赖 adapter 而被动获得未显式依赖的功能。

数据库是阶段性特例。当前部分 database 实现基于 Spring Data、MyBatis-Plus Spring 生态，短期无法完全解耦。后续应优先拆出纯 Java 的 database 抽象，再把 Spring-bound 实现标识为具体运行时实现。

## 当前进展

2026-06-20：

- `isass-adapter-springboot` 已成为 core/common 的 Spring Boot 自动配置入口
- `isass-core-common` 已移除自己的 Spring Boot auto-configuration imports
- Spring MVC 参数绑定、消息转换等 Web 异常映射已从 `isass-core-common` 迁到 `isass-web-springmvc`
- `isass-core-common` 不再直接依赖 Spring WebMVC starter
- 微服务认证请求头 provider 的 Spring 组件实现已迁到 `isass-security-springsecurity`，`isass-core-common` 只保留纯 Java 类型和常量
- 角色码服务管理器的 Spring 组件实现已迁到 `isass-security-springsecurity`，`isass-core-common` 只保留纯 Java 聚合管理器
- 通用选择项服务管理器已改为 `isass-core-common` 纯 Java 聚合器，由 `isass-adapter-springboot` 自动配置负责 Spring Bean 装配
- 启动后自动销毁临时 Bean 的 Spring 生命周期监听器已从 `isass-core-common` 迁到 `isass-adapter-springboot`，core 仅保留 `AutoDestroyable` 标记接口

## 后续拆分原则

后续继续处理 `isass-core-*` 和其他 `isass-*` 中剩余 Spring 类型时，按以下顺序迁移：

- `isass-core-*` 中的 Spring 类型必须移除或桥接到 adapter。
- 其他 `isass-*` 中的 Spring 类型优先替换为 Java SPI、纯 Java 接口或运行时无关抽象。
- Spring Bean 注册、配置绑定、事件监听、ConversionService、AOP、LoggingSystem 等运行时语义进入 `isass-adapter-springboot`。
- Web MVC、Spring Security、Spring Data 等无法立即抽象的实现，先标记为 Spring-bound 实现，再逐步拆出运行时无关的 core 抽象。
- 每个 Spring 使用点都要记录功能、当前依赖原因、迁移方案、风险和优先级。
