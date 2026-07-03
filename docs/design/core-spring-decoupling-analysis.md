# Core Spring Decoupling Analysis

## 总原则

`isass-core-*` 是 isass 最底层框架能力，必须解耦 Spring。其他 `isass-*` 模块尽全力解耦 Spring；短期无法解耦的模块需要标记原因、边界和迁移路线。

运行时装配不应写死在 core 或功能模块里。长期方向是：core/feature 模块提供纯 Java 接口、实现和 Java SPI 贡献描述；`isass-adapter-springboot`、`isass-adapter-micronaut`、`isass-adapter-solon` 等 adapter 负责把这些能力注册到对应运行时。

## 模块命名

`isass-core-nocode` 已调整为 `isass-nocode-core`。`nocode` 表示 v1/v2/v3 的一系列接口和实现，包括 controller、service、access 等低代码业务能力。后续可以继续扩展为 `isass-nocode-xxx` 模块。

## 当前扫描范围

- `isass-core-common/src/main/java`
- `isass-nocode-core/src/main/java`
- `isass-core-*` 的 Maven 依赖

测试源码里的 Spring 依赖暂时作为低优先级处理；main 源码和模块主依赖优先处理。

## Spring 使用点清单

| 类/位置 | Spring 使用 | 功能 | 迁移方案 | 优先级 |
| --- | --- | --- | --- | --- |
| `vip.isass.framework.common.support.Converter` | 继承 `org.springframework.core.convert.converter.Converter` | isass 通用转换器，同时兼容 Hutool converter 和 Spring converter | 已移除 Spring Converter 继承，保留 Hutool converter 兼容并显式声明 `convert(S)` | 完成 |
| `common/converter/**` | `@Component` | 注册 String、Map、Date、LocalDateTime 等转换器 | 已移除 converter 类上的 `@Component`；`isass-adapter-springboot` 通过 `IsassSpringConverterAdapter` 桥接到 Spring `ConditionalGenericConverter` | 完成 |
| `BuildInCoreExceptionMapping` | 原 `@Component` | 核心异常到框架异常码映射 | 已改为普通 Java 映射实现；Spring Boot 场景由 `isass-adapter-springboot` 注册，Web MVC 异常已迁出到 `isass-web-springmvc` | 完成 |
| `BeanProviderUtil` | 原 `SpringContextUtil` 中的 `ApplicationContextAware`、`ApplicationContext`、`DefaultListableBeanFactory`、`ResolvableType`、`@Component` | 全局访问运行时 Bean、动态注册/移除 Bean、按泛型/Support 查询 Bean | 已改成纯 Java 门面，实际运行时能力由 `BeanProvider` 提供；`isass-adapter-springboot` 提供 `SpringBeanProvider`；旧 `SpringContextUtil` 不保留 | 完成 |
| `LoginUserUtil` | 通过 `BeanProviderUtil` 获取 `LoginUserService` | 获取当前登录用户服务 | 已改为纯 Java `LoginUserService` provider/setter；Spring Boot adapter 负责从 Spring Bean 桥接 | 完成 |
| `LongSequence` | 通过 `BeanProviderUtil.getBeanOfSupport` 查找 `Sequence<Long>` | 获取 Long 序列生成器 | 已改为纯 Java `Sequence<Long>` provider/setter；Spring Boot adapter 负责从 Spring Bean 桥接，默认仍回退随机 Long | 完成 |
| `SystemClock` | 通过 `BeanProviderUtil` 获取 `ISystemClock` | 获取系统时钟实现 | 已改为纯 Java `ISystemClock` provider/setter；Spring Boot adapter 负责从 Spring Bean 桥接，默认仍使用 JDK 时间 | 完成 |
| `IAnyJsonEntity` | 通过 `BeanProviderUtil` 获取 `IDictTranslationProvider` | JSON 实体字典翻译 | 已改为纯 Java `IDictTranslationProvider` provider/setter；Spring Boot adapter 负责从 Spring Bean 桥接，未配置 provider 时跳过字典翻译 | 完成 |
| `LogUtil` | 原先使用 `LoggingSystem`、`LogLevel`、`LoggerConfiguration`，并通过运行时容器获取 Bean | 动态关闭/恢复日志级别 | 已抽象 `LogLevelManager`；core 默认无操作，`isass-adapter-springboot` 提供 `SpringBootLogLevelManager` | 完成 |
| `ReflectUtils` | `AopUtils.getMostSpecificMethod` | 查找代理类/接口上的实际 API 方法 | 已改为 JDK 反射按方法名和参数查找接口方法 | 完成 |
| `ApiService` | `AnnotationUtils.findAnnotation`、Spring `@Order` | 按本地服务/Feign 服务优先级路由 API 服务调用 | 已改为 `@IsassOrder` + `IsassOrderUtil`，并以反射方式兼容 Spring `@Order` | 完成 |
| `IV2Service` | 继承 Spring `Ordered` | v2 service 优先级排序 | 已移除 `Ordered`/`IsassOrdered` 继承；仅保留历史 `getOrder()` 默认方法兼容 v2 | 完成 |
| `V2ServiceManagerUtil` | 泛型约束 `S extends Ordered` | v2 service 链路选择与降级 | 已移除排序接口泛型约束，统一通过 `IsassOrderUtil.getOrder()` 读取顺序 | 完成 |
| `IV2ServiceManager` | `@Primary` | v2 service manager 在 Spring 中作为主 Bean | 已移除 core 注解；如后续仍需要 Spring primary 语义，由 adapter 或具体 Spring 模块负责 | 完成 |
| `isass-nocode-core/v2/service/*` | Spring `Ordered`、`@Primary` | nocode v2 service 链路 | 已与 core-common v2 service 同步移除排序接口继承，排序读取由 `IsassOrderUtil` 承载 | 完成 |
| `isass-nocode-core/src/test` | `@SpringBootTest`、`@Transactional` | 集成测试事务和 Spring 容器启动 | main 源码解耦后，测试拆成纯 Java 单测和 Spring adapter 集成测试 | P2 |
| `StringToV2WhereConditionConverter` | 依赖历史 `common.structure` v2 查询条件 | JSON 字符串转 v2 查询条件 | 已迁到 `isass-nocode-core` 的 `vip.isass.framework.nocode.v2.converter` 包，Spring Boot adapter 继续注册 | 完成 |
| `SensitiveDataProperty` | 引用历史 `common.structure` v2 entity 常量 | 默认查询时过滤敏感字段 | 已改为 core 自有字段名常量，不再依赖 v2 entity | 完成 |
| `isass-core-common/pom.xml` | `spring-boot`、`spring-context`、`spring-aop`、`spring-boot-starter-json` | 原先为运行时 Bean 工具、Converter、AopUtils、LoggingSystem 等提供依赖 | 已移除 Spring 依赖和 Boot JSON starter；core 保留明确的 Jackson 2 / Jackson 3 依赖。`isass-nocode-core` 自身使用 Jackson 3，已显式声明 `tools.jackson.core:jackson-databind` | 完成 |
| `isass-core-common/src/main/resources/banner.txt`、`logback-spring.xml` | Spring Boot banner 占位符、Spring Boot logback conversionRule | Spring Boot 应用默认 banner 和日志配置 | 已迁到 `isass-adapter-springboot/src/main/resources`；core-common 不再携带 Spring Boot 专属运行时资源 | 完成 |

