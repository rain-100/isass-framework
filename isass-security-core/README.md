# isass-security-core

与 Web 和认证框架无关的安全契约。提供业务/内部主体、当前主体 SPI、主体授权上下文、JWT，以及数据读取和固定租户策略。

- 包名统一为 `vip.isass.framework.security`；只依赖 core-common 和 JWT 库，不依赖 Spring Security、Servlet、Redis、NoCode 或具体微服务。
- `CurrentPrincipalService` 的 getter 不执行 I/O；显式 `loadRoleContext/loadAuthorizationContext` 按需加载并复用请求结果。`PrincipalAuthorizationLoader.loadRoles/load` 支持分级装载，`ApiKeyAuthenticator` 负责凭证校验。BSP 在其 API 模块适配本地服务或远程 Entrypoint。
- `PrincipalAuthorizationContext` 不新增状态字段：上下文 null 为未加载，permissionCodes null 为仅角色，权限集合（包括空集合）为完整授权；集合不可变，不携带角色显示名称。可配置条件统一称为数据过滤，由 DataReadPolicy 提供；它与固定租户策略独立执行，角色元数据读取不能清除固定租户边界。
- JSON 字段隐藏留在 core-common 的 serialization 包；查询补键投影归 NoCode；Spring Token、过滤器、HMAC 和角色投影归 Spring Security 适配模块。
- TrustedReadContext 按 Criteria 对象身份承载服务器读取约束，嵌套作用域只叠加、退出后恢复；不提供客户端可信标记，不授予输出字段或客户端筛选能力。

[返回模块目录](../README.md)
