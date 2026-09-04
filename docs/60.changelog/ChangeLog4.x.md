# super core

## 更新日志

### 4.0.0-SNAPSHOT

- **OpenAPI 文档能力迁移**：将 OpenAPI 运行时组装、文档 Controller、权限放行和 Knife4j UI 从通用
  `isass-web-springmvc` 迁移至 `isass-service-apidoc`；未依赖 `apidoc-service` 的 Web 应用不再自动暴露 API 文档。
- **Entrypoint API 文档分组排序**：`EntrypointInfo` 新增 `displayOrder`，运行时元数据和 OpenAPI 顶层 tags
  按该值排序，并通过 `x-order` 暴露给 Knife4j；默认值为 `1000`，NoCode 生成服务沿用默认值。
- **NoCode OpenAPI 分组置顶**：即使只有 NoCode 接口，也会在 OpenAPI 顶层 tags 中声明“零代码接口”
  `x-order=1`，避免 Knife4j 将该分组显示在末尾。
- **Knife4j 分组排序值解析修复**：修复前端将数值排序值误判为空值的问题，确保“零代码接口”的
  `x-order=1` 实际参与分组排序并位于第一项。
- **NoCode 分组同序置顶**：当其他 Entrypoint 分组同样使用 `displayOrder=1` 时，OpenAPI 仍优先输出“零代码接口”，
  避免同序分组按注册顺序将其插入中间。
- **单体 API 文档按服务切换**：单体启动时根据 classpath 静态文档和本地 Entrypoint 元数据发现嵌入服务，
  `swagger-config` 为每个服务生成可切换分组，并通过 `/{serviceName}/v3/api-docs` 返回该服务独立文档，避免依赖服务
  的接口全部混入当前应用文档。
- **NoCode Criteria 与 Schema 展示修复**：分页列表等对象型 Query Criteria 改为按 getter 展开，兼容 fluent setter，
  恢复实体切换后的 Criteria 条件；OpenAPI Schema 默认使用 Java 简单类名，避免调试页面显示完整包路径。
- **NoCode Criteria 参数按操作裁剪**：隐藏内部的 `whereConditions` 和 `associationCriteria`，仅在修改接口展示
  `updateMode`、`nullValueMode`、`matchFields`，仅在分页查询展示 `selectColumns` 和关联查询；关联查询使用实际
  HTTP 参数名 `association.query`，Knife4j 不再为可选数组参数自动填充 `[""]`。
- **NoCode Criteria 泛型字段类型修复**：按具体 Criteria 实现类解析继承泛型 getter 的返回类型，避免 `id`、
  `createUserId`、`modifyUserId` 等 Long 类型查询参数退化为 `Serializable` 空对象并在 Knife4j 中显示 `{}`。
- **NoCode OpenAPI 说明与表单参数修复**：生成实体和 Criteria 的字段注释元数据，运行时补充模型、属性和参数说明；
  对象型 `FORM_FIELD` 参数展开为独立 query 参数，`FORM_FILE` 参数按 binary 文件参数输出。
- **API 模型说明注解**：新增 `vip.isass.framework.entrypoint.annotation.ApiDoc`，支持在模型类、字段、记录组件和 Criteria
  getter 上声明 API 文档说明；NoCode 生成器据此输出文档元数据，不再通过实体上的 `PROPERTY_COMMENTS` 静态映射定义字段说明。
- **DDD 模型目录规范**：NoCode 生成器将实体和 Criteria 分别输出到 `domain.model.entity` 与
  `domain.model.criteria`；业务项目模板为 `domain.model.*` 和 `application.model.*` 同步提供
  `entity`、`criteria`、`vo`、`dto`、`req`、`resp`、`enums` 分类目录。
- **Entrypoint record 请求体 Schema 修复**：OpenAPI 组装器补充 Java `record` 组件访问器解析，登录等使用
  record 请求对象的接口不再将请求示例生成为 `{}`，可以展示具体请求字段。
- **Entrypoint OpenAPI 路径修复**：运行时文档组装器改为按字面量写入完整 URL path key，避免 Jackson
  把以 `/` 开头的 Entrypoint 路径误当作 JSON Pointer 并展开成多层对象；`/v3/api-docs` 现在输出符合
  OpenAPI 规范的 `paths["/service/context/resource/operation"]`，Knife4j 可以正常识别并展示接口。
- **Entrypoint OpenAPI 分组与动态 NoCode 投影恢复**：NoCode 八个标准操作统一归入“零代码接口”单一分组，
  每种操作只生成一份 `/{service}/nocode/{entity}/{operationName}` 文档，并恢复 service/entity 下拉、
  Criteria 参数过滤和请求体模型切换元数据，避免每个 CRUD 资源重复出现“增-批量”等接口；实际运行路由仍为
  具体服务和资源路径。只有自定义业务操作才按 `EntrypointInfo.tag` 形成中文独立分组；NoCode 生成器使用实体
  中文注释填充 `tag`，实体下拉显示小驼峰 `resourceName`（值仍为 `contextName/resourceName`），自定义操作的
  `displayName` 统一使用简短中文名称。
- **内部基础设施路由开放**：`InternalAccessBuilder` 新增受限的 `allowRoute`，允许服务通过 Java Provider
  精确开放没有 Entrypoint 接口的框架基础设施 Controller；普通业务入口仍必须使用类型安全的方法引用。
  NoCode 远程初始化客户端改用完整请求上下文生成附加请求头，并发送与签名完全一致的序列化 Body，使内部 HMAC
  可覆盖跨微服务初始化数据导入。