## 已完成的阶段性迁移

- `isass-core-common` 的 Spring Boot auto-configuration imports 已移除。
- `isass-adapter-springboot` 已成为 Spring Boot 运行时装配入口。
- Spring MVC 异常映射已迁到 `isass-web-springmvc`。
- `MsAuthenticationHeaderProvider` 的 Spring 配置注入迁到 `isass-security-springsecurity`。
- `RoleCodeServiceManager` 的 Spring 装配迁到 `isass-security-springsecurity`。
- `SelectOptionServiceManager` 已变为纯 Java 聚合器，Spring 装配迁到 `isass-adapter-springboot`。
- `AutoDestroyManager` 已从 core 删除，Spring 生命周期监听器迁到 `isass-adapter-springboot`。
- `DbEntityConvert`、`V2DbEntityConvert` 已移除 Spring 注解，`info.package` 由 `isass-adapter-springboot` 注入。
- 排序语义已从 Spring `Ordered/@Order/@Primary` 迁移到 `@IsassOrder/IsassOrderUtil`，并保留对 Spring `@Order` 的无编译依赖兼容读取；`IsassOrdered` 已删除。
- converter 体系已移除 core 对 Spring Converter 和 `@Component` 的依赖；Spring Boot 场景由 `IsassSpringConverterAdapter` 注册到 Spring conversion service。
- `BeanProviderUtil` 已替代 `SpringContextUtil`，且不保留旧类；Spring 运行时能力由 adapter 的 `SpringBeanProvider` 注入。
- `LogUtil` 已改为委托 `LogLevelManager`，Spring Boot 的 LoggingSystem 操作迁到 adapter。
- `ReflectUtils` 已移除 Spring AOP 依赖。
- `isass-core-common` 已移除 Spring 相关 Maven 依赖。
- `isass-core-common` 的 `banner.txt` 和 `logback-spring.xml` 已迁到 `isass-adapter-springboot`，core-common 不再携带 Spring Boot 专属运行时资源。
- `isass-nocode-core` 的 main 源码已不再反向引用 `vip.isass.framework.common.structure`，v2 迁移包内已补齐批量保存、未实现方法异常、db entity 和 db entity 转换器。
- `isass-core-dependencies` 已纳入 `isass-nocode-core` 版本管理。
- `isass-web-springmvc`、`isass-database-core`、`isass-database-mybatisplus`、`isass-adapter-springboot` 和 `isass-service-attachment` 已迁到 `vip.isass.framework.nocode.v2`，`common.structure` 暂时只作为历史兼容包保留。
- `StringToV2WhereConditionConverter` 已从 core-common 迁到 nocode v2，`SensitiveDataProperty` 已移除对历史 v2 entity 的引用；core-common 外部已无 `common.structure` 引用。
- `V2MybatisPlusGenerator` 的模板变量和 nocode MyBatis XML namespace 已迁到 `vip.isass.framework.nocode` 包名。
- API 文档由 Smart-Doc 生成标准 OpenAPI，`OpenApiEnhancerSpi` 在 Web 边界完成 V3 路径折叠，Knife4j UI 位于独立的 `isass-apidoc-openapi3` 模块。
- `isass-core-common` 已显式声明 `slf4j-api`，不再依赖 Swagger/Knife4j 或其他传递依赖间接提供日志 API。
- `isass-nocode-core` 已新增 v3 operation pipeline、provider router、cache facade/cache operation 等纯 Java 基础抽象，用于替代 v1/v2 的 service 排序链承载缓存/事件等增强的旧模式。
- `isass-nocode-core` 中整文件注释的旧 lowcode MyBatis Plus 源码和未引用的 nocode `ICommonMapper`/XML 已删除；可运行的 MyBatis Plus 实现保留在 `isass-database-mybatisplus`。
- `LoginUserUtil`、`LongSequence`、`SystemClock` 已从主动读取 `BeanProviderUtil` 改为显式 provider/setter，Spring Boot 运行时由 `isass-adapter-springboot` 通过 `ObjectProvider` 桥接。
- 历史兼容包和 nocode v2 的 `IAnyJsonEntity` 已从主动读取 `BeanProviderUtil` 改为显式 `IDictTranslationProvider` provider/setter，并补充未配置 provider 时的跳过行为测试。
- 数据库自动建库的 Spring `ApplicationContextInitializer` 已从 `isass-database-core` 迁到 `isass-adapter-springboot`，并通过反射发现 `DatabaseInitializerManager`；未依赖数据库模块的业务只依赖 Spring Boot adapter 时不会被强制带入 database-core。
- `DatabaseExceptionMapping`、`BuildInDatabaseExceptionMapping` 已移除 `@Component`，`DatabaseExceptionMapping` 已迁到 `isass-adapter-springboot` 的 `IsassDatabaseSpringBootAutoConfiguration`，通过 `@ConditionalOnClass(name=...)` 和反射按 database-core classpath 条件装配；`BuildInDatabaseExceptionMapping` 仍由 mybatisplus 自动配置显式注册。
- MyBatis Plus typehandler、`LongSequenceImpl`、`SystemClockImpl`、MySQL mapper location provider 已移除 `@Component`，改由 MyBatis Plus 自动配置显式注册；剩余 `@ComponentScan` 作为 Spring-bound 模块边界后续继续收缩。
- `DatabaseAutoConfiguration` 已删除；`DatabaseMybatisPlusAutoConfiguration`、PostgreSQL MyBatis Plus 自动配置已移除多余 `@ComponentScan`；MyBatis Plus 主配置改为显式 `@Import(SqlSessionConfig.class)`。
- MySQL 公共 repository 已移除 `@Repository` 和字段注入，改为构造器注入并由 MySQL 自动配置显式注册；MyBatis mapper 仍由 `@MapperScan` 发现。
- `SqlSessionConfig` 已从字段注入迁到构造器注入，通过 `ObjectProvider` 收集可选 mapper location provider 和 typehandler，便于后续继续抽离 Spring 配置边界。
- `IsassServiceLoader` 已作为第一阶段 Java SPI 发现工具落地；`isass-core-common` 和 `isass-nocode-core` 通过 `META-INF/services/vip.isass.framework.common.support.Converter` 暴露默认 converter，Spring Boot adapter 合并 Spring Bean converter 与 SPI converter。
- `IExceptionMapping` 已接入 Java SPI；core-common、web-springmvc、database-core、database-mybatisplus 通过 `META-INF/services` 暴露内置异常映射，`ExceptionAdvice` 合并 Spring Bean 映射和 SPI 映射，业务自定义 Bean 优先于同 class 的 SPI 默认实现。
- `BeanProviderUtil` 和 `LogUtil` 已提供显式 `set*FromServiceLoader` 初始化入口；非 Spring runtime 可以通过 Java SPI 提供 no-arg 实现，也可以在 adapter 启动时继续主动调用 setter 注入带运行时上下文的实现。
- `isass-mq-core` 已移除 Spring Boot 自动配置入口和 `SmartLifecycle` / stereotype / `@ConfigurationProperties` 直接依赖；MQ Spring Boot 装配迁到 `isass-adapter-springboot`，通过 `IsassFeature.MQ_CORE` 按 classpath 条件注册 `DynamicMqProperties`、`MqManager`、旧 producer/consumer initializer 以及 `SmartLifecycle` 桥；SpringEvent/Kafka011 provider 模块改为显式声明自身 Spring 依赖，不再从 mq-core 传递获得。
- `isass-mq-redisstream`、`isass-mq-redispubsub` 的 factory 和 producer 主体已去掉 Spring stereotype / `Assert` 工具依赖；Spring 只保留在各自 auto-config 的显式 `@Bean` 装配边界。

