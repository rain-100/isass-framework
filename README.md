# isass-framework

## 更新日志

 更新日志请查看 [docs/60.changelog](docs/60.changelog)

## 模块命名规范

模块目录和 Maven `artifactId` 统一使用 `isass-分类-模块名` 格式。

- `isass`：固定项目前缀。
- `分类`：模块所属能力域，例如 `core`、`database`、`mq`、`net`、`security`、`serialization`、`web`、`adapter`。
- `模块名`：模块的具体能力名称，应简洁表达职责，例如 `common`、`redis`、`springsecurity`、`protobuf`。

示例：

- `isass-core-common`
- `isass-database-redis`
- `isass-security-springsecurity`

新增或重命名模块时，应优先归入已有分类；只有现有分类无法准确表达能力域时，再新增分类。

## Git commit 规范

### commit message格式

``` text
<type>(<scope>): <subject>
```

#### type(必须)

用于说明git commit的类别，只允许使用下面的标识。
          
- feat：新功能（feature）。

- fix/to：修复bug，可以是QA发现的BUG，也可以是研发自己发现的BUG。
- fix：产生diff并自动修复此问题。适合于一次提交直接修复问题
- to：只产生diff不自动修复此问题。适合于多次提交。最终修复问题提交时使用fix

- docs：文档（documentation）。
 
- style：格式（不影响代码运行的变动）。
 
- refactor：重构（即不是新增功能，也不是修改bug的代码变动）。
 
- perf：优化相关，比如提升性能、体验。
 
- test：增加测试。
 
- chore：构建过程或辅助工具的变动。
 
- revert：回滚到上一个版本。
 
- merge：代码合并。
 
- sync：同步主线或分支的Bug。

#### scope(可选)

scope用于说明 commit 影响的范围，比如数据层、控制层、视图层等等，视项目不同而不同。

例如在Angular，可以是location，browser，compile，compile，rootScope， ngHref，ngClick，ngView等。如果你的修改影响了不止一个scope，你可以使用*代替。

#### subject(必须)

subject是commit目的的简短描述，不超过50个字符。

结尾不加句号或其他标点符号。

---

根据以上规范git commit message将是如下的格式：
- fix(DAO):用户查询缺少username属性 
- feat(Controller):用户查询接口开发

---

以上就是我们梳理的git commit规范，那么我们这样规范git commit到底有哪些好处呢？

- 便于程序员对提交历史进行追溯，了解发生了什么情况。
- 一旦约束了commit message，意味着我们将慎重的进行每一次提交，不能再一股脑的把各种各样的改动都放在一个git commit里面，这样一来整个代码改动的历史也将更加清晰。
- 格式化的commit message才可以用于自动化输出Change log。

## 服务注册与发现
- spring.cloud.discovery.enabled（不用配置）
- spring.cloud.nacos.discovery.enabled （是否启用 nacos 服务注册与发现，默认true）

## 配置中心
- spring.cloud.nacos.config.enabled （是否启用 nacos 配置中心，默认true）

## API 文档

isass v4 的 API 文档由两部分组成：

- OpenAPI：服务运行时暴露标准接口定义，推荐地址为 `/{spring.application.name}/v3/api-docs`。
- Service Docs：服务随包携带的 Markdown 文档，默认目录为 `src/main/resources/service-docs/`，推荐接口为 `/{spring.application.name}/service-docs`。

业务服务依赖 `isass-web-springmvc` 后，会自动暴露 Markdown 文档接口：

```text
GET /service-docs
GET /service-docs/{docId}
GET /{spring.application.name}/service-docs
GET /{spring.application.name}/service-docs/{docId}
```

资源目录和接口的映射规则如下：

```text
src/main/resources/service-docs/guide/token.md
  -> GET /{spring.application.name}/service-docs/guide/token

src/main/resources/service-docs/database/attachment-db.md
  -> GET /{spring.application.name}/service-docs/database/attachment-db
```

`GET /{spring.application.name}/service-docs` 返回当前服务的文档索引，供 zyplayer-doc 同步、前端页面和 AI 知识库采集使用。`GET /{spring.application.name}/service-docs/{docId}` 返回 Markdown 原文。