- **Entrypoint Map 键投影修复**：`PropertyPresenceBinder` 在 HTTP/gRPC 请求字段出现性绑定与投影时，
  统一按 JSON 序列化后的字符串键匹配 Java Map，支持 `Map<Long, ?>` 等标量键类型，避免有序 Map 因
  字符串键查询触发 `ClassCastException`，并防止嵌套对象字段在传输边界被错误过滤。
- **内部访问路由与错误语义修复**：`InternalAccessBuilder` 改为复用统一 Entrypoint 元数据注册表，按具体 operation
  区分 NoCode 与自定义路由；继承 `ICrudService` 的业务接口中，自定义方法不再被错误登记为 `/nocode` 路径。
  同时收窄内部 HMAC 过滤器的异常捕获范围，只把签名认证失败转换为 401，下游权限拒绝和业务异常保留原始语义。
- **Agent 规则与使用文档统一维护**：将数据库结构、Liquibase 注释 DSL、NoCode CRUD/关联/生命周期、
  初始化 JSON ID 和业务价值测试等通用规则收敛到 `docs/usage/`；框架、工作区、业务微服务及项目生成模板的
  `AGENTS.md` 仅保留执行摘要、项目专有规则和文档入口，避免框架升级时在多个服务同步复制规则。同步清理
  初始化文档中允许数据库外键及关联文档反向依赖工作区 `AGENTS.md` 的过时描述，并在源码许可证已统一为
  简短 SPDX 后删除各项目重复的“源文件读取优化”章节。
- **Entrypoint 集合 Query 统一为逗号分隔**：HTTP 客户端、服务端、OpenAPI 与前端请求工具统一使用单参数
  英文逗号分隔格式，例如 `idIn=1,2,3`；OpenAPI 数组 Query 声明为 `style=form, explode=false`。
  同名重复 Query 参数不再兼容并会被服务端拒绝，避免重复字段造成 URL 冗长和各端序列化规则不一致。
- **统一内部微服务 HMAC 认证**：删除 Bootstrap 专用 Body 签名和后台任务自带 API Key 的旧链路；Entrypoint
  HTTP 调用统一签名方法、路径、Query 与请求体摘要，并在用户或外部应用请求中继续透传原 JWT/API Key。
  新增 Java `InternalAccessProvider` 精确声明目标服务允许内部访问的 operation，`ROLE/AUTHENTICATED` 采用
  “业务主体授权或内部 HMAC operation 授权”语义，不维护调用方服务名单。`CurrentPrincipalUtil` 分别暴露
  业务主体与可共存的内部服务主体，Auth Bootstrap 复用同一机制且不再使用独立认证过滤器。
- **授权角色展示信息**：`PrincipalAuthorizationContext` 新增按角色编码和名称组成的 `roles` 集合；
  `roleCodes` 继续作为程序鉴权依据，管理端无需维护不完整的角色编码中文映射即可统一显示数据库角色名称。
- **NoCode 生成器服务包自动推导**：删除调用方配置的 `serviceInfoPackageName`，生成器从追加限界上下文后的
  完整 `context` 自动取得服务根包；服务名映射为多段 Java 包（例如 `order-processing` →
  `order.processing`）时也能正确引用 `ServiceInfo`。
- **错误响应文案校正**：统一错误响应不再输出错误分发阶段不可靠的 HTTP Method，只保留状态信息、原始路径
  和错误详情，避免 POST 请求在 `/error` 二次分发后被误报为 GET。
- **NoCode 多层关联查询**：`association.query` 支持 `rolePermissions.permission` 点分路径，自动补齐父路径并
  按层批量装载，每条路径只执行一次目标查询；嵌套 Criteria 使用完整路径命名空间。新增空路径、未知关系
  与最大 16 层深度校验，保持默认不展开、未显式关系不递归的安全边界。
- **设计文档收敛**：删除已经实施完成且与 AGENTS、使用文档及当前代码重复的 Entrypoint/NoCode 讨论稿、
  CRUD 场景实施稿和关联查询设计稿；后续功能变更只维护当前代码、测试、必要使用说明及本更新日志。
- **稳定 Long ID 公共协议**：`Sequence` 新增静态 `stableLongId(identity)`，使用固定的
  UTF-8、SHA-256、摘要前八字节大端序及正数映射协议，把带业务命名空间的字符串稳定映射为正数
  `long`。该方法不依赖运行时 Sequence Provider，并以固定测试向量锁定持久化兼容性；业务仍须通过
  唯一索引或固定 ID 内容校验处理理论哈希碰撞。
- **Entrypoint 访问策略声明**：`UrlAccessSecurityStrategy` 下沉至 `isass-entrypoint-core`，成为全局安全
  配置与单个 `EntrypointOperation.accessStrategy` 共用的策略枚举；操作默认使用 `ROLE` 动态权限校验，
  `NONE` 生成匿名放行 URL，`AUTHENTICATED` 只要求主体认证。Web 自动配置仅收集当前进程本地实现，
  远程代理不会污染本服务规则；手写 Controller 继续使用 `PermitUrlProvider`。原 `allowAnonymous` 布尔字段
  已删除，BSP 登录、HMAC Bootstrap 等入口迁移为 `NONE`，登录后租户选择入口迁移为 `AUTHENTICATED`。
