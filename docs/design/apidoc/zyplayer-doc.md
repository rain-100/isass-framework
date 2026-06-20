# zyplayer-doc 对接设计

## 定位

isass v4 使用 zyplayer-doc V3 作为统一文档平台。每个微服务仍然在自身运行时暴露 OpenAPI 和 `service-docs` Markdown，`isass-apidoc-zyplayer` 在服务启动后把这些文档同步到 zyplayer-doc。

这种方式保留了微服务单体启动时的自描述能力，也让前端、测试、AI 工具可以从 zyplayer-doc 看到聚合后的项目文档。

## zyplayer 组织模型

isass 对 zyplayer-doc 的组织约定如下：

| zyplayer 概念 | isass 映射 | 示例 |
| --- | --- | --- |
| 分组 | 项目 | `isass` |
| 空间 | 微服务中文名 | `附件管理服务` |
| 空间 UUID | 微服务应用名 + 创建时间戳 | `attachment-service@20260620091415999` |
| 空间版本 | 微服务主版本 | `v4.x` |
| 页面 | API 接口文档、Markdown 文档或文件夹 | `文件浏览服务/文件列表` |

`group-name` 默认是 `isass`，框架会先调用 `/openApi/v1/spaceGroup/list` 查询分组，找不到时调用 `/openApi/v1/spaceGroup/update` 创建分组。

空间名使用 `info.service-name-cn`。空间 UUID 使用 `{spring.application.name}@{yyyyMMddHHmmssSSS}`，例如 `attachment-service@20260620091415999`。zyplayer-doc 回收站彻底删除空间后仍会保留唯一编码占位，因此 isass 不使用固定 UUID；查询空间时只匹配带时间戳后缀的新规则，并选择同一微服务下时间戳最新的空间，不兼容旧的 `spring.application.name` 固定值。空间不再拼接版本号，服务版本通过 zyplayer-doc 的空间版本功能管理。框架把 `4.0.0-SNAPSHOT`、`4.1.2` 等服务版本归一化为 `v4.x` 这样的主版本；已有空间会先查询版本是否存在，新空间直接创建当前主版本。

## 同步流程

`isass-apidoc-zyplayer` 启动同步时执行：

1. 查询或创建 zyplayer 分组。
2. 查询或创建微服务空间，并开启空间版本控制。
3. 查询或创建当前微服务版本。
4. 采集运行时 OpenAPI，转换为 zyplayer `editorType=6` 的 API 接口文档。
5. 采集 `src/main/resources/service-docs/**/*.md`，转换为 zyplayer `editorType=2` 的 Markdown 文档。
6. 按目录策略创建文件夹。
7. 对比远端页面上的 `isass-doc-sync` 标记和内容 hash，新增、更新或跳过页面。
8. `delete-missing=true` 时，只删除带有 isass 同步标记且本地已不存在的页面。
9. `release=true` 时，同步后发布页面。

## 目录策略

空间内默认创建四个一级目录：

| 排序 | 目录 | 内容 |
| --- | --- | --- |
| 10 | `api接口` | 运行时 OpenAPI operation 转换后的 API 调试页面 |
| 20 | `使用文档` | 使用说明、鉴权说明、示例说明等 Markdown |
| 30 | `设计文档` | 设计类 Markdown |
| 40 | `数据库文档` | screw 或人工维护的数据库 Markdown |

框架创建或复用一级目录时会写入 zyplayer-doc 的 `seqNo` 排序字段。同步时按 `(parentId, name)` 判断目录是否已经存在，避免重复创建同名目录；`delete-missing=true` 时会清理受框架管理目录下的空废弃目录和空重复目录，已有内容的重复目录会保留，避免误删人工维护内容。

`api接口` 的二级目录默认取 OpenAPI operation 的第一个 `tags`。框架读取 smart-doc 生成的 `classpath:service-docs/api/openapi.json`，因此 Controller JavaDoc 中的 `@tag`、方法说明、参数说明和实体字段说明会进入 zyplayer API 页面。未提供 tag 时归入默认分类。

Markdown 文档按 `service-docs` 目录归类。`service-docs/api/**` 只作为开发期 API 生成物目录，不作为 Markdown 上传到 zyplayer-doc；在线调试接口来自 `service-docs/api/openapi.json`。

| 资源目录 | zyplayer 目录 |
| --- | --- |
| `service-docs/guide/**` | `使用文档` |
| `service-docs/design/**` | `设计文档` |
| `service-docs/database/**` | `数据库文档` |

## editorType 类型

| editorType | 类型 | 用途 | isass 推荐使用场景 |
| --- | --- | --- | --- |
| 0 | 文件夹 | 只作为页面树目录，不是正文文档 | 自动创建 `api接口`、`设计文档`、`使用文档`、`数据库文档` |
| 1 | 富文本 | TinyMCE 富文本页面 | 手工维护的运营说明或复杂格式文档 |
| 2 | Markdown | Markdown 页面 | `service-docs/**/*.md`、token 使用说明、数据库 Markdown 文档 |
| 3 | 旧表格 | 旧版表格视图 | 兼容历史数据，不建议新增 |
| 4 | 旧大纲/脑图 | 旧版大纲视图 | 兼容历史数据，不建议新增 |
| 5 | 文件 | 上传 Word、Excel、PPT、PDF、图片、文本等原始文件 | 附件型资料 |
| 6 | API 接口 | zyplayer-doc 在线调试接口页面 | OpenAPI operation 转换后的接口调试文档 |
| 7 | 思维导图 | Markmap 思维导图 | 模块关系、业务流程 |
| 8 | draw.io 流程图 | draw.io 图形文档 | 架构图、流程图 |
| 9 | 表格 | Univer 表格 | 在线维护结构化表格 |
| 10 | 引用文档 | 引用其他空间或页面 | 跨空间复用文档 |
| 11 | Excalidraw 手绘白板 | 手绘白板 | 草图、会议白板 |
| 12 | 页面构建器 | 可视化页面构建 | 复杂组合页面 |

## 过滤策略

框架内置过滤不适合暴露给开发者的通用接口，例如：

- `IsassErrorController`
- `/error`
- `/actuator/**`
- `/*/actuator/**`

业务服务可以通过配置追加：

- `exclude-controllers`：按 controller 简名或全限定名过滤。
- `exclude-paths`：按精确 URL 过滤。
- `exclude-path-patterns`：按 Ant URL pattern 过滤。

`exclude-paths` 和 `exclude-path-patterns` 都支持两种写法：

```yaml
exclude-paths:
  - /internal/debug
  - GET /internal/health
exclude-path-patterns:
  - /actuator/**
  - POST /internal/**
```

不写 method 表示所有 HTTP 方法都过滤；写成 `METHOD URL` 时，只过滤指定方法。

## 后续功能

- 支持非 Spring 项目从 Maven `pom.xml` 的 `service-name-cn` 属性读取中文服务名。
- 通过 zyplayer 官方 OpenAPI 补全空间版本列表接口的路径兼容矩阵。
- 扩展更多 zyplayer 文档类型，例如表格、draw.io、思维导图和附件文件。
