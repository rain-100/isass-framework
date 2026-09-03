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
@EntrypointInfo(
        serviceName = "asset-service",
        contextName = "sample",
        resourceName = "sampleGroup",
        displayOrder = 100,
        tag = "样片组")
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

## 接口分组

- `ICrudService` 提供的八个 NoCode 标准操作统一使用“零代码接口” tag，并且每种操作在 Knife4j 中只显示
  一次。文档使用 `/{service}/nocode/{entity}/{operationName}` 动态投影，调试页通过下拉框选择实际
  `service` 和 `contextName/resourceName`；不能把每个资源的同名 CRUD 操作重复展开。
- 动态文档路径只用于合并展示和调试。实际 HTTP 路由仍是
  `/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}`，不改变服务端寻址、权限资源或客户端
  调用协议。
- OpenAPI 操作通过 `x-isass-service-entities`、`x-isass-entity-options`、
  `x-isass-criteria-parameters` 和 `x-isass-oneof-mapping` 提供资源下拉、Criteria 参数过滤及请求体模型切换；
  新的 OpenAPI 组装实现不得删除这些扩展元数据。
- `x-isass-entity-options` 的显示标签使用实体 `resourceName` 的小驼峰值（例如 `sampleGroup`、`modelFace`），
  不使用中文实体名；选项值仍为 `contextName/resourceName`（例如 `sample/sampleGroup`），用于拼接实际请求路径。
- 自定义 `EntrypointOperation` 使用所属 `EntrypointInfo.tag` 作为中文分组名，每个实际包含自定义操作的业务
  资源才形成独立分组。`tag` 只影响文档展示，不参与 URL、权限标识或远程寻址。
- `EntrypointInfo.displayOrder` 控制业务分组在 OpenAPI/Knife4j 中的顺序，数值越小越靠前；默认值为 `1000`。
  NoCode 生成器沿用该默认值，只有顶层汇总 tag“零代码接口”额外以 `x-order=1` 声明，并在同序时优先展示。
- `tag` 为空时兼容回退为 `contextName/resourceName`；业务接口和生成器必须显式提供中文 `tag`，不能依赖
  技术名回退作为正式文档名称。
- 同一个 `ICrudService` 可以同时包含 NoCode 标准操作与自定义操作；两类操作必须根据操作自身的 `nocode`
  元数据分别归组，不能按接口整体归组。
- `EntrypointOperation.displayName` 必须使用简短中文动作名称；稳定英文名称只写在 `operationName`。
