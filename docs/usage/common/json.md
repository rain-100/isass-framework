# 框架 JSON 使用约定

- 框架代码的 JSON 序列化、反序列化、树转换及对象转换统一调用 `JsonUtil` 的静态方法；调用方不创建或持有独立的 `ObjectMapper`。
- 默认读写使用 `JsonUtil` 的默认配置。需要省略 null 字段时，显式调用 `writeValueWithNotNullInstance`，避免依赖调用进程的 Spring Bean 配置。
- Spring MVC 的消息转换由 Spring 管理 mapper；`ObjectMapperConfiguration` 将 `JsonUtil.configure` 应用到 Spring 的 builder。MyBatis-Plus、Redis 等第三方 API 必须接收 mapper 对象时，传入 `JsonUtil.DEFAULT_INSTANCE`，不在适配层另建实例。
- `JsonUtil.LEGACY_MAPPER` 只用于尚依赖 Jackson 2 的第三方集成，也启用 `PROPAGATE_TRANSIENT_MARKER`，忽略带 getter 的 `transient` 字段。新代码使用 Jackson 3 的 `JsonUtil` 方法。
