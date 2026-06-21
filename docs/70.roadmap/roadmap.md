# isass roadmap

> 说明：本文件合并原 `2024.md`、`2025.md`、`2026.md` 与 `docs/roadmap-pending-analysis.md`，后续作为唯一 roadmap 入口维护。  
> 维护规则：每完成一项，必须在对应任务的“完成记录”补充实际完成内容、验证命令和提交信息；正在进行的任务必须同步“已完成步骤”和“下一步”。

## 状态说明

- `[x]` 已完成：功能或重构已落地，并记录完成内容。
- `[~]` 进行中：已完成部分步骤，仍有明确剩余步骤。
- `[ ]` 未开始：需要按方案执行。
- `[-]` 暂不实施：已明确暂时不排期，后续如重启需重新评估。

## 一、基础架构与模块治理

### 1.1 模块命名与版本升级

- [x] **模块重命名**
  - 完成记录：
    - `super-*` 已迁移为 `isass-framework-*`，后续又将模块名统一为 `isass-*`。
    - 包路径保持 `vip.isass.framework.xxx`，避免业务代码大范围改包。

- [x] **JDK、Spring Boot、Spring Cloud 与依赖升级**
  - 完成记录：
    - JDK 已升级到 25。
    - Spring Boot / Spring Cloud 已升级到当前框架目标版本。
    - `isass-core-dependencies` 作为依赖版本管理入口继续承接 BOM 和内部模块版本。

