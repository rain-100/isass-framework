# Isass V4 微服务模块与 DDD 目录规范

## Maven 模块

每个微服务统一使用三模块结构：

```text
isass-service-{microservice}
├── {microservice}-api
├── {microservice}-service
└── {microservice}-boot
```

`api` 和 `service` 是可发布到 Maven 的普通 jar：前者提供跨服务稳定契约，后者提供单体模式所需的本地 Spring 实现。`boot` 只负责启动、运行配置和部署制品，必须设置 `maven.install.skip`、`maven.deploy.skip`，不得作为其他服务的依赖。

调用方在单体模式依赖 `{microservice}-service`，分布式模式只依赖 `{microservice}-api`。服务实现选择优先级为本地实现、gRPC 远程实现、HTTP 远程实现。

## 限界上下文与分层

一个微服务可包含多个平级限界上下文；不同上下文不得通过 Mapper、Repository 或直接查询数据表相互耦合，只能通过公开契约、应用服务或事件协作。

```text
{microservice}-api/src/main/java/vip/isass/{microservice}/{context}/
├── util/
├── application/service/
└── domain/
    ├── model/entity/
    ├── criteria/
    ├── req/
    ├── vo/
    ├── event/
    ├── exception/
    └── enums/

{microservice}-service/src/main/java/vip/isass/{microservice}/{context}/
├── interfaces/
│   ├── rest/
│   ├── listener/
│   │   ├── kafka/
│   │   ├── rocketmq/
│   │   ├── pulsar/
│   │   └── redis/{pubsub,stream}/
│   └── job/
├── application/
│   └── service/
├── domain/
│   ├── model/entity/
│   └── repository/
└── infrastructure/
    ├── db/liquibase/
    ├── persistence/mybatisplus/
    └── xxx/
```

`interfaces` 使用复数形式，避免使用 Java 关键字 `interface` 作为包名。API 中的 `req`、`vo`、`criteria` 与零代码实体是公开领域契约模型；标准零代码实体放在 API 的 `domain/model/entity`。仅服务端内部使用、不会跨模块传递的持久化实体才放在 service 的 `domain/model/entity`。

零代码的简单表可以保持应用服务与 Repository 的轻量实现。只有确有复杂业务规则的场景才引入聚合、值对象、领域服务和事件。

## Liquibase

每个上下文的 Liquibase 配置类归入自身基础设施层，例如：

```text
vip/isass/{microservice}/{context}/infrastructure/db/liquibase/
└── XxxLiquibaseConfiguration.java
```

changelog 资源按上下文放在：

```text
{microservice}-service/src/main/resources/db/changelog/{context}/
```

这里的 `db/changelog/{context}` 是当前 `LiquibaseServiceNaming` 的约定；Java 配置类仍位于
`infrastructure/db/liquibase/`。`boot` 模块只负责应用入口和运行配置，不承载领域 Liquibase 配置类。

## BSP 约定

`isass-service-bsp` 是 Basic Service Platform，使用 `bsp-api`、`bsp-service`、`bsp-boot` 三模块。当前上下文为 attachment、filesystem、auth、dict、param、dir、equipment、area、log；其业务表统一以 `bsp_` 开头，例如 `bsp_auth_*`、`bsp_dict_*`、`bsp_param_*`。
