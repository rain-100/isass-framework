# Nocode 服务客户端

## 调用原则

调用方只注入目标服务 API 模块公开的 V4 `IService` 子接口，例如：

```java
@Resource
private IUserService userService;
```

接口类型必须来自当前 V4 API，例如 `vip.isass.bsp.auth.application.service.IUserService`。
不要注入迁移前的 `vip.isass.auth.api.service.user.IUserService`：它不是同一个 Java 契约，也不会参与 nocode 的本地或远程路由。

框架启动时读取所有依赖 API JAR 中的 `META-INF/isass/nocode-contract.json`，并按下列顺序提供同一个接口：

1. 本地 Spring 实现；
2. gRPC 远程实现；
3. HTTP 远程实现。

如果本地存在实现，例如 BSP 中 `IUserService` 对应 `UserService`，框架不会再创建远程代理。没有本地实现时才创建远程代理。没有可用 endpoint 时，调用会明确报出逻辑服务、实体和操作名，不会静默返回空值。

## 远程 endpoint 配置

```yaml
isass:
  framework:
    nocode:
      grpc:
        endpoints:
          bsp-service: dns:///bsp-service:9090
      http:
        endpoints:
          bsp-service: http://bsp-service:8080
```

配置项的 key 是合同中的 `serviceName`，例如 `bsp-service`，不是 Maven artifactId、Java 包名或实体名。

当 gRPC endpoint 可用时优先使用 gRPC；未配置 gRPC endpoint、gRPC 在调用前不可用，或接口参数包含上传 `InputStream` 时，使用 HTTP。对于已发送请求：只有幂等操作允许从失败的 gRPC 调用降级 HTTP；非幂等操作不会自动重试，避免重复写入。

## HTTP 实现

HTTP 客户端由框架内部一个动态 Spring Boot 4 `@HttpExchange` 客户端实现。业务服务接口不需要添加 Spring HTTP 注解，也不会为每个接口或每个 HTTP 方法生成一个客户端类。合同中的 HTTP 方法、路径变量、查询参数和 body 在调用时动态映射。

文件下载仍返回 `FileStream`；gRPC 使用既有 server-streaming 协议，HTTP 直接消费响应流。文件上传的 `InputStream` 不走 gRPC，以避免把流内容预读到内存。