- [~] **核心模块与 Spring 解耦**
  - 目标：
    - `isass-core-*` 必须不依赖 Spring。
    - 其他 `isass-*` 尽全力解耦 Spring。
    - Spring Boot、Micronaut、Solon、Quarkus 等运行时通过 adapter 层接入。
  - 已完成步骤：
    - 新增 `docs/design/core-spring-decoupling-analysis.md`，记录 `isass-core-*` Spring 使用点和迁移方案。
    - `isass-core-common` main 源码已移除 Spring 编译依赖。
    - 排序、converter、异常映射、运行时 BeanProvider、LogUtil、ReflectUtils 已迁到 core 抽象 + Spring Boot adapter 桥接。
    - `SpringContextUtil` 已移除，替换为 `BeanProviderUtil`。
    - 新增 `isass-adapter-springboot` 第一阶段，作为 Spring Boot 运行时适配入口。
    - `isass-core-common` 的 Spring Boot auto-configuration imports 已移除。
    - 数据库自动建库 Spring initializer 已从 `isass-database-core` 迁到 `isass-adapter-springboot`，并通过反射避免 adapter 强制传递 database-core。
    - database / mybatisplus 异常映射、MyBatis Plus typehandler、序列、时钟和 MySQL 公共 repository 已移除 Spring stereotype 注解，改为自动配置显式注册。
    - database-core、MyBatis Plus 主配置、MySQL 配置和 PostgreSQL 配置已移除多余 `@ComponentScan`。
    - `SqlSessionConfig` 已迁到构造器注入。
    - 2026-06-21：已扫描 `isass-core-*` main 源码，无 Spring 类型/注解残留命中。
    - 2026-06-21：`isass-core-common` 的 `banner.txt` 和 `logback-spring.xml` 已迁到 `isass-adapter-springboot`，core-common 不再携带 Spring Boot 专属运行时资源。
    - 2026-06-21：已扫描 `isass-database-core` 的 Spring 绑定；运行时边界集中在 Liquibase Spring 配置，代码生成模板中的 Spring 注解短期按生成 Spring 业务代码处理。
    - 2026-06-21：`DatabaseAutoConfiguration` 中的 `DatabaseExceptionMapping` 注册已迁到 `isass-adapter-springboot` 的 `IsassDatabaseSpringBootAutoConfiguration`，通过 `@ConditionalOnClass(name=...)` 和反射按 database-core classpath 条件装配；database-core 已删除原自动配置入口。
    - 2026-06-21：已新增纯 Java `LiquibaseServiceNaming`，承接服务级 changelog 路径和 Liquibase history/lock 表名规则；`AbstractLiquibaseConfiguration` 不再内联路径规则，`LiquibaseConfigurer` 已移除 Spring `CollectionUtils` / `StringUtils` 工具依赖。验证：`mvn -pl isass-database-core -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`。
    - 2026-06-21：`AbstractLiquibaseConfiguration` / `LiquibaseConfigurer` 已从 `isass-database-core` 迁到 `isass-adapter-springboot`，SpringLiquibase、LiquibaseProperties、ResourceLoader 桥接归入 Spring Boot adapter；adapter 对 database-core、Liquibase、Boot Liquibase 依赖使用 optional，attachment 已改用 adapter 包路径。验证：`mvn -pl isass-adapter-springboot -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`、`mvn -pl isass-service-attachment-service -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`。
    - 2026-06-21：`isass-database-core` 已移除 `spring-boot-starter-json`、`spring-boot-starter-jdbc`、`spring-boot-starter-liquibase`、`spring-boot-starter-test` 的 compile 依赖；测试改用 JUnit、AssertJ、Spring Test 的 test scope，attachment-service 显式依赖 `spring-boot-starter-liquibase`。验证：`mvn -pl isass-database-core -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`、`mvn -pl isass-service-attachment-service -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`、`mvn -pl isass-database-core dependency:tree -Dincludes=org.springframework.boot,org.springframework,org.liquibase -Dscope=compile -Dmaven.javadoc.skip=true`。
    - 2026-06-21：Spring Boot adapter 已新增 `@ConditionalOnIsassFeature`、`IsassFeature` 和 `OnIsassFeatureCondition`，以统一 marker class 方式表达按 classpath 激活的功能装配；`IsassDatabaseSpringBootAutoConfiguration` 已改用 `IsassFeature.DATABASE_CORE`。验证：`mvn -pl isass-adapter-springboot -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`。
    - 2026-06-21：新增 `isass-database-dameng` 模块，承载达梦驱动 optional 依赖、`db-migration-dameng-liquibase`、Javassist 和 `DmdbResultSetMetaDataModifier`；`isass-database-core` 不再默认携带达梦厂商扩展。验证：`mvn -pl isass-database-core,isass-database-dameng -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`、`mvn -pl isass-database-core dependency:tree -Dincludes=com.dameng,com.github.mengweijin,org.javassist,org.liquibase -Dscope=compile -Dmaven.javadoc.skip=true`、`mvn -pl isass-database-dameng dependency:tree -Dincludes=com.dameng,com.github.mengweijin,org.javassist,org.liquibase -Dscope=compile -Dmaven.javadoc.skip=true`。
    - 2026-06-21：新增 `IsassServiceLoader` 作为 Java SPI 发现工具；`isass-core-common` 和 `isass-nocode-core` 通过 `META-INF/services` 暴露默认 `Converter`；Spring Boot adapter 改为合并 Spring Bean converter 与 SPI converter，非 Spring 运行时可复用同一套 ServiceLoader 发现机制。验证：`mvn -pl isass-adapter-springboot -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`。
    - 2026-06-21：`IExceptionMapping` 已接入 Java SPI；core-common、web-springmvc、database-core、database-mybatisplus 通过 `META-INF/services` 暴露内置异常映射；`ExceptionAdvice` 改为合并 Spring Bean 映射与 SPI 映射，并移除 Web 内置异常映射的 `@Component`。验证：`mvn -pl isass-core-common,isass-web-springmvc -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`、`mvn -pl isass-adapter-springboot -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`、`mvn -pl isass-database-mybatisplus -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`。
  - 下一步：
    - 继续把日志级别管理、运行时 BeanProvider 等 adapter 接入点整理为可被非 Spring 运行时复用的 SPI/注册表边界。
  - 验证方式：
    - `rg -n "org\\.springframework|jakarta\\.annotation\\.Resource|@Component|@Service|@Repository|@Controller" isass-core-*/*/main`
    - 相关模块执行 `mvn -pl <module> -am test -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false`

