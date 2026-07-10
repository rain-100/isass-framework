# Smart-Doc 与 V3 OpenAPI

Smart-Doc 负责业务手写 Controller，并把结果生成到：

```text
src/main/resources/openapi3/openapi.json
```

推荐配置：

```json
{
  "componentType": "NORMAL",
  "openUrl": "http://127.0.0.1:${server.port}/${spring.application.name}/v3/api-docs"
}
```

`componentType: NORMAL` 让普通接口使用稳定命名 Schema。

## V3 文档来源

V3 标准接口不依赖实体 Controller。API 模块在 Maven `generate-resources` 阶段由 `isass-nocode-generator` 生成：

```text
META-INF/isass/v3-contract.json
proto/<application>-v3.proto
```

契约生成器直接读取 `IV3XxxService`、实体源码及 Javadoc。实体字段无需增加 Swagger、Schema 或自定义描述注解。

每个实体可生成一个轻量 `V3XxxController` 作为手写 Spring MVC 扩展入口。它不实现 `IV3Controller`，不暴露标准 CRUD；仅用业务微服务自定义的业务接口。Smart-Doc 会扫描这些手写接口并保留其普通 OpenAPI path。

文档服务通过 `OpenApiEnhancerSpi`：

1. 读取 Smart-Doc 的普通接口文档；
2. 从 classpath 加载 V3 契约；
3. 添加命名实体 Schema、统一 V3 path、`oneOf` 和 Criteria 元数据；
4. 保留普通 Controller 的 path、tag 和顺序。

## 自定义 V3 接口

在 `IV3XxxService` 方法 Javadoc 中使用：

```java
/**
 * 查询租户可用图标
 *
 * @param tenantId 租户 ID
 * @http GET /available/{tenantId}
 * @order 501
 */
List<V3Icon> findAvailableIcons(Long tenantId);
```

- `@http` 自定义 HTTP 方法和相对路径；
- `@order` 自定义文档顺序；
- `@param` 提供参数说明；
- 方法和返回类型说明使用普通 Javadoc。

标准 V3 接口按“增、改、查、删”排序，标题采用“动作-参数-结果”，例如“增-批量”“查-根据条件-分页列表”。

## Criteria

统一文档只展示实体等值字段，并补充 `selectColumns`、`orderBy`。分页参数只出现在分页接口。增强条件字段不会展示，但服务内部仍可使用。
