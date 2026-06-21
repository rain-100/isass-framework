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
- `NocodeOperationInvoker`：执行实际本地或远程 provider，已落地。
- `NocodeOperationInterceptor`：方法增强接口，按顺序包裹 invoker，已落地。
- `NocodeOperationPipeline`：把 interceptor 组合成调用链，已落地。
- `NocodeOperationProvider` / `NocodeOperationRouter`：负责 local、remote、auto 路由，已落地。
- `NocodeOperationExecutor`：统一组合 route + pipeline 的 v3 调用入口，已落地；后续 access/controller/socketio/kafka 等接入层应调用 executor，而不是直接关心 provider 选择和 interceptor 编排。
- `NocodeAccessRequest` / `NocodeAccessHandler`：框架无关的接入请求模型和处理入口，已落地；为 controller/socketio/kafka 等动态接入层提供统一底座。
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
- `NocodeOperationExecutor` 已补齐 v3 标准调用入口，为后续动态 access 层生成提供稳定的纯 Java 调用门面。
- `NocodeAccessRequest` 和 `NocodeAccessHandler` 已补齐 v3 access 接入层的纯 Java 底座，后续 Spring MVC 动态 controller 只需做协议映射。

## Roadmap 对应

该设计对应 `docs/70.roadmap/2024.md` 中低代码 v3 通用代码设计实现相关任务，尤其是：

- 低代码子模块 DDD 重设计
- 新增 access 接入层
- v3 通用 controller 动态生成
- v3 代码生成器
- service 逻辑前置/后置监听
