# Nocode v3 Service Routing And Cache Design

## 背景

v1/v2 的 service 调用链通过 `ApiOrder` / `getOrder()` 排序选择实现。这个机制可以表达单体环境优先本地 service、微服务环境使用 feign service，但不适合作为缓存、审计、事件、幂等等方法级增强机制。

典型问题是 Redis service：如果把缓存做成一个完整 service 实现，只想缓存一个方法时，也必须实现整个 service 接口。其他方法即使返回 `null`，仍会进入调用链，增加空实现和运行时开销。

## 结论

`IsassOrdered` 已删除。v2 暂时保留 `IV2Service#getOrder()` 默认方法兼容历史链路，v3 service 不继承排序接口，也不再把缓存能力建模为一个排序 service。

v3 拆成两层：

1. **调用路由层**
   - 只解决本地实现、远程实现、自动选择的问题。
   - 可提供 `local`、`remote`、`auto` 等策略。
   - 单体启动时优先本地 provider；微服务部署时可以按配置或服务发现选择 remote provider。

2. **方法增强层**
   - 缓存、事件、审计、权限、幂等、限流等都作为 operation interceptor。
   - 拦截器以“某个实体 + 某个操作 + 某组参数”为粒度生效，不要求实现完整 service 接口。
   - 缓存语义参考 Spring Cache，但抽象定义在 isass nocode/core 中，Spring Boot adapter 再桥接 Spring `CacheManager`。

3. **接入层**
   - controller、socketio、kafka、定时任务等接入方式只负责协议解析和响应转换。
   - 接入层统一构造 `NocodeAccessRequest`，交给 `NocodeAccessHandler` 转为 `NocodeOperation` 并执行。
   - Spring MVC 等具体动态端点生成应放在对应 adapter/access 模块，`isass-nocode-core` 只保留纯 Java 请求模型和 handler。

## 缓存建议

v3 可以定义如下核心概念：

