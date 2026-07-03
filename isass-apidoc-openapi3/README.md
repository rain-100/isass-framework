# isass-apidoc-openapi3

Isass 的 OpenAPI 3 文档前端，基于 Knife4j Vue 2 前端源码维护。

该模块在 Knife4j 的基础上增加：

- `oneOf` 请求体模型选择；
- `array.items.oneOf` 批量请求模型选择；
- 选择模型后自动生成请求示例；
- 根据 `x-isass-oneof-mapping` 自动填写 `entityName` 路径参数。

## 构建前端

```shell
cd frontend
npm install
npm run build3
```

构建结果会复制到 `src/main/resources/META-INF/resources`，随后可正常执行 Maven 打包。

上游项目：<https://github.com/xiaoymin/knife4j>

上游代码依据 Apache License 2.0 使用和修改，许可证见 `LICENSE-KNIFE4J`。