## 尚未迁移的兼容边界

- `isass-core-common/src/main/java/vip/isass/framework/common/structure/**` 仍保留历史 v2 包名，用于兼容未迁移的业务微服务和工具代码。
- `isass-database-core` 的 Spring 绑定已初步归类：
  - `DatabaseAutoConfiguration` 已删除；原 `DatabaseExceptionMapping` 注册已迁到 Spring Boot adapter 的 classpath 条件装配。
  - Spring Boot adapter 已新增 `@ConditionalOnIsassFeature` / `IsassFeature`，以 marker class 统一描述按 classpath 激活的可选功能装配；当前已用于 database-core 异常映射桥。
  - `LiquibaseServiceNaming` 已抽出为纯 Java 命名规则，负责服务级 changelog 路径和 Liquibase history/lock 表名；`AbstractLiquibaseConfiguration` / `LiquibaseConfigurer` 已移除 Spring 工具类依赖。
  - `AbstractLiquibaseConfiguration` / `LiquibaseConfigurer` 已迁到 `isass-adapter-springboot` 的 `vip.isass.framework.adapter.springboot.database.liquibase` 包，`SpringLiquibase`、`LiquibaseProperties`、`ResourceLoader` 桥接不再放在 database-core 源码中。
  - `isass-database-core` 已移除 Spring Boot JSON/JDBC/Liquibase/Test starter 的 compile 依赖；使用 Spring Boot Liquibase 桥的业务服务需要显式依赖 `spring-boot-starter-liquibase`。
  - `isass-database-dameng` 已拆分为达梦厂商扩展模块，承载达梦驱动 optional 依赖、`db-migration-dameng-liquibase`、Javassist 和达梦 ResultSetMetaData 修补类。
  - `src/main/resources/template/**` 与 `v2Template/**` 中的 Spring MVC、Feign、Service、Repository 注解属于代码生成输出模板，短期按“生成 Spring 业务代码”的边界处理，不作为运行时 core 解耦阻塞项。