### 1.2 配置、构建与部署

- [ ] **配置文件统一为 TOML**
  - 目标：
    - 合并历史 yml/properties 配置，统一放到 `resources/config`。
    - 保留旧配置的兼容读取窗口，避免业务服务一次性迁移风险。
  - 执行步骤：
    - 盘点框架和 attachment 中使用的配置前缀，形成 TOML key 命名规范。
    - 选择 TOML 解析库，优先考虑轻量、无 Spring 强绑定方案。
    - 在 core 层定义配置抽象，在 Spring Boot adapter 中桥接 Environment。
    - 先迁移 apidoc、service-docs、nocode v3 配置，作为样板。
    - 增加旧 key 到新 key 的兼容测试。

- [-] **Docker 分层优化**
  - 暂不实施原因：
    - 当前优先级低于核心解耦和低代码 v3。
    - 后续需要结合最终打包方式、GraalVM 路线和部署体系统一评估。
  - 重启条件：
    - 微服务打包方式稳定后，再评估 fat jar、lib 外置、Spring Boot layer、原生 Dockerfile 的取舍。

- [ ] **GraalVM 原生编译支持**
  - 目标：
    - 打通 native image 技术路线，降低云原生启动和内存成本。
  - 前置条件：
    - 核心模块 Spring 解耦基本完成。
    - 反射、资源扫描、动态代理入口有可枚举的配置。
  - 执行步骤：
    - 先选 `isass-service-attachment` 作为 native smoke test 项目。
    - 梳理 Jackson、MyBatis Plus、smart-doc、zyplayer、ServiceLoader 的 native hint。
    - 先跑 JVM 构建绿灯，再加 native 构建 profile。
    - 把失败项拆成反射配置、资源配置、动态代理配置三类修复。

## 二、API 文档与服务文档

### 2.1 Swagger/Knife4j 替换

- [x] **删除 Swagger/Knife4j，集成 smart-doc + zyplayer-doc**
  - 完成记录：
    - `isass-web-swagger` 旧模块已移除。
    - `isass-core-dependencies` 已删除 `knife4j-dependencies` BOM 和 `isass-web-swagger` 版本管理。
    - 框架源码里的 `io.swagger.annotations` 已移除。
    - 接口文档改为 smart-doc 生成 OpenAPI/Markdown，zyplayer-doc 展示与在线调试。
    - `isass-apidoc-zyplayer` 已实现对接 zyplayer-doc 的空间、版本、目录、API 文档和 Markdown 文档同步。
    - attachment 已作为首个适配项目验证 `service-docs/api`、`service-docs/database`、`service-docs/guide`、`service-docs/design` 目录约定。

- [x] **集成数据库文档生成工具 screw**
  - 完成记录：
    - 框架文档已说明开发阶段通过 Maven 主动生成数据库 Markdown 文档，避免自动化构建阶段强依赖数据库环境。
    - 生成文档放入 `resources/service-docs/database` 后可被 service-docs 接口暴露，并同步到 zyplayer-doc。

### 2.2 API 文档后续增强

- [~] **zyplayer-doc 同步稳定性**
  - 已完成步骤：
    - 空间名改为微服务中文名。
    - 空间版本改为主版本格式，如 `v4.x`。
    - 空间唯一标识改为 `applicationName + 时间戳后缀`，避免 zyplayer-doc 回收站软删除导致固定 uuid 冲突。
    - 一级目录顺序约定为：`api接口`、`使用文档`、`设计文档`、`数据库文档`。
    - 支持按 controller `@Tag` 中文名分组 API，接口名使用 smart-doc/Javadoc 注释。
    - 支持 `exclude-paths`、`exclude-path-patterns` 过滤 `url` 或 `METHOD url`。
  - 下一步：
    - 深入确认 zyplayer-doc “实时版本”和具体版本的数据落点、editVersion 冲突控制和已有文档改版本规则。
    - 为版本查询、版本创建、目录查重、文档查重补充自动化测试。
    - 在 attachment 重启两次验证空间、版本、一级目录、API 文档不重复创建。