- **403 错误语义修正**：`IsassErrorController` 删除“403、携带 Authorization 且错误分发阶段无 Principal
  即判定 token 失效”的不可靠推断；动态权限拒绝现在稳定返回 403/权限不足，真正的 JWT 认证失败继续由
  JWT 过滤器明确返回 401。
- **NoCode CRUD 生命周期收敛**：八个标准入口分别归一到 `CrudWriteExecutor.superCud` 和
  `CrudQueryExecutor.query` 两个执行边界；新增强类型 `CrudWriteLifecycleContext`、
  `CrudQueryLifecycleContext` 及查询请求/结果模型。生命周期监听器改为 Spring Bean 自动收集，删除静态
  Registry 与手工注册；写回调明确区分事务内执行、真实提交和回滚，查询回调统一覆盖分页、游标分页、
  计数与存在性查询，并仅抑制同一 Service 重入。BSP 缓存、权限约束、存储平台和关联同步监听器已迁移到
  新模型。
- **NoCode 写入口命名精简**：正式批量修改与 Criteria 删除入口由 `updateBatch`、`deleteBatch` 更名为
  `update`、`delete`，URL 同步改为 `/update`、`/delete`；集合 Body 与 Criteria 的批量语义不变，不保留
  旧方法或旧 URL 兼容。单实体 `update(E)`、`update(E,C)` 和 `delete(PK)` 继续作为未发布的 Java 便捷重载。
- **统一主体授权与本地权限映射**：删除框架旧 `ApiKeyAuthenticationService`、URL—角色元数据链路和
  `DynamicRoleAuthorizationManager`；`IAuthorizationService` 统一发布 Body 承载的 `apiKeyContext` 与
  JWT 主体读取的 `jwtContext`，`PrincipalAuthenticationToken` 同时持有已验证主体和授权上下文。
  新增 `EntrypointPermissionResolver` 与 `DynamicPermissionAuthorizationManager`，业务进程只根据本地
  Entrypoint—权限编码映射授权，未映射入口默认拒绝（平台超级开发者保留诊断通道）。JWT 下游调用传播
  原 JWT 且不叠加 API Key；定时任务等无用户上下文的内部调用从
  `isass.security.bootstrap.api-key` 附加服务凭证。API Key/JWT 一旦被识别但认证失败会立即返回 401，
  同一请求出现多种 ISASS 凭证会被拒绝，并为 BSP Bootstrap 根信任安全分支提供可组合配置接口。
- **统一认证远程错误语义**：Entrypoint HTTP 客户端新增 `EntrypointRemoteBusinessException`，显式区分
  远端返回的统一业务失败与网络、协议、反序列化等传输故障；API Key 认证只把 BSP 明确返回的认证失败
  转换为 401，基础设施不可用继续按服务故障上报，避免把 BSP 宕机伪装成无效凭证。
- **单体权限解析与 NoCode 二次鉴权修正**：动态权限管理器聚合当前进程全部
  `EntrypointPermissionResolver`，单体启动时可同时识别 BSP 与业务微服务的本地权限清单；NoCode
  授权上下文补齐 `contextName/resourceName`，操作身份统一为
  `service/context/resource#operation`，与 Java 权限 DSL 创建的 `AuthResource.uri` 完全一致，避免 HTTP
  第一层授权通过后被 NoCode 第二层误拒绝。
- **认证审计完善**：公共请求日志模型增加 `principalType`、`principalId`、`credentialId`，可区分用户、
  服务账号及具体 API Key 凭证；MyBatis-Plus 审计字段继续统一从 `CurrentPrincipalUtil` 获取操作者。
- 修复按限界上下文拆分代码生成任务时忽略 `includeTables/excludeTables` 的问题；数据库发现阶段现在先按
  调用方配置过滤，再按上下文分组，单表增量生成不会误覆盖其他实体和 Criteria。
- **NoCode 与数据库依赖解耦**：`isass-nocode-core` 移除 MyBatis-Plus、JSqlParser 和动态数据源依赖；
  `MybatisPlusRepository`、Wrapper 适配、表元数据注册器及相关测试迁入
  `isass-database-mybatisplus`，并由数据库模块注册 `TableMetaRegistrar`。不使用数据库的服务（如 apidoc）
  现在可以依赖 NoCode、Entrypoint 和 Spring Boot Adapter，而不会触发 JDBC 或动态数据源自动配置。
- 新增 ORM 无关的 `Page<T>` 分页结果，统一由 Repository、`ICrudService.page`、批处理工具和
  自定义应用入口返回；`MybatisPlusRepository` 在基础设施边界把 MyBatis-Plus `IPage` 转换为
  `Page`，业务接口、领域仓储和 OpenAPI 不再暴露 MyBatis-Plus 分页类型，并移除
  `isass-core-common` 对 `mybatis-plus-core` 的无效依赖。运行时 OpenAPI Schema 同时支持泛型属性解析、
  按实际泛型参数隔离组件名以及 `Map` 返回值，避免分页泛型退化为 `Object` 后再次触发 JavaBean 解析异常。