- `NocodeOperation`：描述实体、操作名、参数、返回类型，已落地在 `isass-nocode-core`。
- `NocodeCrudOperation`：标准 CRUD 操作名枚举，已落地；后续动态 controller、socketio/kafka access adapter、ORM provider 和代码生成器应复用同一组操作名。
- `NocodeOperationInvoker`：执行实际本地或远程 provider，已落地。
- `NocodeOperationInterceptor`：方法增强接口，按顺序包裹 invoker，已落地。
- `NocodeOperationPipeline`：把 interceptor 组合成调用链，已落地。
- `NocodeOperationListener` / `NocodeOperationListenerInterceptor`：service 逻辑前置、后置和异常监听底座，已落地；业务扩展可以只监听关心的阶段，不需要实现完整 service。
- `NocodeOperationProvider` / `NocodeOperationRouter`：负责 local、remote、auto 路由，已落地。
- `NocodeOperationExecutor`：统一组合 route + pipeline 的 v3 调用入口，已落地；后续 access/controller/socketio/kafka 等接入层应调用 executor，而不是直接关心 provider 选择和 interceptor 编排。
- `NocodeAccessRequest` / `NocodeAccessHandler`：框架无关的接入请求模型和处理入口，已落地；为 controller/socketio/kafka 等动态接入层提供统一底座。
- `NocodeCrudAccessRequests`：标准 CRUD access request 工厂，已落地；统一 `id`、`body`、`criteria` 等参数名，避免不同接入层和 provider 使用不同字符串。
- `NocodeCrudAccessDefinition`：标准 CRUD access 参数契约，已落地；定义每个标准操作的必需参数和可选参数，为后续动态 controller、socketio/kafka adapter 和 v3 代码生成器共享同一份操作描述。
- `NocodeAccessRequestValidator`：标准 CRUD access 请求校验器，已落地；`NocodeAccessHandler` 会在执行标准 CRUD operation 前校验必需参数和未知参数，自定义 operation 不受该校验约束。
- `NocodeEntityDefinition` / `NocodeFieldDefinition`：框架无关的实体和字段元数据，已落地；用于后续动态 access、代码生成器和 ORM adapter 共享实体描述。
- `NocodeEntityRegistry`：框架无关的实体元数据注册表，已落地；Spring/Micronaut/Solon adapter 可以各自负责发现实体定义，再注册到同一个 v3 registry。
- `NocodeEntityRelation` / `NocodeDeleteOptions`：实体关系元数据和删除请求选项，已落地；access adapter 可通过请求参数表达是否需要级联删除或关联表删除，ORM adapter 后续按统一契约执行。
- `NocodeEntity`：自定义实体的 v3 标记接口，已落地；业务实体可直接提供 entity name、display name、table name 和字段元数据，并生成 `NocodeEntityDefinition`。
- `NocodeEntity#formatTimestamp` / `NocodeEntity#setupTimestamp`：Long 毫秒时间戳的调试辅助方法，已落地；支持 `Entity::getCreateTime` 和 `Entity::setCreateTime` 方法引用，便于查看和设置 bigint 时间字段。
- `NocodeFieldDefinition#clientWritable` / `NocodeFieldAutoFill`：字段写入控制和自动填充元数据，已落地；后续新增/更新 operation 可据此拒绝前端覆盖只读字段，并在创建时间、修改时间等字段上自动写入当前时间。
- `NocodeCrudWritePayloadProcessor` / `NocodeCrudWriteInterceptor`：标准 save/update 写入前处理器，已落地；可在 operation pipeline 中基于实体字段元数据过滤前端只读字段，并填充创建时间、修改时间等服务端字段。
- `NocodeEntityDefinitionProvider`：Java SPI 实体元数据 provider，已落地；非 Spring 环境可以通过 `META-INF/services` 提供实体定义，`NocodeEntityRegistry.fromServiceLoader()` 会自动加载并注册。
- `NocodeQueryCriteria` / `NocodeQueryCondition` / `NocodeQueryGroup`：Map/List 化查询条件模型，已落地；不再要求为每个字段生成 `orXxx`、`xxxNotEqual` 等大量属性，复杂条件通过 group + joiner 表达。
- `NocodePageRequest` / `NocodePageResult`：框架无关的分页请求和分页结果模型，已落地；ORM adapter 可把 MyBatis Plus、sqltoy 等分页对象转换为统一 v3 模型。
- `NocodeQueryValidator`：基于实体元数据的查询校验器，已落地；可在 access/ORM adapter 执行前校验 select、where/group、sort 字段是否存在，以及字段是否允许查询或排序。
- `NocodeBlankStringPolicy`：空字符串查询策略，已落地；接入层可明确选择忽略空字符串，或把空字符串作为真实查询值传给 ORM adapter。
- `NocodeFieldConstraint` / `NocodeEntityValidator` / `NocodeCrudValidationInterceptor`：JSR303 风格字段校验元数据、分组校验器和 CRUD 写入校验拦截器，已落地；支持 create/update 分组、notNull、notBlank、size 等基础约束，后续可桥接 Jakarta Validator 和 Web MVC 错误响应。
- `NocodeCacheOperation`：缓存元数据，例如 cache name、key、cacheable/put/evict 行为，已落地。
- `NocodeCacheManager`：isass 自有缓存门面，已落地。
- `NocodeCacheKeyGenerator`：缓存 key 生成器，已落地。
- `NocodeCacheOperationResolver`：从 operation 解析缓存元数据，已落地。
- `NocodeCacheInterceptor`：把 cacheable/put/evict 语义接入 operation pipeline，已落地；未匹配缓存配置或缓存不存在时直接透传到下游 invoker。

已落地的 v3 基础抽象均为纯 Java，不依赖 Spring，也不要求 v3 service 继承排序接口。

Spring Boot 场景下：

- `isass-adapter-springboot` 或未来 `isass-nocode-springboot` 把 `NocodeCacheManager` 桥接到 Spring `CacheManager`。
- 如果业务已经使用 Spring Cache，可以复用底层 cache provider。
- v3 的接口元数据不直接依赖 Spring 注解，保证未来 Micronaut/Solon adapter 可以复用。
- `isass-web-springmvc` 已提供 `NocodeSpringMvcCrudRoute` 作为 v3 动态 controller 的默认 HTTP method/path descriptor，并通过 `NocodeSpringMvcCrudRequestFactory` 把 Spring MVC 解析出的 path/query/body 参数转换为框架无关的 `NocodeAccessRequest`；后续运行时注册端点时应复用这两个边界，避免路由、文档和代码生成各自维护路径规则。

## 与旧机制关系

- v1/v2：继续保留 `ApiOrder` / `getOrder()` 历史语义，避免破坏现有 service manager。
- v3：不继承排序接口，不使用完整 service 作为缓存层。
- 后续当 v3 替代 v1/v2 后，再评估删除 `ApiService`、`V2ServiceManagerUtil` 等 legacy 调用链。