- [ ] **服务文档暴露协议固化**
  - 目标：
    - 微服务统一暴露 `/{service-name}/service-docs`。
    - API JSON 暴露 `/{service-name}/v3/api-docs`，数据源使用 `service-docs/api/openapi.json`，不再依赖 SpringDoc。
  - 执行步骤：
    - 移除 SpringDoc 依赖和自动生成链路。
    - 添加 `v3/api-docs` controller，读取 `service-docs/api/openapi.json`。
    - 当 openapi.json 不存在时返回明确错误或空文档策略。
    - attachment 重新生成 smart-doc 文档并验证接口返回。

## 三、异常体系

- [x] **Resp 新增 detailMessage**
  - 完成记录：
    - `Resp` 已新增 `detailMessage` 字段。
    - `ExceptionAdvice` 在生产统一提示时将用户可见消息放入 `message`，将 traceId + 原始异常详情放入 `detailMessage`。
    - 目标是开发排查方便，同时避免内部异常细节混入用户提示。

- [x] **IsassErrorController 错误响应策略设计**
  - 完成记录：
    - 新增 `docs/design/error-response-strategy.md`。
    - `IsassErrorController` 对 `Accept: text/html` 的页面/静态资源错误保留 HTTP 状态。
    - JSON/API 请求返回 `Resp` 结构。
    - 修复状态映射列表为空时的 NPE 风险。
  - 后续记录：
    - “同一个 404 何时返回 HTTP HTML/空响应，何时返回 Resp”的完整场景矩阵已记录到设计文档，后续如修改错误链路需先更新该文档。

- [~] **重构异常模块，优化异常抛出接口**
  - 已完成步骤：
    - `Resp.detailMessage` 与 `ExceptionAdvice` 双字段错误信息已落地。
    - `IsassErrorController` 已有 HTML/JSON 错误响应策略。
  - 下一步：
    - 统一框架异常基类、异常码、HTTP 状态映射、用户提示消息和详细诊断消息的关系。
    - 将异常映射接口保持在 core 抽象层，Spring Web 只负责 HTTP adapter。
    - 为常见异常增加映射测试：参数校验、权限、404、业务异常、未知异常。

- [ ] **异常码按模块分类**
  - 目标：
    - 微服务按端口或 `ModuleInfo` 分类。
    - isass 框架异常码按模块分类。
    - 支持捕获已有异常转换成约定异常码和异常消息。
  - 执行步骤：
    - 设计异常码范围规范，例如 core/web/database/nocode/apidoc/security。
    - 扩展 `ModuleInfo` 或新增异常码元数据对象。
    - 在异常映射层支持按异常类型、模块、状态码解析统一错误响应。
    - 补充单元测试和文档。

## 四、低代码 v3 核心设计

### 4.1 模块与接口边界

- [~] **低代码子模块 DDD 重设计**
  - 目标：
    - `isass-core-nocode` 已重命名为 `isass-nocode-core`。
    - 未来允许出现 `isass-nocode-xxx` 模块。
    - v1/v2 历史接口保留，v3 按 DDD 和 operation pipeline 重设计。
  - 已完成步骤：
    - `isass-nocode-core` 已新增 v3 operation pipeline、provider router、access handler。
    - 已新增标准 CRUD 操作名、access request 工厂、参数契约和请求校验。
    - 已新增 cache facade/cache operation。
    - 已新增自定义实体标记接口、实体/字段元数据、实体元数据 SPI provider、实体注册表。
    - 已新增 Map/List 化查询条件、查询元数据校验器、空字符串查询策略。
    - v2 自有包已补齐 `BatchSave`、`UnimplementedMethodException`、`IV2DbEntity`、`V2DbEntityConvert`。
    - `isass-web-springmvc`、`isass-database-core`、`isass-database-mybatisplus`、`isass-adapter-springboot` 和 `isass-service-attachment` 已迁到 `vip.isass.framework.nocode.v2`。
  - 下一步：
    - 按 `docs/design/nocode-v3-service-routing-cache.md` 继续补齐 v3 adapter 和 ORM 执行层。
    - 明确 v1/v2 与 v3 的共存边界，避免新功能继续扩展旧 service 排序链。

