# zyplayer-doc 接入指南

## 依赖

业务微服务需要依赖 `isass-apidoc-zyplayer`。服务启动后，框架会采集 smart-doc 生成的 `service-docs/api/openapi.json` 和 `service-docs` Markdown，并同步到独立部署的 zyplayer-doc。`service-docs/api` 下的 Markdown 不会上传。

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

`group-name` 表示 zyplayer 分组，默认是 `isass`。空间名使用 `info.service-name-cn`，空间 UUID 使用 `{spring.application.name}@{yyyyMMddHHmmssSSS}`，例如 `attachment-service@20260620091415999`。

zyplayer-doc 回收站彻底删除空间后仍会保留唯一编码占位。为了避免固定 UUID 被历史删除数据占用，isass 新建空间时会追加毫秒时间戳后缀；查询空间时只匹配这种带后缀的新规则，并选择同一微服务下时间戳最新的空间。

空间内一级目录固定按 `api接口`、`使用文档`、`设计文档`、`数据库文档` 排列。框架会为这些目录写入 `seqNo`，并在目录已存在时复用同名目录；开启 `delete-missing` 后，只会清理受框架管理目录下的空废弃目录和空重复目录。

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