- **Entrypoint 与 NoCode 破坏式重构落地**：新增 `isass-entrypoint-core/registry/http/grpc` 四个模块，以
  `IEntrypoint`、`EntrypointInfo`、`EntrypointOperation` 和五类参数注解在运行时生成本地/HTTP/gRPC
  入口与 OpenAPI；NoCode URL 固定为 `/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}`，
  自定义入口固定为不含 `nocode` 的同构路径，并全面禁止业务 Path 参数。删除旧 `IService`、Manager、
  transport/contract 实现、`isass-nocode-http/grpc` 模块、Maven 合同生成 Goal 与
  `nocode-contract.json`，不提供旧类型或旧 URL 兼容。标准 CRUD 仅发布 `createBatch`、`superCud`、
  `update`、`delete`、`page`、`cursorPage`、`count`、`exists` 八个操作；新增六分组
  `SuperCudReq/SuperCudResult`、独立事务执行器、嵌套保存点条件新增、`MERGE/REPLACE`、
  `IGNORE_NULL/WRITE_NULL`、方向性关联/树形级联、字段出现性掩码和 ID 游标分页。字段出现信息能够在
  Bean、record、集合、数组和 Map 中经 HTTP/gRPC 双向传递；生成实体 setter 会登记显式提交字段。
  NoCode 初始化数据改用 `/{serviceName}/nocode/system/initialization/*` 基础设施入口并按运行时
  Entrypoint 元数据拆分本地/远程实体。BSP 与 Asset 的应用服务已迁移到 Entrypoint/ICrudService，并增加
  全接口合同解析测试。
- 删除已被 Entrypoint 本地实现/远程代理选择机制取代的 `ApiService`、`ApiOrder`、`IsassOrder`、
  `IsassOrderUtil`，同时删除旧公共请求日志服务接口及其无调用方的反射辅助类；v4 不再保留
  “本地/Feign/Manager 按顺序聚合”的旧服务调用链。
- 修复 Entrypoint HTTP 服务端把 `FileStream` 当作普通对象包装成 `{data:{}}` 的问题；文件流契约迁入
  `isass-entrypoint-core`，HTTP 入口现在直接流式写出文件内容、媒体类型、长度及 UTF-8
  `Content-Disposition`，预览与下载接口不再经过 JSON 包装。
- 修复对象 Query 中出现不属于 Criteria 的公共参数时，整个 Criteria 退回 Jackson 绑定并丢失有效条件的
  问题；HTTP 入口现在逐项忽略未知 Query 参数，同时继续调用已知条件的 setter，避免 `appId` 等公共参数
  使分页查询退化为无条件查询。
- 完善自动服务入口与 NoCode 边界设计讨论，确定 `IEntrypoint`、`EntrypointInfo`、含 `displayName`、`description`、`displayOrder` 的 `EntrypointOperation` 及五类参数注解；明确 `ICrudService` 直接继承 `IEntrypoint`，正式 CRUD 方法统一标注入口注解并沿用“增-批量”“查-分页列表”等展示名，单体新增、条件新增、单体删除和 `requireOne` 等默认便捷方法不发布远程入口；确定拆分 `isass-entrypoint-core/registry/http/grpc` 四个模块，registry 仅提供协议无关元数据；各微服务自行维护 Smart-doc 配置并按服务名隔离 OpenAPI 产物，由 `isass-apidoc-openapi3` 按微服务/单体配置合并静态文档和本地 Entrypoint，同时确定不兼容读取旧 `nocode-contract.json`；并补充对象 Query 双向绑定、NoCode 入口层级、固定 URL 鉴权、应用/领域分层、聚合所有权和级联写入方案。
- 将工作区自有源码中重复的 LGPL/Apache 长 Header 规范化为短 SPDX 标识；新增安全优先的六项目 dry-run/apply/check 工具、生成器 SPDX 模板、第三方保护规则与边界测试，并补齐项目许可证构建元数据。
- NoCode `xxxIn` 等集合条件统一使用 `xxxIn=1,2,3`，不再发布或接受同名重复 Query 参数。
- 修复零代码 HTTP 文件预览和下载直接写入中文文件名导致 Tomcat 拒绝 `Content-Disposition` 响应头的问题；文件名现在按 UTF-8 RFC 5987 编码。
- 修复同时包含标准 CRUD 与自定义业务方法的 Entrypoint 被整体归入 NoCode 命名空间的问题；命名空间、HTTP
  客户端、OpenAPI 与二次鉴权现在均按操作判定，标准 CRUD 继续使用 `/nocode/`，同一资源上的自定义业务方法
  使用普通业务路径。

