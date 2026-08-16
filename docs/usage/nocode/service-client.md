# Entrypoint 服务调用

调用方只依赖目标服务的 API 模块并注入其 `IEntrypoint` 子接口。运行时优先使用当前进程中的本地实现；
没有本地实现时，registry 才创建远程代理。业务微服务不需要 Feign、HttpExchange 或专用适配器。

接口必须使用运行时注解声明地址：

```java
@EntrypointInfo(serviceName = "bsp-service", contextName = "config", resourceName = "dictionaryType")
public interface IDictionaryTypeService
        extends ICrudService<DictionaryType, DictionaryTypeCriteria, Long> {

    @EntrypointOperation(
            operationName = "listEnabledByBizType",
            displayName = "查询启用字典类型",
            httpMethod = HttpMethod.GET)
    List<DictionaryType> listEnabledByBizType(@QueryParam("bizType") String bizType);
}
```

框架直接解析接口和注解，不读取 `nocode-contract.json`，也不运行合同生成 Maven Goal。未标注
`@EntrypointOperation` 的默认方法只在调用方 JVM 内执行；标注后的默认方法必须进入本地实现或远程传输。

## HTTP 与 gRPC

HTTP 地址按以下顺序解析：

1. `isass.entrypoint.http.services.{serviceName}.url`；
2. `isass.entrypoint.http.base-url`；
3. Spring Cloud 服务发现。

示例：

```yaml
isass:
  entrypoint:
    client:
      transport-order: [HTTP, GRPC]
      services:
        bsp-service:
          transport-order: [GRPC, HTTP]
    http:
      base-url: http://127.0.0.1:31000
      services:
        bsp-service:
          url: http://127.0.0.1:31010
    grpc:
      services:
        bsp-service:
          host: 127.0.0.1
          port: 31110
          plaintext: true
```

只有连接前不可用的传输可以回退到下一种传输。请求一旦发出，超时或业务异常都不得跨协议重试，以免重复
执行非幂等写操作。认证头由 `AdditionalRequestHeaderProvider` 统一附加；服务账号 API Key 的内部目标识别
复用上述 `isass.entrypoint.http.services.*.url` 和 `isass.entrypoint.http.base-url`，无需维护第二套地址配置。

## 地址规则

- 自定义入口：`/{serviceName}/{contextName}/{resourceName}/{operationName}`；
- NoCode 标准入口：`/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}`。

业务参数只使用 Query、Body、Header 或 multipart，不使用 Path 参数。
