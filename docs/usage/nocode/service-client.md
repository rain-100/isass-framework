# Entrypoint 服务调用

调用方只依赖目标服务的 API 模块并注入其 `IEntrypoint` 子接口。运行时优先使用当前进程中的本地实现；
没有本地实现时，registry 才创建远程代理。业务微服务不需要 Feign、HttpExchange 或专用适配器。

接口必须使用运行时注解声明地址：

```java
@EntrypointInfo(
        serviceName = "bsp-service",
        contextName = "config",
        resourceName = "dictionaryType",
        tag = "字典类型")
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

Body 中的 `Map` 遵循 JSON 对象键语义：传输树中的键统一为字符串。框架在字段出现性绑定与投影时，会按
Java Map 键的序列化字符串匹配原值，因此 `Map<Long, ?>`、`Map<Integer, ?>` 等标量键类型可以安全跨
HTTP/gRPC 传输，不会因传输树使用字符串键而丢失嵌套字段或触发有序 Map 的键类型异常。

HTTP Query 的数组和集合固定序列化成一个英文逗号分隔参数，例如 `idIn=1,2,3`。服务端根据 Java 参数或
对象属性的数组、集合类型反向拆分；单元素仍为 `idIn=1`。同名重复 Query 参数不属于 Entrypoint 合同并会被
拒绝。英文逗号是保留分隔符，集合中的字符串元素不能包含字面量英文逗号；需要承载任意字符串集合时应改用
Body，而不是 Query。
