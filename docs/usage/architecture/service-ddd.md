# Isass V4 服务模块与 DDD 目录规范

## Maven 模块

每个服务统一使用三模块结构：

```text
isass-service-{service}
├── {service}-api
├── {service}-service
└── {service}-boot
```

`api` 和 `service` 是可发布到 Maven 的普通 jar：前者提供跨服务稳定契约，后者提供可按运行模式启用的本地 Spring 实现。`boot` 只负责启动、运行配置和部署制品，必须设置 `maven.install.skip`、`maven.deploy.skip`，不得作为其他服务的依赖。

一个部署制品可以同时包含依赖服务的 `api` 与 `service` jar，并通过 `isass.boot.microservice.enabled` 在运行时切换而无需重新构建：微服务模式只注册 API 侧能力和远程 Entrypoint 代理，依赖服务的实现类即使位于 classpath 也不得注册为 Bean；单体模式再启用依赖服务的本地实现，本地实现优先于 HTTP 远程实现。源码编译依赖仍应面向对方 `api` 契约，依赖服务的 `service` jar 由 boot 装配进入部署制品。动态 gRPC 合同代码仍作为遗留预研保留，但未部署 gRPC Server，不得配置为运行时 endpoint 或作为 V4 验收前提。

每个服务的 `{Service}ServiceAutoConfiguration` 是本地实现总入口：当前应用名等于该服务名时，无论运行模式均启用；当前应用为其他服务时，仅 `isass.boot.microservice.enabled=false` 才启用。该配置由 `AutoConfiguration.imports` 统一加载，服务自己的 Boot 入口不再重复显式导入；`{Service}ApiAutoConfiguration` 不受运行模式条件限制。配置缺失时按微服务隔离处理，不能意外启用依赖服务的本地实现。

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

数据库生成的领域模块统一归入限界上下文的 `domain` 命名空间，表名使用 `{service}_{context}_{entity}`，领域归属由表注释
`[--domain:{domain}]` 声明；领域较大时可以使用
`[--domain:{domain};--subdomain:{subdomain}]` 声明一个可选的一级子域。生成器据此生成
`{context}/domain/{domain}[/{subdomain}]` 下的领域模型与基础设施，并将标准 CRUD 服务按相同领域归属生成到
`{context}/application/{domain}[/{subdomain}]/service`。CRUD 服务属于上下文应用层，不再嵌套在领域包内部，
也不再按实体增加一级应用能力包。

CRUD Service 是由生成器创建的领域模块应用服务骨架，同时也是手写应用能力的扩展点。它位于 application 层，
允许直接协调同一限界上下文内其他聚合或领域的 Repository、应用服务和端口；生成来源不构成额外的领域隔离边界。
对外能力应优先复用 NoCode 标准 CRUD、Criteria、关联能力和生命周期；能够由这些机制完整表达时，不得增加同义的
查询、新增、修改或删除 Entrypoint。只有具有独立业务语义且无法由标准 NoCode 执行边界表达的用例才新增自定义入口。
跨限界上下文协作仍必须使用对方公开契约或事件，不能直接访问对方 Repository。可复用的业务不变量应下沉到领域服务，
具体基础设施通过端口接入，避免把领域规则或基础设施细节堆积在 CRUD Service 中。
因此 Service 接口和实现默认只在文件不存在时生成，正常重新生成不得覆盖其中的手写扩展；Entity 和 Criteria
仍由数据库元数据统一覆盖生成。强制重建 Service 只能作为显式的一次性迁移操作使用。

手写应用代码按“应用能力优先、技术类型其次”纵向组织。只属于一个子域的能力放在
`{context}/domain/{domain}/{subdomain}/application/{capability}`；同一领域内编排多个子域的能力放在
`{context}/domain/{domain}/application/{capability}`；跨领域编排能力才放在 `{context}/application/{capability}`。
`capability` 表示稳定的业务用例族，例如 `authinit`、`license`、`taskmanagement`，不是可执行模块名，也不能省略后
直接把手写代码放入上下文根的 `application.model`、`application.service`、`application.listener` 或
`application.initialization`。

```text
{service}-api/src/main/java/vip/isass/{service}/{context}/
├── application/
│   ├── {domain}[/{subdomain}]/service/       # 生成的标准 CRUD 服务契约
│   └── {cross-domain-capability}/
│       ├── model/{vo,dto,req,resp,enums,event}/
│       └── service/
└── domain/{domain}[/{subdomain}]/
    ├── application/
    │   └── {domain-capability}/
    │       ├── model/{vo,dto,req,resp,enums,event}/
    │       └── service/
    └── domain/
        ├── model/{entity,criteria,vo,dto,req,resp,enums,event}/
        └── exception/

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
│   ├── {domain}[/{subdomain}]/service/       # 生成的标准 CRUD 服务实现
│   └── {cross-domain-capability}/{service,model,listener,initialization,schedule,job}/
└── domain/{domain}[/{subdomain}]/
    ├── application/
    │   └── {domain-capability}/{service,model,listener,initialization,schedule,job}/
    ├── domain/{model,repository,exception}/
    └── infrastructure/{persistence,xxx}/
```

`interfaces` 使用复数形式，避免使用 Java 关键字 `interface` 作为包名。领域模型只服务于领域及聚合边界；应用模型用于
跨聚合编排或某一应用能力专属的数据传递，并放在所属 `{capability}/model` 下。多个应用能力共同使用的稳定领域概念应
优先提升到对应 `domain.model`，不能用上下文根的 `application.model` 充当公共模型仓库。外部 HTTP、消息和任务入口放在
`interfaces`；应用内部监听器、初始化器和调度器跟随所属应用能力。
领域事件的数据模型统一放在 `domain.model.event`，不另建平级的 `domain.event` 包。
标准零代码实体和 Criteria 放在 API 的 `{context}/domain/{domain}[/{subdomain}]/domain/model/entity`、
`{context}/domain/{domain}[/{subdomain}]/domain/model/criteria`。仅服务端内部使用、不会跨模块传递的持久化实体才放在 service 的对应领域模块中。

子域是同一领域内的代码组织边界，不是新的限界上下文，也不进入物理表名。第一层只支持一个可选子域，不支持递归的
子子域。跨子域协作优先通过各子域公开服务或领域级应用能力完成；现有 NoCode 显式关联可以跨子域引用实体，因此不能
把子域误当成天然的独立部署或强隔离边界。

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

`isass-service-bsp` 是 Basic Service Platform，使用 `bsp-api`、`bsp-service`、`bsp-boot` 三模块。当前上下文为 file、auth、config、device、location、log；file 上下文包含 attachment 与 filesystem 两个领域。其业务表遵循 `{service}_{context}_{entity}`，例如 `bsp_file_att_file`、`bsp_auth_user`、`bsp_config_dictionary_type`、`bsp_location_admin_division`，并在表注释中声明领域。

BSP 作为一个服务，明确使用唯一的 `BspLiquibaseConfiguration` 与
`db/changelog/bsp/db.changelog-master.yaml`；不要为任一 BSP 上下文或数据库产品再建立 master。
