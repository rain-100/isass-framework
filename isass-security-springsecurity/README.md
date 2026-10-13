# isass-security-springsecurity

Spring Security 集成。提供 JWT、API Key、动态入口授权及内部服务 HMAC 支持。

- 业务主体与内部主体独立读取；超级管理员由角色识别，不能用某个业务 edit/update 权限代替。
- 通用模型和 JWT 来自 isass-security-core；过滤器/授权管理器依赖 PrincipalAuthorizationLoader、ApiKeyAuthenticator，不依赖 BSP 服务接口。应用须提供 SPI 实现，使用 bsp-api 时自动装配 BSP 适配器。
- JWT 默认只认证身份，公开及登录型入口不主动加载授权，认证成功日志默认只记录身份、访问地址和来源 IP，不打印角色。权限入口先加载角色，超级管理员直接通过，否则再加载功能权限；NoCode 和业务数据策略按需加载并复用请求上下文。PrincipalAuthenticationToken 的角色从上下文派生，getter 不执行 I/O。加载失败清理认证并终止，不降级放行。
- `isass.security.authentication.load-roles-for-logging` 默认 false；设为 true 时，JWT 认证成功即调用 loadRoles 并打印角色编码（日志级别 DEBUG），公开/登录型入口也会加载，后续授权复用结果。此开关不调用完整权限加载；API Key 直接复用认证时已有的角色。即使 DEBUG 关闭，true 仍加载角色；加载失败返回 503，生产环境建议保持 false。
- 内部 HMAC 只证明调用服务身份，不能替代用户功能权限、数据过滤或租户边界。
- API Key 拒绝返回 401；授权上下文缺失或认证服务故障返回 503，并清理请求认证、终止执行。

[返回模块目录](../README.md)
