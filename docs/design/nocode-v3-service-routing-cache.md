# Nocode v3 Service Routing And Cache Design

## 背景

v1/v2 的 service 调用链通过 `ApiOrder` / `IsassOrdered` 排序选择实现。这个机制可以表达单体环境优先本地 service、微服务环境使用 feign service，但不适合作为缓存、审计、事件、幂等等方法级增强机制。

典型问题是 Redis service：如果把缓存做成一个完整 service 实现，只想缓存一个方法时，也必须实现整个 service 接口。其他方法即使返回 `null`，仍会进入调用链，增加空实现和运行时开销。

## 结论

`IsassOrdered` 暂时保留给 v1/v2 兼容使用。v3 service 不继承 `IsassOrdered`，也不再把缓存能力建模为一个排序 service。

v3 拆成两层：

1. **调用路由层**
   - 只解决本地实现、远程实现、自动选择的问题。
   - 可提供 `local`、`remote`、`auto` 等策略。
   - 单体启动时优先本地 provider；微服务部署时可以按配置或服务发现选择 remote provider。

2. **方法增强层**
   - 缓存、事件、审计、权限、幂等、限流等都作为 operation interceptor。
   - 拦截器以“某个实体 + 某个操作 + 某组参数”为粒度生效，不要求实现完整 service 接口。
   - 缓存语义参考 Spring Cache，但抽象定义在 isass nocode/core 中，Spring Boot adapter 再桥接 Spring `CacheManager`。

## 缓存建议

v3 可以定义如下核心概念：

- `NocodeOperation`：描述实体、操作名、参数、返回类型，已落地在 `isass-nocode-core`。
- `NocodeOperationInvoker`：执行实际本地或远程 provider，已落地。
- `NocodeOperationInterceptor`：方法增强接口，按顺序包裹 invoker，已落地。
- `NocodeOperationPipeline`：把 interceptor 组合成调用链，已落地。
- `NocodeOperationProvider` / `NocodeOperationRouter`：负责 local、remote、auto 路由，已落地。
- `NocodeCacheOperation`：缓存元数据，例如 cache name、key、cacheable/put/evict 行为，已落地。
- `NocodeCacheManager`：isass 自有缓存门面，已落地。
- `NocodeCacheKeyGenerator`：缓存 key 生成器，已落地。

已落地的 v3 基础抽象均为纯 Java，不依赖 Spring，也不要求 v3 service 继承 `IsassOrdered`。

Spring Boot 场景下：

- `isass-adapter-springboot` 或未来 `isass-nocode-springboot` 把 `NocodeCacheManager` 桥接到 Spring `CacheManager`。
- 如果业务已经使用 Spring Cache，可以复用底层 cache provider。
- v3 的接口元数据不直接依赖 Spring 注解，保证未来 Micronaut/Solon adapter 可以复用。

## 与旧机制关系

- v1/v2：继续保留 `ApiOrder` / `IsassOrdered`，避免破坏现有 service manager。
- v3：不继承 `IsassOrdered`，不使用完整 service 作为缓存层。
- 后续当 v3 替代 v1/v2 后，再评估删除 `IsassOrdered`、`ApiService`、`V2ServiceManagerUtil` 等 legacy 调用链。

## 迁移进展

- `isass-nocode-core` 已补齐 v2 自有包下的 `BatchSave`、`UnimplementedMethodException`、`IV2DbEntity`、`V2DbEntityConvert`。
- `isass-nocode-core/src/main/java` 已不再反向引用 `vip.isass.framework.common.structure`，后续可以继续推动业务和数据库实现从 `common.structure` 迁移到 `nocode.v2` 或 v3 模型。
- `isass-core-dependencies` 已纳入 `isass-nocode-core` 版本管理，业务模块可以直接声明依赖而不写版本。
- `isass-web-springmvc`、`isass-database-core`、`isass-database-mybatisplus`、`isass-adapter-springboot` 已改为依赖并使用 `isass-nocode-core` 的 v2 类型。
- `isass-service-attachment` 已作为首个适配项目，把 v2 低代码 entity、criteria、service 导入迁移到 `vip.isass.framework.nocode.v2`。
- `vip.isass.framework.common.structure` 暂时保留，兼容尚未迁移的业务微服务和工具代码；后续可继续缩小它在 `isass-core-common` 中的存在范围。

## Roadmap 对应

该设计对应 `docs/70.roadmap/2024.md` 中低代码 v3 通用代码设计实现相关任务，尤其是：

- 低代码子模块 DDD 重设计
- 新增 access 接入层
- v3 通用 controller 动态生成
- v3 代码生成器
- service 逻辑前置/后置监听
