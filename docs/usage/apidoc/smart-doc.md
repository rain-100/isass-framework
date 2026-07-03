# Smart-Doc、OpenAPI 3 与 Knife4j

## 文档链路

```text
强类型 Controller + 实体 Javadoc
  -> Smart-Doc
  -> classpath:/openapi3/openapi.json
  -> OpenApiEnhancerSpi
  -> GET /v3/api-docs
  -> Knife4j /doc.html
```

Smart-Doc 负责解析 Java 类型和 Javadoc；OpenAPI 增强器只做 V3 路径折叠和 `oneOf` 组合；Knife4j 负责展示与在线调试。

## Smart-Doc 配置

每个微服务在 `src/main/resources/smart-doc.json` 配置：

```json
{
  "serverUrl": "http://127.0.0.1:20320",
  "outPath": "src/main/resources/openapi3",
  "projectName": "your-service-name",
  "allInOne": true,
  "coverOld": true,
  "componentType": "NORMAL",
  "packageFilters": "vip.isass.your.controller.*",
  "requestFieldToUnderline": false,
  "responseFieldToUnderline": false,
  "inlineEnum": true,
  "displayActualType": true,
  "isStrict": false
}
```

关键项：

- `outPath` 固定为 `src/main/resources/openapi3`。
- `allInOne` 生成单个 `openapi.json`。
- `componentType: NORMAL` 生成稳定、可读的命名 Schema。
- 请求和响应字段不转换为下划线。
- `packageFilters` 必须覆盖业务 Controller 和生成的 V3 实体 Controller。

生成命令：

```bash
mvn -Psmart-doc smart-doc:openapi
```

## 运行时接口

```text
GET /v3/api-docs
GET /{spring.application.name}/v3/api-docs
GET /v3/api-docs/swagger-config
GET /doc.html
```

服务从 `classpath:/openapi3/openapi.json` 读取原始文档，调用 `OpenApiEnhancerSpi` 后缓存最终结果。

## Javadoc

Smart-Doc 直接读取 Controller、参数、返回值和实体字段的 Javadoc。实体字段无需增加 Swagger、Schema 或自定义运行时描述注解。

V3 实体模板会把数据库字段注释写入字段 Javadoc。重新生成实体后，Smart-Doc 将其写入命名 Schema 的 `description`。

常用标签：

| 标签 | 作用 |
| --- | --- |
| `@apiNote` | 方法详细说明 |
| `@param` | 参数说明与示例 |
| `@return` | 返回值说明 |
| `@download` | 标记文件下载 |
| `@ignore` | 忽略类或方法 |
| `@ignoreParams` | 忽略指定请求参数 |
| `@response` | 补充响应字段说明 |
| `@tag` | 接口分组 |
| `@extension` | 扩展自定义元数据 |

## V3 文档

每个 V3 实体生成一个强类型 Controller。Smart-Doc 首先输出完整的实体路径、命名请求 Schema 和 `Resp<T>` 响应 Schema。

`V3OpenApiEnhancer` 再把实体路径折叠为：

```text
/{serviceName}/{entityName}/v3/**
```

请求体和响应通过 `oneOf` 保留各实体类型；Criteria 参数从实体 Schema 属性生成，只展示等值字段和允许的通用字段。分页参数只出现在分页查询接口。

## 数据库文档

screw 仅作为手工数据库文档工具，不属于 API 文档运行时：

```bash
mvn -pl isass-service-attachment-service -Pdb-doc generate-resources
```