- **零代码初始化跨服务路由**：初始化 JSON 现在按实体所属微服务分组导入，`resources/init` 下的目录仅作分类，不再被误用为目标服务。一个 JSON 可同时包含本地与远程实体；本地直接导入，远程实体按服务合并为一次 HTTP 初始化调用。
- **出站 API Key 与 ROLE 授权收紧**：
  - `SpringApiKeyHeaderProvider` 从 `isass.security.bootstrap.api-key` 读取当前微服务应用凭证，并且仅对 `isass.entrypoint.http.services.*.url`、`isass.entrypoint.http.base-url` 声明的内部服务地址或服务发现实例附加 `X-ISASS-API-Key`，不再向对象存储、AI 平台等任意外部 URL 泄露服务凭证；旧 `isass.http.endpoints` 配置已删除。
  - `DefaultSecurityMetadataSourceProvider` 直接使用 `IAuthorizationService`，单体/BSP 选择本地实现，业务微服务选择 Entrypoint 远程代理；删除旧 `IRoleCodeService`、Manager 与 BSP 适配器链。
  - `DynamicRoleAuthorizationManager` 在 `ROLE` 策略下改为默认拒绝；除 `PermitUrlProvider` 明确放行的 URL 外，接口必须存在资源、权限与角色关联才允许访问。
  - 接口资源匹配使用实际请求的“方法 + 路径”，避免 Spring MVC 泛型路径模板导致初始化接口等资源无法稳定匹配。
  - BSP Bootstrap 下游凭证排除范围收窄到 `auth/bootstrap` 资源中的 API Key 生成和 HMAC
    注册两个根入口；注册诊断等普通授权入口仍可携带服务 API Key。
- `isass-core-dependencies` 统一管理微信服务所需的 Bouncy Castle JDK 18+ 组件版本，业务微服务不得再直写该依赖版本。
- 修复 nocode MyBatis-Plus 代码生成器同时设置空 `exclude` 导致 `includeTables` 失效的问题；包含表与排除表现在严格二选一。
- 修复生成器将 `ModuleInfo` 错误定位为上下文包的问题；默认从模块名首段推导微服务根包，并允许通过元数据覆盖。

### 4.0.0

#### feat

- **OpenAPI 增强 SPI**：`isass-web-springmvc` 新增 `OpenApiEnhancerSpi`，`ServiceDocsController` 在增强器存在时输出运行时增强文档，不存在时保持原始 smart-doc 输出。
- **架构升级**：支持 Maven 4 风格构建，引入 `root="true"` 属性和 `modelVersion 4.1.0`。
- **模块重构**：新增 `isass-framework-dependencies` (BOM) 和 `isass-framework-parent` (Parent POM)，提供更加灵活的项目继承与依赖管理方式。
- **包名重构**：全量迁移包名，从 `vip.isass.core` 和 `vip.isass.kernel` 统一变更为 `vip.isass.framework`，保持与框架命名的统一。
- **Jakarta EE 适配**：全面适配 Jakarta EE 10，切换相关注解包名。
- **Spring Boot 升级**：深度适配自定义 Spring Boot 4.0.5 版本，将所有自动配置从 `spring.factories` 迁移至新的 `AutoConfiguration.imports` 机制。
- **MQ 系统重构**：
    - 引入新的 `IMqConsumer` 接口和 `MqMessageContext`。
    - 实现 MQ 消费者生命周期的全自动托管，移除冗余的初始化模板代码。
    - 优化配置项，统一使用 `mq.` 前缀。
- **MQ 多源重构**：
    - 引入 `DynamicMqProperties`、`MqSourceProperties`、`IMqFactory`、`IMqProducer`、`IMqMessageHandler` 和 `MqManager`。
    - 新增 `MqPublisher`，支持默认 `primary` 源和显式 source 发送。
    - Spring Event 与 Kafka 0.11 接入 source-oriented factory 模型，业务代码无需依赖具体 MQ 产品。
    - MQ 源必须显式配置 `type`，移除 `factory-class` 和 source 名称自动匹配。
    - 新增 `isass-mq-redisstream` 与 `isass-mq-redispubsub` 模块，直接基于 Redisson 实现 Redis Stream 和 Redis Pub/Sub MQ 源。
- **健康检查优化**：适配 Spring Boot 4.x 的模块化健康检查体系，更新 `HealthIndicator` 相关实现。
- **数据库初始化优化**：回归 `v4-old` 的 `DatabaseInitializerManager` 过程式逻辑，移除 `DatabaseInitializer` 接口及 SPI 扩展机制，统一管理各数据库方言。
- **数据库迁移工具替换**：将 Flyway 替换为 Liquibase。
    - `isass-database-core` 移除 Flyway 依赖，保留 Liquibase 服务级命名规则。
    - Spring Boot Liquibase 桥接由 `isass-adapter-springboot` 提供，业务服务按需依赖 `spring-boot-starter-liquibase`。
    - 新增 `LiquibaseServiceNaming` 服务级命名规则，Spring Boot adapter 提供 `AbstractLiquibaseConfiguration` 抽象配置基类与 `LiquibaseConfigurer` 配置工具类。
    - 服务侧通过声明独立 `SpringLiquibase` bean 实现微服务/单体两种启动模式。
    - 单体模式下多个服务 bean 在 Spring 初始化阶段分别执行，changelog 与 Liquibase 管理表通过服务名隔离。
- **达梦 Liquibase 兼容**：新增 `isass-database-dameng` 模块，引入 `com.github.mengweijin:db-migration-dameng-liquibase`，使用开源扩展支持达梦数据库，并将 Liquibase 版本锁定为 `5.0.3`。
- **模块合并**：
    - `isass-framework-nocode-*` 核心功能合并至 `isass-framework-common` 的 `core.structure` 目录下。
    - `isass-framework-database-mybatisplus-mysql` 和 `postgresql` 合并至 `isass-framework-database-mybatisplus`，支持多数据库自动配置。
    - 移除了冗余的 `isass-framework-database-mysql` 和 `isass-framework-database-postgresql` 包装模块，改为直接引用 JDBC 驱动。
    - `isass-framework-mq-redisstream` 和 `redispubsub` 合并至 `isass-framework-database-redis`。
    - `isass-framework-build` 模块保留并持续优化（曾短暂更名为 `isass-framework-deploy`）。

