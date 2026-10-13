# isass-adapter-springboot

Spring Boot 装配桥。把公共契约、NoCode 执行器、事件和配置桥接到 Spring Boot。

- 采用显式自动配置，不通过扫描隐式注册框架 Bean；业务规则与初始化数据留在业务服务。
- 当前主体通过 isass-security-core 的 SPI 桥接，允许无 Spring Security 的非 Web 应用使用公共能力。

[返回模块目录](../README.md)