## 建议迁移顺序

1. **SPI 化**：converter、exception mapping 已先接入 Java SPI；BeanProvider、LogLevelManager 已提供显式 SPI 初始化入口。下一步继续扫描非 core 模块，把仍可解耦的 Spring 注解和工具类迁出功能实现。
2. **database adapter 化**：继续处理代码生成模板的 v3 输出边界，避免新生成代码继续绑定历史 v2/Spring 结构。
3. **其他模块解耦**：继续扫描 `isass-*` 非 core 模块中可迁移的 Spring 依赖，优先剥离纯工具类依赖，再拆 HTTP client、配置读取、资源读取、生命周期回调等真实运行时边界。

## 风险

- `BeanProviderUtil` 仍是静态门面，长期可以继续拆为更细的 registry/provider，减少核心代码对全局静态上下文的依赖；对于需要运行时上下文的 adapter，主动 setter 仍比强制 SPI no-arg 更合适。
- converter 同时兼容 Hutool 和 Spring 的历史语义，目前 Spring MVC 参数绑定由 adapter 桥接维持；已通过 `IsassServiceLoader.mergeByClass` 让运行时 Bean 优先于同 class 的 SPI 默认实现，后续仍需避免不同 class 但 source/target 相同的默认 converter 产生歧义。
- database 相关模块当前大量依赖 Spring Data/MyBatis-Plus Spring 生态，短期作为 Spring-bound 实现处理，不应阻塞 `isass-core-*` 解耦。