### 使用 screw 生成数据库文档

screw 需要通过 JDBC 连接数据库读取真实表结构，才能生成 Markdown、HTML 或 Word 文档。因此不要把 screw 绑定到默认构建流程，否则 CI/CD 没有数据库环境时会失败。

推荐每个业务服务提供一个默认不启用的 Maven profile，例如 `db-doc`：

```bash
mvn -pl isass-service-attachment-service -Pdb-doc generate-resources
```

生成目录建议固定为：

```text
src/main/resources/service-docs/database/
```

生成文件建议使用服务名或数据库名命名：

```text
src/main/resources/service-docs/database/attachment-db.md
```

开发人员本地具备数据库连接条件时，主动执行 `-Pdb-doc` 更新数据库文档；自动化构建只打包已经提交到仓库的 Markdown 文件。

### 使用 smart-doc

smart-doc 适合在不侵入业务代码的情况下，根据 JavaDoc 生成 API 文档、OpenAPI、Postman 等产物。isass v4 推荐将 smart-doc 作为开发期文档生成工具使用，而不是替代运行时 `/{spring.application.name}/v3/api-docs`。

推荐用法：

- 运行时调试和聚合：使用 SpringDoc/Knife4j 暴露 `/{spring.application.name}/v3/api-docs`。
- 离线 API 文档、Postman、AI 训练材料：使用 smart-doc 生成 Markdown/OpenAPI，并把 Markdown 放入 `src/main/resources/service-docs/`。
- 业务说明、鉴权说明、数据库说明：统一放入 `src/main/resources/service-docs/`。

示例目录：

```text
src/main/resources/service-docs/
  api/attachment-api.md
  guide/token.md
  guide/file-upload.md
  database/attachment-db.md
```

### 查看接口文档

isass v4 推荐单独部署 `zyplayer-doc` V3，把它作为统一文档平台。业务微服务依赖 `isass-apidoc-zyplayer` 后，可以在启动完成时把 `src/main/resources/service-docs/**/*.md` 同步到 zyplayer-doc。

zyplayer-doc 的 OpenAPI 密钥可以放在 Nacos 公共配置中，所有微服务共享一份配置：

```yaml
isass:
  apidoc:
    zyplayer:
      enabled: true
      base-url: http://127.0.0.1:8083
      api-key: ${ZYPLAYER_DOC_API_KEY}
      private-key: ${ZYPLAYER_DOC_PRIVATE_KEY}
      delete-missing: false
      release: true

info:
  service-name-cn: 附件微服务
```

同步时，框架会用服务中文名和版本号创建 zyplayer 空间：

```text
spring.application.name = attachment-service
info.service-name-cn = 附件微服务
info.version/git.build.version = 4.0.0-SNAPSHOT

zyplayer space name = 附件微服务v4.0.0
zyplayer space uuid = attachment-service:4.0.0
```

`-SNAPSHOT`、`-RC1` 等预发布后缀会被去掉，避免开发构建反复创建新空间。

同步策略：

- 先调用 zyplayer-doc `/openApi/v1/space/list` 查询空间，存在则复用，不存在则 `/openApi/v1/space/update` 创建。
- 再调用 `/openApi/v1/space/page/list` 查询页面树。
- 每个由 isass 管理的页面都会写入 `isass-doc-sync` HTML 注释标记，包含服务名、文档 ID 和内容 hash。
- 远端页面不存在则新增，hash 或标题变化则更新，未变化则跳过。
- `delete-missing=false` 是默认值，避免误删 zyplayer 里手工补充的文档。
- `delete-missing=true` 时，只删除带 `isass-doc-sync` 标记、且本次本地扫描不到的页面；手工页面永不删除。
- `release=true` 时，同步后调用 `/openApi/v1/space/page/release` 发布页面。

后续功能列表：

- 支持非 Spring 项目从 Maven `pom.xml` 的 `service-name-cn` 属性读取中文服务名。
- 支持把 OpenAPI JSON 转换为 zyplayer-doc `editorType=6` 的 API 调试页面结构。
