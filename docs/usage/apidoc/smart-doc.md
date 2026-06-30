# smart-doc 与 OpenAPI 3 使用指南

## 生成目录

isass v4 使用 smart-doc 在编译阶段生成 OpenAPI 3 文档，固定产物为：

```text
src/main/resources/openapi3/openapi.json
```

该目录只存放 OpenAPI 3 产物，不再放入历史 `service-docs` 复合文档目录。
旧的 `service-docs/api/openapi.json` 不再生成，也不会被框架读取。

## 运行时接口

框架提供：

```text
GET /v3/api-docs
GET /{spring.application.name}/v3/api-docs
```

接口直接读取 `classpath:/openapi3/openapi.json`，在可用时调用
`OpenApiEnhancerSpi` 增强内容，并缓存最终结果。Knife4j 使用该接口展示和调试 API。

## 微服务配置

每个微服务在 `src/main/resources/smart-doc.json` 提供 smart-doc 配置：

```json
{
  "serverUrl": "http://127.0.0.1:20320",
  "outPath": "src/main/resources/openapi3",
  "projectName": "your-service-name",
  "allInOne": true,
  "coverOld": true,
  "packageFilters": "vip.isass.your.controller.*",
  "requestFieldToUnderline": false,
  "responseFieldToUnderline": false,
  "inlineEnum": true,
  "displayActualType": true,
  "isStrict": false
}
```

- `outPath` 固定为 `src/main/resources/openapi3`。
- `allInOne` 必须为 `true`，生成单个 `openapi.json`。
- `requestFieldToUnderline` 与 `responseFieldToUnderline` 必须为 `false`，
  避免把实际的驼峰参数名错误转换成下划线格式。
- `packageFilters` 应限制到业务 Controller 和需要输出的 V3 通用 Controller。

编译时会自动生成：

```bash
mvn compile
```

临时跳过生成：

```bash
mvn compile -Dsmart-doc.skip=true
```

## screw 数据库文档

screw 保留为手工、按需使用的数据库文档工具，不属于 Knife4j API 文档运行时。
它需要连接真实数据库，因此不应绑定到默认构建。

例如：

```bash
mvn -pl isass-service-attachment-service -Pdb-doc generate-resources
```

attachment 当前仍配置为输出到：

```text
src/main/resources/service-docs/database/
```

这个目录默认不存在；只有手工运行 screw 时才会重新创建。框架不再扫描或暴露其中内容。

## Javadoc 要求

smart-doc 会读取 Controller、参数、返回值和 DTO/Entity 字段的 Javadoc。常用标签：

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