## 迁移进展

- `isass-nocode-core` 已补齐 v2 自有包下的 `BatchSave`、`UnimplementedMethodException`、`IV2DbEntity`、`V2DbEntityConvert`。
- `isass-nocode-core/src/main/java` 已不再反向引用 `vip.isass.framework.common.structure`，后续可以继续推动业务和数据库实现从 `common.structure` 迁移到 `nocode.v2` 或 v3 模型。
- `isass-core-dependencies` 已纳入 `isass-nocode-core` 版本管理，业务模块可以直接声明依赖而不写版本。
- `isass-web-springmvc`、`isass-database-core`、`isass-database-mybatisplus`、`isass-adapter-springboot` 已改为依赖并使用 `isass-nocode-core` 的 v2 类型。
- `isass-service-attachment` 已作为首个适配项目，把 v2 低代码 entity、criteria、service 导入迁移到 `vip.isass.framework.nocode.v2`。
- `StringToV2WhereConditionConverter` 已迁到 nocode v2 converter 包，避免 core-common 继续引用 v2 查询条件。
- `V2MybatisPlusGenerator` 的模板变量和 nocode MyBatis XML namespace 已迁到 `vip.isass.framework.nocode` 包名。
- 旧 Swagger/Knife4j 模块和框架源码中的 `io.swagger.annotations` 已移除，v2 接口文档描述后续以 smart-doc Javadoc 为准。
- `isass-nocode-core` 中旧 lowcode MyBatis Plus 注释源码和未引用 mapper 已删除，具体 ORM 实现继续放在 `isass-database-mybatisplus`。
- `vip.isass.framework.common.structure` 暂时保留，兼容尚未迁移的业务微服务和工具代码；后续可继续缩小它在 `isass-core-common` 中的存在范围。
- `NocodeCacheInterceptor` 已补齐 cacheable/put/evict 的执行语义，缓存增强不再需要通过实现完整 service 并参与排序链完成。
- `NocodeOperationListenerInterceptor` 已补齐 before/after/error 监听语义，service 事件监听不再需要通过替换 service 实现完成。
- `NocodeCrudValidationInterceptor` 已补齐 save/update 的分组校验语义，为后续动态 controller 和 ORM provider 在执行前统一校验请求体提供底座。
- `NocodeOperationExecutor` 已补齐 v3 标准调用入口，为后续动态 access 层生成提供稳定的纯 Java 调用门面。
- `NocodeAccessRequest`、`NocodeAccessHandler` 和 `NocodeAccessRequestValidator` 已补齐 v3 access 接入层的纯 Java底座，后续 Spring MVC 动态 controller 只需做协议映射并复用标准参数校验。
- `NocodeCrudOperation`、`NocodeCrudAccessRequests` 和 `NocodeCrudAccessDefinition` 已补齐 v3 标准 CRUD 操作名、access request 工厂与参数契约，后续动态 controller、代码生成器和 ORM provider 可复用统一契约。
- `NocodeEntityRelation`、`NocodeEntityRelationType` 和 `NocodeDeleteOptions` 已补齐级联删除/关联表删除的元数据和请求参数契约，后续 ORM adapter 可据此实现实际删除。
- `NocodeFetchOptions` 已补齐关联查询请求选项，`findById`、`page`、`list` 可以通过 access 参数表达是否加载关联数据以及指定关联名；后续 ORM adapter 可据此实现一对一、一对多等关联查询。
- `NocodeEntity`、`NocodeEntityDefinition`、`NocodeFieldDefinition`、`NocodeFieldAutoFill`、`NocodeCrudWriteInterceptor`、`NocodeEntityDefinitionProvider`、`NocodeEntityRegistry`、`NocodeQueryCriteria`、`NocodePageResult`、`NocodeQueryValidator` 等 v3 元数据、查询模型和写入处理器已补齐，为自定义实体继承 v3 接口、criteria 简化、分页对象统一、字段写入控制、自动填充、bigint 时间戳调试、ORM 无关实体探索和非 Spring SPI 自动发现提供第一阶段底座。

## Roadmap 对应

该设计对应 `docs/70.roadmap/2024.md` 中低代码 v3 通用代码设计实现相关任务，尤其是：

- 低代码子模块 DDD 重设计
- 新增 access 接入层
- v3 通用 controller 动态生成
- v3 代码生成器
- service 逻辑前置/后置监听