#### optimize

- 优化 `IV2LocalService` 接口，增加 `getService()` 必需方法以提升类型安全性。
- 统一 Maven 编译配置，由父 POM 统一管控 `maven.compiler.release` (Java 25)。
- 清理根项目 POM，使其专注于模块聚合管理。

#### migrate

- **Jackson 3.x 迁移**：将自定义代码从 Jackson 2.x (`com.fasterxml.jackson.*`) 迁移至 Jackson 3.x (`tools.jackson.*`)：
    - `JsonUtil`：重写 `ObjectMapper` 构造为 `JsonMapper.builder()` 构建器模式；`Feature` 枚举替换为 `JsonReadFeature`/`StreamWriteFeature`；`JsonProcessingException` → `JacksonException`；`jsonNode.elements()` → `jsonNode.iterator()`。
    - 序列化器/反序列化器：`JsonSerializer` → `ValueSerializer`，`JsonDeserializer` → `ValueDeserializer`，`SerializerProvider` → `SerializationContext`，移除 `throws IOException`。
    - 转换器：`StdConverter` 导入路径迁移至 `tools.jackson.databind.util.StdConverter`。
    - MyBatis-Plus 集成：`JacksonTypeHandler` → `Jackson3TypeHandler` 适配 Jackson 3 ObjectMapper；`CreatorProperty` 构造器迁移至 `CreatorProperty.construct()` 工厂方法；`ValueInstantiators.findValueInstantiator` 适配新签名（`BeanDescription.Supplier` + `modifyValueInstantiator`）。
    - 保留 `com.fasterxml.jackson.core:jackson-databind:2.21.2` 编译依赖，用于 Spring Data Redis 等第三方库的向后兼容；在 `JsonUtil` 中新增 `LEGACY_MAPPER`（Jackson 2.x ObjectMapper）供旧 API 调用。

#### test

- `ExceptionAdvice` 集成测试扩展至 19 个用例，覆盖 UnifiedException（有/无 status、有/无 cause）、core 映射（IllegalArgumentException/AbsentException/AlreadyPresentException/UnsupportedOperationException/IOException/FileNotFoundException/DateTimeException）、未映射异常回退 UNDEFINED、showDetailError 开关控制。验证：`mvn -pl isass-web-springmvc -am test -Dtest=ExceptionAdviceTest -Dmaven.javadoc.skip=true`。
- `StatusMessageEnum` 评估结论：JWT_TOKEN_ERROR/UN_LOGIN/TOKEN_EXPIRED/TOKEN_ILLEGAL 因 core-common（JwtUtil）和 web-springmvc（IsassErrorController）跨模块引用保留，符合 `docs/design/exception-code-architecture.md` 设计约束，无需拆分。

#### docs

- **NoCode 生成服务命名**：本地 CRUD 实现统一由 `${Entity}ApplicationService` 更名为 `${Entity}Service`，将 `ApplicationService` 保留给手写业务用例编排服务。
- **自动服务入口与 NoCode 边界设计**：由 `ICrudService` 继承关系唯一识别 NoCode 标准入口，并要求
  自定义业务操作使用独立 `IApplicationService`；确定将
  `isass-nocode-generator` 改为普通 `jar`，只保留领域模型、Criteria、Repository、Mapper、CRUD Service、
  关联及级联元数据等源码生成能力，删除合同与 Smart-doc 配置生成 Goal 及其实现；HTTP/gRPC 客户端采用
  全局传输顺序和按服务覆盖，不提供操作级覆盖且不在请求发出后跨协议重试；当前阶段不提供静态 `.proto`、
  离线 OpenAPI 或诊断 JSON 导出工具，运行时 `/v3/api-docs` 和 `ServiceDefinitionRegistry` 分别作为文档与
  入口元数据来源；确定 `findRoleCodesByUri` 只允许携带 `ROLE_INTERNAL_AUTH_QUERY` 的已认证应用主体通过
  固定本地规则调用，避免动态 URL 授权递归；同时重排主设计与 CRUD 专项文档，合并重复结论并统一
  `cursorPage` 命名；逐项评审现有 `IService` 的 36 个 CRUD 方法，确定升级七个现有方法并新增
  `cursorPage`，最终形成八个正式入口，其余改为 Java 默认实现或删除；批量新增、批量修改和批量删除保留
  为带 `EntrypointOperation` 的默认方法，单体新增、不存在时新增和单体删除改为未标注入口注解的 Java
  默认方法；`superCud` 的请求实体 `SuperCudReq` 使用
  `addEntities/addByFields/updateEntities/updateCriteria/deleteIds/deleteCriteria`，支持一个请求任意组合
  全部标准写操作及空变更集幂等保存，返回新增、修改、删除三类汇总数量；`createIfAbsent` 使用批次统一的
  实体匹配字段，Java 调用支持 getter Lambda，唯一索引和范围重叠策略由业务负责；新增独立
  `CrudWriteExecutor` 作为事务、授权、校验、生命周期、关联、审计和事件的统一执行边界，避免 Spring
  Bean 自调用绕过切面；同时补充 `newCriteria`、`NullValueMode` 以及非 CRUD 元数据方法、
  `IServiceManager` 的迁移结论。
