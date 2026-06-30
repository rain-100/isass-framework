# OpenAPI 3 资源目录硬切换设计

## 目标

将 smart-doc 生成的 OpenAPI 3 文档从历史复合文档目录
`src/main/resources/service-docs/api/openapi.json` 硬切换到语义明确的
`src/main/resources/openapi3/openapi.json`。

新路径不兼容旧路径，不提供回退读取或可配置双路径。

## 范围

### isass-framework-v4

- 删除 `ServiceDocsScanner`、`ServiceDoc` 及 Markdown 服务文档接口
  `/service-docs`、`/service-docs/**`。
- `ServiceDocsController` 仅保留 `/v3/api-docs`，并直接读取
  `classpath:/openapi3/openapi.json`。
- Controller 对最终响应使用 `volatile` + 双检锁进行懒加载缓存；读取或增强失败时
  不写入缓存，后续请求可以重试。
- `OpenApiEnhancerSpi` 改为 `String enhance(String rawOpenApiJson)`，只负责转换传入的
  OpenAPI JSON，不负责资源读取或缓存。
- 相关测试 fixture 从 `service-docs/api/openapi.json` 迁移到
  `openapi3/openapi.json`。
- 更新 smart-doc 使用指南、当前 README/设计说明和 ChangeLog 中仍作为现行约定的路径。
- 历史实施计划和历史记录保留原文，不追溯改写。
- `/v3/api-docs` HTTP 路由保持不变。

### isass-service-attachment

- smart-doc 的 `outPath` 改为 `src/main/resources/openapi3`。
- 删除当前整个 `src/main/resources/service-docs/`，包括：
  - `service-docs/api/`
  - `service-docs/database/`
  - `service-docs/guide/`
- 将生成并提交的 OpenAPI 文档放到 `src/main/resources/openapi3/openapi.json`。
- screw 插件及其输出配置保持原样；以后手动运行 screw 时仍可重新创建
  `service-docs/database/`。

### isass-service-apidoc

- `V3OpenApiEnhancer` 继续负责 V3 schema 注入与 request body 改写。
- 删除当前依赖 `ServiceDocsScanner` 并自行缓存的 `OpenApiEnhancer` 包装逻辑，或将其
  精简为实现新 SPI 的纯增强适配器。
- `ApidocAutoConfiguration` 不再注入 `ServiceDocsScanner`。
- 更新涉及资源路径的测试、配置或文档；若没有直接硬编码，则不做无意义改动。
- Knife4j、V3 schema 增强和 swagger-config 行为保持不变；最终文档缓存移至框架
  Controller。

## 数据流

```text
smart-doc
  -> src/main/resources/openapi3/openapi.json
  -> classpath:/openapi3/openapi.json
  -> ServiceDocsController
  -> OpenApiEnhancerSpi.enhance(rawOpenApiJson)
  -> ServiceDocsController final-result cache
  -> GET /v3/api-docs
  -> Knife4j
```

## 删除与兼容策略

- 这是一次硬切换。
- 旧的 `service-docs/api/openapi.json` 不再生成、不再读取。
- `/service-docs` 与 `/service-docs/**` 不再提供。
- 旧路径缺失时不做兼容处理。
- 新路径缺失时 `/v3/api-docs` 返回 404。
- 资源读取或增强异常不会污染缓存。

## 测试与验证

- 先修改框架 controller 测试，使其只在 `openapi3/openapi.json` 下提供 fixture，并确认
  旧实现测试失败。
- 增加缓存只构建一次、构建失败后可重试、增强器存在与缺失两条路径的测试。
- 删除 scanner、Markdown controller 与路径工具的专用测试。
- 运行 apidoc 全量测试，确认增强与 e2e 路由继续通过。
- 编译 attachment，确认 smart-doc 在新目录生成 `openapi.json`。
- 验证 attachment 的 `service-docs/` 已删除，screw 配置未变化。
- 按 R1 → R2 → R3 顺序执行 Maven 安装验证。
