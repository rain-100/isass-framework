# isass-security-springsecurity

Spring Security 集成。提供 JWT、API Key、动态入口授权及内部服务 HMAC 支持。

- 业务主体与内部主体独立读取；超级管理员由角色识别，不能用某个业务 edit/update 权限代替。
- 内部 HMAC 只证明调用服务身份，不能替代用户功能权限、数据范围或租户边界。

[返回模块目录](../README.md)
