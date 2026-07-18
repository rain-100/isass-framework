# Isass V4 服务模块与 DDD 目录规范

## Maven 模块

每个服务统一使用三模块结构：

```text
isass-service-{service}
├── {service}-api
├── {service}-service
└── {service}-boot
```

`api` 和 `service` 是可发布到 Maven 的普通 jar：前者提供跨服务稳定契约，后者提供单体模式所需的本地 Spring 实现。`boot` 只负责启动、运行配置和部署制品，必须设置 `maven.install.skip`、`maven.deploy.skip`，不得作为其他服务的依赖。

调用方在单体模式依赖 `{service}-service`，分布式模式只依赖 `{service}-api`。当前正式服务实现选择优先级为本地实现、HTTP 远程实现。动态 gRPC 合同代码仍作为遗留预研保留，但未部署 gRPC Server，不得配置为运行时 endpoint 或作为 V4 验收前提。

### Boot 部署制品

`boot` 在 `package` 阶段必须产出 Spring Boot 可执行 JAR；部署归档由服务所采用的标准装配决定，不通过
Maven install/deploy 发布。BSP 使用与附件服务一致的 `isass-core-build` 标准 assembly，不维护服务私有的
`src/assembly/deployment.xml`。需要专用部署 ZIP 的服务可在自身 boot 模块增加 `package-deployment-zip`
执行；ZIP 根目录使用运行服务名（例如 `im-service`），并至少包含：

服务配置统一保存在源码的 `{service}-boot/src/main/resources/config/application.yml`。标准 assembly 将它外置为
部署包根目录下的 `config/application.yml`；不得再使用根目录 `application.yml` 或 `application.properties`。

```text
{service-name}/
├── {service}-boot-{version}.jar
└── config/application.yml
```

可执行 JAR、标准 assembly、专用 ZIP、容器镜像均由 CI/部署制品库发布，绝不通过 Maven install/deploy 发布。

## 限界上下文与分层

一个服务可包含多个平级限界上下文；不同上下文不得通过 Mapper、Repository 或直接查询数据表相互耦合，只能通过公开契约、应用服务或事件协作。

```text
{service}-api/src/main/java/vip/isass/{service}/{context}/
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

{service}-service/src/main/java/vip/isass/{service}/{context}/
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

每个服务只创建一个 `SpringLiquibase` bean，其配置归入 service 模块的基础设施层，例如：

```text
vip/isass/{service}/infrastructure/db/liquibase/
└── {Service}LiquibaseConfiguration.java
```

master 内部引用的 changelog 按限界上下文组织资源；每个上下文有一个包含最终完整表结构的 init XML，后续变更
放在当前大版本目录并由唯一 YAML master 显式 include：

```text
{service}-service/src/main/resources/db/changelog/{service}/
├── db.changelog-master.yaml
└── {context}/
    ├── {service}-{context}-init.xml
    └── v4/
        └── {context}-4.0.1-description.xml
```

但同一服务必须只有一个 changelog master 与一对 Liquibase history 表；上下文目录只能作为该 master
的内部组织，不得分别创建 `SpringLiquibase` bean 或方言 master。当前 `LiquibaseServiceNaming` 的默认约定
是 `db/changelog/{service}/db.changelog-master.yaml`；Java 配置类仍位于
`infrastructure/db/liquibase/`。`boot` 模块只负责应用入口和运行配置，不承载领域 Liquibase 配置类。

## BSP 约定

`isass-service-bsp` 是 Basic Service Platform，使用 `bsp-api`、`bsp-service`、`bsp-boot` 三模块。当前上下文为 attachment、filesystem、auth、config、device、location、log；其业务表遵循 `{service}_{context}_{entity}`，例如 `bsp_auth_user`、`bsp_config_dictionary`、`bsp_location_admin_division`。

BSP 作为一个服务，明确使用唯一的 `BspLiquibaseConfiguration` 与
`db/changelog/bsp/db.changelog-master.yaml`；不要为任一 BSP 上下文或数据库产品再建立 master。
