# Nocode 统一服务代理设计

## 目标

让业务代码始终注入 V4 API 中声明的 `IService` 子接口；当同一接口有本地实现时直接使用本地实现，当没有本地实现时自动通过 gRPC 或 HTTP 调用远程服务。传输优先级固定为：**本地实现 > gRPC > HTTP**。

## 范围与边界

- 只处理由 `isass-nocode-generator` 生成 `META-INF/isass/nocode-contract.json` 的 V4 服务接口。
- 判断本地实现必须使用合同的 `serviceInterface` 全限定名，不能按简单类名、`entityName` 或历史包名匹配。
- 例如 BSP 的 `vip.isass.bsp.auth.application.service.IUserService` 已由
  `vip.isass.bsp.auth.application.service.UserService` 实现；该应用中绝不注册它的远程代理。
- 迁移前的 `vip.isass.auth.api.service.user.IUserService` 与 V4 接口不是同一 Java 类型，不参与本机制。
- 不改变 nocode 的 HTTP 服务端路由和 gRPC 服务端协议；本设计只补齐客户端和 Spring Bean 注册。

## 契约发现与 Bean 注册

启动时用已有 `ContractResourceLoader` 枚举 classpath 上所有
`META-INF/isass/nocode-contract.json`。对每个 `ServiceContract`：

1. 用 `Class.forName(contract.serviceInterface())` 加载接口，加载失败立即报出合同资源、接口名和依赖缺失信息；
2. 验证该类型是接口且继承 `IService`；否则报出无效合同；
3. 若 `ApplicationContext` 中已有该接口类型的本地 Bean，则不注册代理；
4. 否则注册唯一的 JDK 动态代理 Bean，Bean 类型就是该 V4 接口。

`ServiceProxyFactory` 负责 Java 方法到 `OperationContract` 的映射；当前合同禁止方法重载，因此按方法名唯一匹配，缺失时明确报错。代理只使用同一 `ServiceContract` 的 transport 集合，避免不同服务之间错误路由。

## 传输实现

### 本地

本地实现不经过 `InvocationTransport`：Spring 注入真实 `ILocalService` 实现。这样事务、切面、指标和业务语义都保留在本地服务 Bean 上，也不会为本地接口制造重复 Bean。

### gRPC

每个远程 `serviceName` 可在 `isass.framework.nocode.grpc.endpoints` 配置一个 target。存在 target 时使用现有动态 gRPC descriptor 和 `GrpcClientTransport`；没有 target 时 gRPC transport 不可用。文件下载继续使用既有 gRPC server-streaming `FileStream` 协议；含 `InputStream` 的上传仍不走 gRPC，自动退到 HTTP。

### HTTP

HTTP 客户端使用 Spring Framework 7 的 `@HttpExchange` 代理，而不是业务代码直接调用 `RestClient`。框架定义一个内部、固定的 exchange 接口：以 `@HttpExchange` 声明 JSON 接收能力，方法参数使用动态 `HttpMethod`、完整 `URI`、查询参数和可空 JSON body，并返回 `JsonNode`。`HttpExchange` 原生支持动态 `HttpMethod` 与 `URI` 参数；由 `HttpServiceProxyFactory` 基于 `RestClientAdapter` 创建该接口的实例。

`HttpClientTransport` 根据 `OperationContract` 组装 URI、路径变量、查询参数和请求体，再经该 exchange 接口调用。对于框架标准 `Resp` JSON，取 `data` 并根据合同 `returnJavaType` 反序列化；非 JSON 文件响应继续由既有 `FileStream` 专用路径处理。每个远程 `serviceName` 的基础地址由 `isass.framework.nocode.http.endpoints` 配置。

## 错误与可观测性

- 合同无法加载、接口类型错误、同一远程接口重复代理、没有可用 transport，均在启动或调用点报出服务名、实体名、接口名和操作名。
- gRPC、HTTP 调用失败沿用 `TransportInvocationException`；仅幂等操作允许在 gRPC 失败后降级 HTTP，非幂等且已发送请求的操作不得重试。
- 代理 Bean 的名称包含完整服务接口名的稳定派生值，便于排查 Spring Bean 列表；日志和异常携带逻辑操作名而非仅 `invoke`。

## 验收

1. 有本地 `IUserService` 实现时，容器只注入 `UserService`，不会创建同接口代理。
2. API JAR 合同存在、无本地实现且配置 gRPC endpoint 时，调用使用 gRPC。
3. 无 gRPC endpoint、配置 HTTP endpoint 时，调用使用 `@HttpExchange` HTTP 客户端。
4. gRPC 对幂等调用失败时降级 HTTP；非幂等请求已发送时不降级。
5. 无 endpoint、无本地实现、合同接口类缺失和重复代理均产生明确异常。