- [x] **service 逻辑事件监听**
  - 完成记录：
    - `NocodeOperationListener` 和 `NocodeOperationListenerInterceptor` 已新增 before/after/error 监听底座。
    - 已接入 v3 operation pipeline。
    - 业务可按操作粒度添加前置、后置和异常监听逻辑。

- [x] **JSR303 风格实体字段校验与分组校验**
  - 完成记录：
    - `NocodeFieldConstraint`、`NocodeEntityValidator` 和 `NocodeCrudValidationInterceptor` 已新增。
    - 支持 create/update 分组校验和 v3 save/update 写入前校验。
    - `BuildInWebExceptionMapping` 已优化 `BindException` / `MethodArgumentNotValidException` 的字段错误消息。

- [x] **Long 时间戳调试辅助方法**
  - 完成记录：
    - `NocodeEntity` 已新增 `formatTimestamp(Function)` 和 `setupTimestamp(String, BiConsumer)` 默认方法。
    - 用于调试 Long 毫秒时间戳字段。

### 4.2 查询、分页和实体元数据

- [~] **criteria 简化与条件分组**
  - 已完成步骤：
    - 已新增 `NocodeQueryCriteria`、`NocodeQueryCondition`、`NocodeQueryGroup`。
    - 支持条件列表和分组表达 equals、in、contains、or 等查询。
    - 已新增 `NocodeQueryValidator`，基于字段元数据校验未知字段、不可查询字段和不可排序字段。
    - `isass-web-springmvc` 已新增 query 参数到 v3 criteria 的 parser。
  - 下一步：
    - 扩展 HTTP query 表达式，支持 `field__operator=value` 或等价语法。
    - 将 v2 生成模板迁移到 v3 查询模型。
    - 在 ORM adapter 中把 v3 criteria 转换为 MyBatis Plus/sqltoy 查询。

- [~] **分页对象优化**
  - 已完成步骤：
    - 已新增纯 Java `NocodePageRequest` / `NocodePageResult`。
  - 下一步：
    - ORM adapter 将 MyBatis Plus、sqltoy 等分页对象转换为统一 v3 模型。
    - Spring MVC controller 层只暴露 v3 分页对象，不直接绑定某个 ORM。

- [~] **自定义实体继承 v3 接口**
  - 已完成步骤：
    - 已新增 `NocodeEntity` 标记接口。
    - 已新增 `NocodeEntityDefinitionProvider` + `ServiceLoader` 自动发现。
  - 下一步：
    - 在 attachment 中挑一个自定义实体验证元数据暴露。
    - 为非自动生成实体补充分页查询、排序查询、字段校验示例。
    - 让 ORM adapter 能使用这些元数据执行查询。

- [~] **取消 db 实体，探索 ORM 无关实体**
  - 已完成步骤：
    - 已新增 `NocodeEntityDefinition` / `NocodeFieldDefinition` 作为 ORM 无关实体描述。
  - 下一步：
    - 分析 MyBatis Plus `TableInfo` 动态注册或绑定方式。
    - 评估 Lombok 自定义注解或 Javassist 动态修改源码的必要性。
    - 设计 ORM adapter 将 v3 元数据绑定到 MyBatis Plus 和未来 sqltoy。