- **级联、关联与树形 CRUD 设计**：将复杂场景库收敛为方向性删除级联、同事务关联编辑、方向性关联查询和
  `parent_id` 树形 CRUD 四项能力；关联声明统一为以 DDL 当前实体指向目标实体的方向性关系，一个声明只
  生成当前实体属性，不配置反向属性名称，反向关系由目标实体 DDL 另行声明；DDL 声明删除级联，不设置
  聚合写入开关或 DDL 更新模式；新增 `IUpdateCriteria` 并由 `FullTypeCriteria` 实现，以 Query 参数动态选择
  `MERGE/REPLACE`，并以 `IGNORE_NULL/WRITE_NULL` 控制普通字段空值写入；只发布批量更新入口，单实体
  更新默认包装为单元素集合；`batchSave` 升级为在同一个事务中执行普通新增、条件新增、按 ID 或 Criteria
  修改、按 ID 或 Criteria 删除的超级增删改接口 `superCud`；全部专项写方法只构造相应 `SuperCudReq`，统一
  委托给独立 `CrudWriteExecutor.superCud`，不依赖 Service 自调用切面；字段出现性由 HTTP、gRPC 和本地 Java
  调用统一规范化为瞬态写入属性掩码，执行器不再依赖原始 JSON；`updateCriteria` 为空时按实体 ID 定位，
  非空时使用批次公共范围并按 `matchFields` 追加当前实体等值条件；关联对象有 ID 时更新、无 ID 时新增；`IParentIdEntity`
  固定提供 `parentId/parent/children`，无需 DDL 标记生成父、子属性；表级
  `[树结构-cascadeDelete=true]` 专门控制删除当前节点时是否向下递归删除全部子孙节点；普通关联的
  级联参数继续使用 `cascadeDelete`，删除只沿当前实体 DDL 明确开启的方向级联；
  `getOne/list/requireOne` 改为复用 `page` 的默认方法，新增按 ID、支持 `orderBy=id asc/id desc` 且无总数
  查询的 `cursorPage` 深分页正式入口，将游标字段统一为 `cursorId/nextCursorId`，并明确高频附加过滤条件
  原则上使用以 ID 结尾的联合索引。
- **微服务 DDD 规范**：新增 `docs/usage/architecture/service-ddd.md`，统一 V4 微服务 api/service/boot 三模块职责、限界上下文分层、Liquibase 配置位置与 BSP 命名约定；同步更新 NoCode MyBatis-Plus 生成器文档的模块与上下文路径示例。
- **零代码文档校正**：`smart-doc` 使用说明改为无版本 nocode 合同、`IXxxService` 与统一动态 HTTP 路由；移除将 OpenAPI 地址 `/v3/api-docs` 误解为 nocode 接口版本的表述。
- **前端零代码说明**：新增 `docs/usage/nocode/frontend-api-usage.md`，说明标准 CRUD 路径、参数绑定、文件流、自定义业务接口与高级响应投影。
- 在 README 中补充模块命名规范，明确 `isass-分类-模块名` 格式。

#### refactor

- **NoCode 超级增删改请求精简**：`SuperCudReq` 收敛为 `addEntities/addByFields/updateEntities/updateCriteria/deleteIds/deleteCriteria`，删除逐项 `AddIfAbsentItem` 和 `UpdateByCriteriaItem`；同一新增或修改批次统一使用一套匹配规则，`updateCriteria.matchFields` 可把当前实体属性追加为等值条件，并保留 `createTime < xxx` 等公共范围条件；新增 Builder 及 getter Lambda 属性解析，本地调用保持类型安全，HTTP/gRPC 仍传 Java 属性名字符串；框架仅做属性和有效 WHERE 等必要校验，不强制唯一索引或更新范围互斥；`SuperCudResult` 改为仅返回新增、修改、删除汇总影响数量，避免批量写入回传实体副本。
- **NoCode 存在性方法命名统一**：正式入口 `exists` 改为返回基本类型 `boolean`，Java ID 便捷方法由 `isPresentById` 重命名为 `existsById`，删除只封装逻辑取反的 `absent`；保留用于“不存在”前置断言的 `requireAbsent`。

- **移除旧 EDB 双实体模型**：删除未被使用的 `IDbEntity`、`DbEntityConvert` 及 `WrapperUtil` 的 EDB QueryWrapper 方法；当前 nocode 实体直接作为 MyBatis-Plus 实体持久化。
- **代码生成器**：Controller 模板生成的本地 Service 字段改为实体小驼峰名称，例如 `iconGroupService`，不再添加 `nocode` 前缀。
- **动态 HTTP 路由**：nocode Adapter 的服务段限制为 `*-service`，避免通配路径拦截 Knife4j `/webjars/**` 等静态资源。
- **单一零代码体系**：移除无版本历史 nocode 与 V2 实现，将原 V3 合同、实体、Criteria、ORM、HTTP/gRPC transport、生成器统一为无版本 `isass-nocode-*` API；标准 HTTP 路由收敛为 `/{serviceName}/{entityName}`，自定义业务方法保留所属服务的确定路径。
- **合同与文档**：构建期资源统一为 `META-INF/isass/nocode-contract.json` 与 `*-nocode.proto`；OpenAPI 投影统一使用“零代码接口”分组，不再保留 V3 路径、类型或模板命名。

