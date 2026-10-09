# isass-web-springmvc

Spring MVC 服务端适配。发布 Entrypoint/NoCode HTTP 路由，统一响应、异常、文件流和 OpenAPI 展示。

- JSON 复用 JsonUtil；业务 API 返回 Resp，文件流保留二进制头和 HTTP 错误语义。
- API 文档以运行时 /{serviceName}/v3/api-docs 为入口；路由与字段元数据来自实际服务契约。

[返回模块目录](../README.md)
