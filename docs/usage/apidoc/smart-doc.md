# Smart-Doc 与零代码 OpenAPI

Smart-Doc 负责扫描业务手写 Controller，并生成：

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

`/v3/api-docs` 是 OpenAPI 规范文档地址，不是零代码接口版本号。

## 零代码文档来源

标准零代码接口不依赖实体 Controller。API 模块在 Maven `generate-resources` 阶段由 `isass-nocode-generator` 生成：

```text
META-INF/isass/nocode-contract.json
proto/<application>-nocode.proto
```

契约生成器读取 `IXxxService`、实体源码和 Javadoc。实体字段无需增加 Swagger、Schema 或其他描述注解。

文档服务通过 `OpenApiEnhancerSpi`：

1. 读取 Smart-Doc 的手写接口文档；
2. 从 classpath 加载 nocode 合同；
3. 添加命名实体 Schema、标准路径、`oneOf` 和 Criteria 元数据；
4. 保留手写 Controller 的 path、tag 和顺序。

标准接口统一路径为：

```text
/{serviceName}/{entityName}
```

自定义业务接口使用所属 `IXxxService` 的完整业务路径，例如：

```text
/attachment-service/attachment/upload
```

## 自定义零代码接口

在 `IXxxService` 方法 Javadoc 中声明 HTTP 合同：

```java
/**
 * 查询租户可用图标
 *
 * @param tenantId 租户 ID
 * @http GET /available/{tenantId}
 * @order 501
 */
List<Icon> findAvailableIcons(Long tenantId);
```

- `@http` 定义 HTTP 方法和相对于实体路径的路径；
- `@order` 定义 API 文档顺序；
- `@param` 提供参数说明；
- 未声明 `@http` 的自定义业务方法会在合同生成阶段明确报错。

标准接口按“增、改、查、删”排序，标题采用“动作-参数-结果”，例如“增-批量”“查-根据条件-分页列表”。

## Criteria

统一文档只展示实体等值字段，并补充 `selectColumns`、`orderBy`。分页参数仅在分页接口展示。`Like`、`Or`、`setOr` 等内部增强条件不作为前端标准参数公开。

前端调用规则、响应结构、文件接口与高级响应投影应以生成的 OpenAPI/Knife4j 文档为准。