- [ ] **数据库字段注释描述关系**
  - 目标：
    - 在数据库字段注释中描述表/字段关系，便于框架分析生成领域对象。
  - 执行步骤：
    - 定义注释格式，例如 `relation:one-to-many:detail_table.foreign_id`。
    - screw 数据库文档生成时保留这些关系描述。
    - v3 元数据 provider 解析关系并生成 `NocodeEntityRelation`。

### 4.3 access 接入层与动态 Controller

- [~] **新增 access 接入层**
  - 目标：
    - controller、socketio、kafka、定时任务等入口统一转换为 `NocodeAccessRequest`。
    - 本地/远程服务选择由 provider router 决定。
    - 缓存、审计、事件监听归 operation interceptor。
  - 已完成步骤：
    - 纯 Java `NocodeAccessRequest` / `NocodeAccessHandler` / `NocodeCrudAccessRequests` 已落地。
    - `NocodeCrudAccessDefinition` / `NocodeAccessRequestValidator` 已统一标准 CRUD 操作名、请求参数名、必需参数、可选参数和请求校验。
    - Spring MVC adapter 已落地 route、request factory、endpoint invoker、query criteria parser 边界。
  - 下一步：
    - 在 `isass-web-springmvc` 中实现动态 endpoint 注册，复用现有 route/factory/invoker/parser。
    - 先实现一个通用 controller 路径模式：`/nocode/{entityName}`、`/nocode/{entityName}/{id}`。
    - 对接 `NocodeAccessHandler`，验证 list/page/find/save/update/delete。
    - 再评估 socketio、kafka、定时任务 adapter 的最小公共参数模型。

- [~] **v3 通用 Controller 动态生成**
  - 已完成步骤：
    - `NocodeSpringMvcCrudRoute` 定义默认 HTTP method/path descriptor。
    - `NocodeSpringMvcQueryCriteriaParser` 解析 HTTP query 参数。
    - `NocodeSpringMvcCrudRequestFactory` 把 path/query/body 转换为 `NocodeAccessRequest`。
    - `NocodeSpringMvcCrudEndpointInvoker` 将请求交给 `NocodeAccessHandler` 执行。
  - 下一步：
    - 设计 Spring MVC 动态注册点，优先考虑 `RequestMappingHandlerMapping` 注册 handler method。
    - 编写最小可运行测试：注册 GET `/nocode/{entityName}/{id}`，请求后命中测试 provider。
    - 决定 API 文档展示方式：默认一个通用 controller，还是按实体生成虚拟分组。
    - 把动态 endpoint 纳入 attachment 单体启动验证。

- [ ] **级联 controller 分组方式**
  - 目标：
    - 支持 API 文档中按实体、领域或 controller tag 分组展示通用接口。
  - 执行步骤：
    - 先确定 v3 通用 controller 是单 controller 还是按实体虚拟 controller。
    - 如果只使用一个 controller，实体路径参数需要在 API 文档中提供可选枚举或说明。
    - smart-doc/zyplayer-doc 同步时生成更符合前端阅读的分组结构。

### 4.4 v3 ORM adapter 与增强功能

- [ ] **多个 ORM 框架同时支持**
  - 目标：
    - 支持 MyBatis Plus、sqltoy 等 ORM 快速切换。
  - 前置条件：
    - ORM 无关实体元数据稳定。
    - v3 criteria/page/result 已稳定。
  - 执行步骤：
    - 定义 `NocodeRepositoryProvider` 或类似纯 Java 接口。
    - MyBatis Plus adapter 实现 CRUD、criteria、分页、排序。
    - sqltoy adapter 作为第二实现验证抽象是否足够。
    - attachment 先保留 MyBatis Plus，避免一次性替换。