- **V3 文件流传输重构**：`V3FileStream` 改为以 `writeTo(OutputStream)` 为主的单次消费数据源；HTTP 适配器使用 `StreamingResponseBody` 直接消费存储源流，不再通过虚拟线程与 Pipe 中转。业务代码可按需通过 `openInputStream()` 获得读取式兼容流。
  - V3 文件端点在响应提交前直接返回空响应体的 HTTP 状态：文件不存在为 `404`，参数错误为 `400`，服务器异常为 `5xx`，不再包装为 `Resp` JSON。
  - 新增 `V3FileNotFoundException` 作为传输无关的文件资源不存在语义；传输已开始后的异常保留服务端日志并中断响应，避免写入无效 JSON。
  - `isass-nocode-grpc` 新增 V3 文件 server-streaming：首帧传输文件元数据，后续以 64 KiB 原始字节块发送；客户端恢复为 `V3FileStream`，避免文件内容聚合为 `byte[]` 或 JSON/Base64。
- **动态 V3 transport 与文档链路**：`IV3XxxService` 是唯一业务契约；构建期生成 `v3-contract.json` 和 proto，运行时由单个 HTTP/gRPC 适配器暴露全部接口，不再生成实体 V3 Controller。
  - OpenAPI 增强器根据契约生成命名 Schema、Javadoc 字段描述、统一 path、请求/响应 `oneOf` 和 Criteria 映射。
  - 业务调用优先级为本地实现、gRPC、HTTP；非幂等请求发出后禁止跨协议重试。
  - 增强结果以双检锁懒加载缓存；`ServiceDocsController` 通过 `OpenApiEnhancerSpi` 可选接入，未安装文档服务时保持原始输出。
  - 新增 `/v3/api-docs/swagger-config` 双模式分组，以及 `/doc.html`、`/services/{serviceName}/doc.html` Knife4j UI 路由。

#### fix

- **超级开发者动态 URL 授权**：`DynamicRoleAuthorizationManager` 在查询 URL 资源角色前优先识别
  `ROLE_SUPER_DEV`，避免尚未绑定业务权限或未登记资源角色的接口错误拒绝超级开发者；普通已认证用户仍需匹配
  URL 对应角色。
- **API Key 跨微服务认证**：`ApiKeyAuthenticationResult` 使用可传输的具体主体类型，支持 BSP 认证契约通过 NoCode HTTP 代理返回认证主体及角色，无需业务微服务自行实现认证适配器；NoCode HTTP 对单个字符串请求体统一按 JSON 字符串发送，避免服务端因 `text/plain` 请求体无法解析 API Key。
- **nocode 合同响应 Schema**：构建期合同生成器除实体与请求参数外，现同时收集自定义业务方法的返回类型及其泛型内部类型，确保 OpenAPI/Knife4j 可展示登录、业务 VO 等响应字段；补充 `ContractGeneratorTest` 回归覆盖。

- **V3 逻辑删除元数据修正**：`V3TableMetaRegistrar` 运行时识别逻辑删除字段后，补齐 MyBatis-Plus `TableFieldInfo` 的逻辑删除标记及未删除/已删除值 `0/1`，避免查询错误生成 `delete_flag = null`。
- **V3 表元数据注册时机修正**：`V3TableMetaRegistrar` 的 `populate()` 仅在 `v3ServiceRegistry` Bean 构造时联动触发，与 MyBatis-Plus `SqlSessionFactory` 构建 `TableInfo` 的顺序无任何约束，导致 MP 实际查询时回调 `postTableInfo` 的 `metaMap` 仍为空，最终实体类名直接转表名（如 `V3Icon` → `v3_icon`），抛出 `Table 'attachment.v3_icon' doesn't exist`。
  - `V3TableMetaRegistrar` 改为实现 `BeanDefinitionRegistryPostProcessor`，在 BDRPP 阶段（任何业务 Bean 实例化之前）通过 `ClassPathScanningCandidateComponentProvider` + `AssignableTypeFilter(IV3Entity.class)` 扫描 `vip.isass` 包下所有 `IV3Entity` 实现类，按接口契约（`IV3IdEntity`/`IV3TraceEntity`/`IV3LogicDeleteEntity`/`IV3VersionEntity`/`IV3TenantEntity`/`IV3ParentIdEntity`）镜像出表元数据，保证 MP `TableInfo` 构建时元数据已就绪。
  - 表名解析优先级调整为：1) 实体类上的 `@TableName`；2) 实体覆盖的 `IV3Entity#tableName()`；3) `V3TablePrefixUtil` 注册前缀 + `StrUtil.toUnderlineCase(entityName)`（兜底，并修复多词实体名 `iconGroup` 之前未转 `icon_group` 的隐患）。
  - `entity.java.ftl` 模板新增 `@Override tableName()` 返回 `${table.name}` 字面量，api 模块实体无需任何 MyBatis-Plus 注解即可被 MP 正确识别。
  - `V3AutoConfiguration` 移除 `V3TableMetaRegistrar.populate(registry)` 静态联动调用，避免与 `SqlSessionFactory` 创建时序耦合。
  - 同步手工补全 `isass-service-attachment` 的现有 `V3Icon` / `V3IconGroup` 实体的 `tableName()` 覆盖（重新跑生成器时由新模板自动生成）。
