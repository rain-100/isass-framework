# 错误响应策略

## 背景

Spring Boot 会把未命中路由、静态资源不存在、Filter 或框架层异常转发到 `/error`。这些请求可能来自浏览器页面，也可能来自前后端接口调用。框架需要同时满足两类诉求：

- 浏览器访问网页或静态资源不存在时，应保留标准 HTTP 状态，例如 404，避免强行返回业务 `Resp`
- API 调用失败时，应返回统一 `Resp` 结构，便于前端、微服务调用方和 AI 工具稳定解析错误码、错误消息和详细信息

## 响应场景

| 场景 | 判断依据 | HTTP 状态 | 响应体 |
| --- | --- | --- | --- |
| 浏览器打开不存在的页面 | `Accept: text/html` 且未显式请求 JSON | 原始错误状态，例如 404 | 空 body 或由 Spring Boot/前端容器处理的页面 |
| 静态资源不存在 | `Accept: text/html` 或浏览器资源请求 | 原始错误状态，例如 404 | 空 body，避免把 JS/CSS/图片请求污染成 `Resp` |
| 前端 API 请求不存在 | `Accept: application/json` | 原始错误状态，例如 404 | `Resp`，`status` 使用框架业务码映射，例如 `NOT_FOUND_404` |
| 微服务或工具调用 API | `Accept: application/json` 或未声明 HTML | 原始错误状态 | `Resp` |
| 进入 controller 前抛出 `UnifiedException` | `/error` 中能获取原始异常 | 原始错误状态 | `Resp`，沿用 `UnifiedException` 的业务码和消息 |
| 权限错误且 token 失效 | HTTP 403 + 请求头带 token + 当前用户为空 | 原始错误状态 | `Resp`，业务码转为 `JWT_TOKEN_ERROR` |

## 实现约定

- `IsassErrorController` 保持 `/error` JSON 处理能力，但在 `Accept: text/html` 且未请求 JSON 时只设置 HTTP 状态并返回空 body
- `ExceptionAdvice` 对进入 controller 后抛出的静态资源不存在异常，支持配置静默 404 URL：命中后直接返回 HTTP 404 空 body，不打印异常日志，也不包装 `Resp`
- `/favicon.ico` 是框架内置静默 404 URL，不需要业务项目额外配置
- `IStatusMapping` 通过构造器注入，缺失时退化为空列表，不应因为没有映射 Bean 产生 NPE
- `ErrorAttributes` 缺失 `status` 时，优先使用 `HttpServletResponse` 当前状态，仍缺失则按 500 兜底
- `message` 保持面向用户或调用方的简要信息，`detailMessage` 保留给异常详情链路，不在普通网页 404 场景中生成

## 静默 404 URL 配置

框架 Web 异常配置统一使用 `isass.framework.web.exception` 前缀。

- `show-detail-error`：是否在错误响应中输出详细异常信息，默认 `true`
- `prod-unified-message`：关闭详细异常信息时的统一提示语，默认 `系统繁忙，请稍后重试`
- `silent-not-found-urls`：静默返回 404 的静态资源 URL 列表，框架内置 `/favicon.ico`

完整配置示例：

```yaml
isass:
  framework:
    web:
      exception:
        show-detail-error: true
        prod-unified-message: 系统繁忙，请稍后重试
        silent-not-found-urls:
          - /robots.txt
          - /assets/missing-logo.png
```

`silent-not-found-urls` 也可以使用逗号分隔的 properties 形式：

```properties
isass.framework.web.exception.silent-not-found-urls=/robots.txt,/assets/missing-logo.png
```

未命中该列表的静态资源不存在异常，仍然按普通异常处理并打印明确日志，便于开发人员发现真实资源路径错误。

## 后续优化

- 如果业务微服务需要强制某些路径即使 `Accept: text/html` 也返回 `Resp`，可在后续增加配置化 API 路径模式
- 如果前端路由需要所有未知路径返回 `index.html`，应由前端网关或静态资源服务器处理，不应放在框架 `/error` 中兜底
