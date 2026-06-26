# smart-doc 与服务文档使用指南

## 服务内 Markdown 文档

业务微服务把随服务发布的 Markdown 文档放在：

```text
src/main/resources/service-docs/
```

运行时推荐暴露：

```text
GET /{spring.application.name}/service-docs
GET /{spring.application.name}/service-docs/{docId}
```

`service-docs` 可以包含鉴权说明、使用指南、设计说明、数据库说明和开发期 API 生成物。其中 `guide`、`design`、`database` 下的 Markdown 会被 `isass-apidoc-zyplayer` 同步到 zyplayer-doc；`api` 目录不作为 Markdown 上传，在线调试接口由 smart-doc 生成的 OpenAPI 转换生成。

推荐目录：

```text
service-docs/
  api/        # API 生成物，例如 openapi.json，不上传 Markdown
  guide/      # 使用文档
  design/     # 设计文档
  database/   # 数据库文档
```

## screw 生成数据库文档

screw 需要在生成阶段通过 JDBC 连接数据库，读取真实表结构后才能输出 Markdown、HTML 或 Word 文档。因此不建议把 screw 绑定到默认构建流程，否则 CI/CD 没有数据库环境时会失败。

推荐放在默认不启用的 Maven profile 中，由开发人员本地按需执行：

```bash
mvn -pl isass-service-attachment-service -Pdb-doc generate-resources
```

输出目录建议固定为：

```text
src/main/resources/service-docs/database/
```

自动化构建只打包已经提交到仓库的 Markdown 文件，不主动连接数据库生成文档。

## smart-doc 生成 API 文档

isass v4 已将 smart-doc 的 `openapi` goal 绑定到 `compile` 阶段，继承 `isass-core-dependencies` 的微服务在编译时自动生成 `service-docs/api/openapi.json`，无需手动执行命令。

### 运行时暴露

- zyplayer-doc 在线调试使用 smart-doc 生成的 `service-docs/api/openapi.json`。
- 框架的 `/{spring.application.name}/v3/api-docs` 直接读取并返回 `service-docs/api/openapi.json`，用于单体调试或外部工具读取。
- `service-docs/api/*.md` 默认不上传 zyplayer-doc。

### 微服务配置

每个微服务在 `src/main/resources/smart-doc.json` 提供 smart-doc 配置。推荐模板：

```json
{
  "serverUrl": "http://127.0.0.1:20320",
  "outPath": "src/main/resources/service-docs/api",
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

- `outPath` 固定指向 `src/main/resources/service-docs/api`，产物随源码提交。
- `requestFieldToUnderline` / `responseFieldToUnderline` 必须设为 `false`，避免 smart-doc 将驼峰字段名转为下划线格式（如 `pageNumber` → `page_number`）。
- `allInOne` 设为 `true`，生成单个 `openapi.json` 文件，供 zyplayer-doc 和运行时 `/v3/api-docs` 统一读取。
- `packageFilters` 限定扫描的 Controller 包名，避免扫描框架内部或测试类。
- 其他配置项按需添加。

### 执行方式

编译时自动触发，无需额外命令：

```bash
mvn compile
```

禁用自动生成时，注释 `isass-core-dependencies` 中 smart-doc 插件的 `<phase>compile</phase>`，或临时跳过：

```bash
mvn compile -Dsmart-doc.skip=true
```

单独生成其他产物（Markdown、Postman 等）：

```bash
mvn smart-doc:markdown
mvn smart-doc:postman
```

### 常见问题

**Q: 生成的 OpenAPI 中 query 参数名是下划线格式（`page_number`），但接口实际接收驼峰格式（`pageNumber`）？**

A: 检查 `smart-doc.json` 中 `requestFieldToUnderline` 和 `responseFieldToUnderline`，必须设为 `false`。该配置**不支持** Maven 插件级覆盖，必须在每个项目的 JSON 文件中单独设置。

## 推荐 Javadoc 写法

smart-doc 会读取类、方法、参数和实体字段的 Javadoc。Controller 方法上应写清楚接口意图、参数中文说明和返回值；自定义 DTO、VO、Entity 的字段也应写 Javadoc，smart-doc 会在解析复杂参数或返回对象时读取字段说明。

常用标签：

| 标签 | 作用 |
| --- | --- |
| `@apiNote` | 方法详细说明 |
| `@param 参数名 描述\|示例值` | 参数说明和示例值 |
| `@return` | 返回值说明 |
| `@download` | 标记文件下载接口 |
| `@ignore` | 忽略类或方法 |
| `@ignoreParams` | 忽略指定请求参数 |
| `@response` | 补充响应字段说明 |
| `@tag` | 接口分组，可把不同 controller 的接口归入同一分类 |
| `@extension` | 扩展自定义元数据 |

示例：

```java
/**
 * 上传附件
 *
 * @apiNote 接收浏览器上传的文件，保存后返回附件 ID 和访问地址。
 * @tag 附件文件
 * @param file 上传文件|avatar.png
 * @param param 上传参数
 * @return 上传结果
 */
```

实体字段示例：

```java
/**
 * 业务类型，用于隔离不同业务模块的附件。
 */
private String bizType;
```

## API 分组

zyplayer-doc 的 API 接口目录默认使用 OpenAPI operation 的 `tags`。如果一个业务场景跨多个 controller，建议使用 smart-doc 的 `@tag` 写同一个分组名，让生成结果保持业务视角一致。
