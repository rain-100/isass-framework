# zyplayer-doc 接入指南

## 依赖

业务微服务需要依赖 `isass-apidoc-zyplayer`。服务启动后，框架会采集运行时 OpenAPI 和 `service-docs` Markdown，并同步到独立部署的 zyplayer-doc。API 调试页面来自运行时 OpenAPI；`service-docs/api` 下的 Markdown 不会上传。

```xml
<dependency>
    <groupId>vip.isass</groupId>
    <artifactId>isass-apidoc-zyplayer</artifactId>
</dependency>
```

## 推荐配置

zyplayer-doc 的 OpenAPI 密钥可以放到 Nacos 公共配置中，让所有微服务共享一份配置。

```yaml
isass:
  apidoc:
    zyplayer:
      enabled: true
      base-url: http://127.0.0.1:8083
      api-key: ${ZYPLAYER_DOC_API_KEY}
      private-key: ${ZYPLAYER_DOC_PRIVATE_KEY}
      group-name: isass
      delete-missing: false
      release: true
      exclude-controllers:
        - com.example.AdminOnlyController
      exclude-paths:
        - /error
        - GET /internal/debug
      exclude-path-patterns:
        - /actuator/**
        - POST /internal/**

info:
  service-name-cn: 附件管理服务
```

`group-name` 表示 zyplayer 分组，默认是 `isass`。空间名使用 `info.service-name-cn`，空间 UUID 使用 `spring.application.name`。服务版本会写入 zyplayer-doc 的空间版本，不再拼接到空间名中。

## 过滤规则

框架内置过滤 `IsassErrorController`、`/error` 和 actuator 相关地址。业务服务可以通过配置追加过滤规则。

`exclude-paths` 和 `exclude-path-patterns` 都支持：

```text
url
METHOD url
```

例如：

```yaml
exclude-paths:
  - /internal/debug
  - GET /internal/health
exclude-path-patterns:
  - /admin/**
  - POST /debug/**
```

`/internal/debug` 会过滤所有 HTTP 方法；`GET /internal/health` 只过滤 GET 方法。

## 运行时文档地址

业务服务推荐暴露：

```text
GET /{spring.application.name}/v3/api-docs
GET /{spring.application.name}/service-docs
GET /{spring.application.name}/service-docs/{docId}
```

以附件服务为例：

```text
http://127.0.0.1:20320/attachment-service/v3/api-docs
http://127.0.0.1:20320/attachment-service/service-docs
```

统一查看文档时，打开已部署的 zyplayer-doc 前端页面即可。

## 空间版本

zyplayer-doc 可以在空间级别启用版本控制。isass 的规则是：

- 分组：项目，例如 `isass`。
- 空间：微服务中文名，例如 `附件管理服务`。
- 版本：微服务版本，例如 `4.0.0`。

`-SNAPSHOT`、`-RC1` 等预发布后缀会被去掉，避免开发构建重复创建空间版本。版本已存在时，框架会跳过创建并继续同步文档。
