# OpenAPI 3 资源目录硬切换设计

## 目标

将 smart-doc 生成的 OpenAPI 3 文档从历史复合文档目录
`src/main/resources/service-docs/api/openapi.json` 硬切换到语义明确的
`src/main/resources/openapi3/openapi.json`。

新路径不兼容旧路径，不提供回退读取或可配置双路径。

## 范围

### isass-framework-v4

- `ServiceDocsScanner` 使用的 OpenAPI classpath 路径改为
  `openapi3/openapi.json`。
- 相关测试 fixture 从 `service-docs/api/openapi.json` 迁移到
  `openapi3/openapi.json`。
- 更新 smart-doc 使用指南、当前 README/设计说明和 ChangeLog 中仍作为现行约定的路径。
- 历史实施计划和历史记录保留原文，不追溯改写。
- `/v3/api-docs` HTTP 路由保持不变。
- Markdown `service-docs` 扫描能力本身保持不变，避免扩大为无关框架删除。

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

- 保持通过框架 `ServiceDocsScanner` 获取原始 OpenAPI 的调用方式。
- 更新涉及资源路径的测试、配置或文档；若没有直接硬编码，则不做无意义改动。
- Knife4j、V3 schema 增强、缓存和 swagger-config 行为保持不变。

## 数据流

```text
smart-doc
  -> src/main/resources/openapi3/openapi.json
  -> classpath:/openapi3/openapi.json
  -> ServiceDocsScanner.readOpenApiJson()
  -> OpenApiEnhancer
  -> GET /v3/api-docs
  -> Knife4j
```

## 删除与兼容策略

- 这是一次硬切换。
- 旧的 `service-docs/api/openapi.json` 不再生成、不再读取。
- 旧路径缺失时不做兼容处理。
- 新路径缺失时沿用当前 `ServiceDocsScanner` 的未找到异常行为。

## 测试与验证

- 先修改框架测试，使其只在 `openapi3/openapi.json` 下提供 fixture，并确认旧实现测试失败。
- 修改框架路径常量后确认 scanner/controller 测试通过。
- 运行 apidoc 全量测试，确认增强与 e2e 路由继续通过。
- 编译 attachment，确认 smart-doc 在新目录生成 `openapi.json`。
- 验证 attachment 的 `service-docs/` 已删除，screw 配置未变化。
- 按 R1 → R2 → R3 顺序执行 Maven 安装验证。

