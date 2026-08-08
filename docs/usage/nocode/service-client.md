# Nocode 服务客户端

## 调用原则

调用方只注入目标服务 API 模块公开的 V4 `IService` 子接口，例如：

```java
@Resource
private IUserService userService;
```

接口类型必须来自当前 V4 API，例如 `vip.isass.bsp.auth.application.service.IUserService`。
不要注入迁移前的 `vip.isass.auth.api.service.user.IUserService`：它不是同一个 Java 契约，也不会参与 nocode 的本地或远程路由。

框架启动时读取所有依赖 API JAR 中的 `META-INF/isass/nocode-contract.json`，当前正式运行时按下列顺序提供同一个接口：

1. 本地 Spring 实现；
2. HTTP 远程实现。

如果本地存在实现，例如 BSP 中 `IUserService` 对应 `UserService`，框架不会再创建远程代理。没有本地实现时才创建远程代理。没有可用 endpoint 时，调用会明确报出逻辑服务、实体和操作名，不会静默返回空值。

## 远程 endpoint 配置

```yaml
isass:
  http:
    endpoints:
      bsp-service:
        url: http://127.0.0.1:31010
```

配置项中的服务名是合同的 `service`，例如 `bsp-service`，不是 Maven artifactId、Java 包名或实体名。显式 URL 优先于服务发现；仅在未配置 URL 时，框架才通过 Spring Cloud LoadBalancer 选择 Nacos 等注册中心中的实例。

不使用服务发现时，直接配置 URL 即可；BSP 单体启动时不需要 Nacos。例如本地 IM 调用 BSP：

```bash
SPRING_APPLICATION_JSON='{"isass":{"http":{"endpoints":{"bsp-service":{"url":"http://127.0.0.1:31010"}}}}}'
```

使用 `SPRING_APPLICATION_JSON` 可以原样保留服务名中的连字符；不要为本地调用额外启用 Nacos。

## gRPC 遗留状态

动态 gRPC 描述符、客户端和 in-process 回归测试暂作为遗留预研代码保留，但当前框架**没有部署 gRPC Server
监听器**，因此任何服务不得配置 `isass.framework.nocode.grpc.endpoints`，也不得把 gRPC 作为生产调用或验收条件。
当前跨服务调用统一使用 HTTP；未来若重新启用 gRPC，必须先补齐唯一 Server 生命周期、端口配置、真实 Netty
跨端口测试和部署安全策略，再恢复优先级与配置示例。

## HTTP 实现

HTTP 客户端由框架内部一个动态 Spring Boot 4 `@HttpExchange` 客户端实现。业务服务接口不需要添加 Spring HTTP 注解，也不会为每个接口或每个 HTTP 方法生成一个客户端类。合同中的 HTTP 方法、路径变量、查询参数和 body 在调用时动态映射。

文件下载通过 HTTP 直接消费响应流；文件上传的 `InputStream` 也通过 HTTP multipart 传输，以避免把流内容预读到内存。

## Criteria 查询参数

标准 `IService` 的 GET 查询以实体的**等值字段**传递，例如 `new ThirdUserCriteria().setType(999)` 会传为
`?type=999`。`Criteria` 内部的 `whereConditions`、排序等结构化条件不是 URL 参数，框架不会把它们调用
`toString()` 后发送；否则服务端无法正确反序列化，且会泄漏不应作为公开 API 的内部模型。

需要复杂条件时，应定义带 `@http POST ...` 的业务方法和明确的请求 DTO，通过 JSON body 传输；不要依赖标准
GET Criteria 的内部条件列表。远端返回的 `Resp.success=false` 会被 HTTP 客户端转换为包含服务、实体、操作和远端
错误信息的异常，绝不会被静默当作 `null` 返回。
