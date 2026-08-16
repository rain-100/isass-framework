# Smart-doc 与 Entrypoint OpenAPI

每个微服务在自己的资源目录维护 Smart-doc 官方插件配置，例如：

```text
src/main/resources/openapi/smart-doc.json
```

构建产物必须按服务名隔离：

```text
META-INF/isass/openapi/asset-service/openapi.json
META-INF/isass/openapi/bsp-service/openapi.json
```

运行时通过 `classpath*:META-INF/isass/openapi/**/openapi.json` 读取静态 Controller 文档，再与当前进程中
具有本地实现的 Entrypoint 元数据合并。`ServiceDocsController` 最终只提供一份 `/v3/api-docs`：

- `isass.boot.microservice.enabled=true` 时，只合并 `spring.application.name` 对应的静态文档和本服务本地
  Entrypoint；
- 单体模式下，合并 classpath 中所有服务静态文档和当前进程全部本地 Entrypoint；
- 路径或 Schema 冲突直接失败，不静默覆盖。

Entrypoint 文档来自运行时注解：

```java
@EntrypointOperation(
        operationName = "publish",
        displayName = "发布",
        description = "发布当前资源",
        displayOrder = 500,
        httpMethod = HttpMethod.POST)
void publish(@QueryParam("id") Long id);
```

不再使用 Javadoc `@http`、自有 Smart-doc 辅助 Maven 插件或
`META-INF/isass/nocode-contract.json`。未标注 `EntrypointOperation` 的方法不生成路由，也不进入 OpenAPI。