- [~] **通用新增/修改自动赋值和前端只读字段**
  - 已完成步骤：
    - `NocodeFieldDefinition` 已新增 `clientWritable` 和 `NocodeFieldAutoFill` 元数据。
    - `NocodeCrudWritePayloadProcessor` 和 `NocodeCrudWriteInterceptor` 已支持 Map body。
    - 已支持过滤前端只读字段并填充创建/更新时间。
  - 下一步：
    - Spring MVC 动态 controller 接入 body 解析后，验证 save/update 自动赋值。
    - ORM adapter 执行写入前复用同一处理链。

- [~] **空字符串查询条件优化**
  - 已完成步骤：
    - 已新增 `NocodeBlankStringPolicy`。
    - `NocodeSpringMvcQueryCriteriaParser` 已支持 HTTP query 参数解析时配置空字符串策略。
  - 下一步：
    - 将策略暴露到配置文件。
    - ORM adapter 执行 criteria 时验证忽略空字符串和匹配空字符串两种语义。

- [~] **级联删除 / 关联表删除**
  - 已完成步骤：
    - 已新增 `NocodeEntityRelation`、`NocodeEntityRelationType`。
    - 已新增 `NocodeDeleteOptions`，deleteById 请求可表达级联删除或关联表删除意图。
  - 下一步：
    - ORM adapter 根据 relation 元数据解析删除计划。
    - 增加事务边界和失败回滚测试。
    - attachment 选择一组主从实体做验证。

- [~] **主从表关联查询**
  - 已完成步骤：
    - `NocodeEntityRelation` 已提供实体关系元数据。
    - `NocodeFetchOptions` 已提供 findById/page/list 的关联查询请求选项。
  - 下一步：
    - ORM adapter 根据 fetch options 加载关联数据。
    - 先支持一对一，再支持一对多。
    - 评估 N+1 查询、批量加载和分页场景的性能边界。

- [ ] **创建时间、修改时间字段改回 bigint(Long)**
  - 目标：
    - 大批量查询业务避免时间对象转换成本。
  - 执行步骤：
    - 盘点框架和 attachment 中时间字段类型。
    - 设计兼容迁移 SQL 和实体字段迁移策略。
    - 先在 v3 元数据中标记时间戳字段，再逐步改业务实体。

### 4.5 代码生成

- [ ] **v3 代码生成器**
  - 目标：
    - 基于 v3 元数据生成实体、repository/provider、配置和服务文档。
  - 前置条件：
    - v3 元数据、criteria、ORM adapter、动态 access 层基本稳定。
  - 执行步骤：
    - 定义生成器输入：数据库元数据、字段注释关系、模块配置。
    - 定义生成器输出：v3 entity definition、实体类、provider 配置、service-docs。
    - 先支持 attachment 的最小实体，再扩展复杂关系。

## 五、文档项目与文档同步

- [-] **使用 vuepress-theme-vdoing 重写文档项目**
  - 暂不实施原因：
    - 当前优先级低于框架核心解耦、低代码 v3 和 API 文档闭环。
  - 重启条件：
    - API 文档、service-docs、框架设计文档路径稳定后，再统一改造文档站。

- [-] **文档项目自动同步具体项目 Markdown**
  - 暂不实施原因：
    - 当前已通过 `service-docs` 和 zyplayer-doc 同步微服务文档，独立文档站同步暂缓。
  - 重启条件：
    - 明确框架文档站与 zyplayer-doc 的边界后，再设计同步机制。

## 六、暂不实施任务

- [-] **repository CTE 递归查询**
  - 暂不实施记录：
    - 2026-06-20：该项暂时取消，不进入当前 roadmap 实现队列。
  - 原始目标：
    - repository 从设计上支持“部分数据库有、部分数据库没有的 SQL 语法”的兼容模式，首先实现 CTE 递归查询。
  - 重启条件：
    - 需要重新评估跨数据库 CTE 兼容策略、降级实现和测试矩阵。

- [-] **开源许可证协议回顾**
  - 暂不实施原因：
    - 当前不影响框架 v4 技术落地。
  - 重启条件：
    - 发布前统一评估 LGPL、Apache-2.0、MIT 等协议取舍。
