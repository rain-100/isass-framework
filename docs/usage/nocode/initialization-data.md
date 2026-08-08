# 零代码初始化数据导入导出

零代码初始化数据用于把开发环境已确认的角色、资源、权限或业务基础数据，以 JSON 形式随微服务发布。框架在服务启动后自动读取模块的 `resources/init/**/*.json`，并按 JSON 内每个实体所属的微服务分组：本地实现直接通过 Repository 插入，远程实现通过该服务的通用初始化接口插入。目录仅用于项目分类，不参与路由。

## 1. JSON 格式

一个文件是一个 JSON 对象。键为实体的 Nocode 名称，即实体类首字母小写后的名称；值为该实体的记录数组。导入器直接写入目标服务的本地 Repository，不调用业务应用服务或 CRUD 生命周期；因此没有数据库外键约束的数据不依赖 JSON 键或文件顺序。若业务表定义了真实外键，则由数据库约束决定父记录必须先存在。

```json
{
  "dictionaryType": [
    {
      "id": "2080000000000000001",
      "tenantId": "2080000000000000000",
      "bizType": "iimage_asset",
      "typeCode": "AGE_STAGE",
      "typeName": "年龄阶段",
      "enableFlag": 1
    }
  ],
  "dictionaryItem": [
    {
      "id": "2080000000000000002",
      "dictionaryTypeId": "2080000000000000001",
      "itemCode": "INFANT",
      "itemName": "婴儿",
      "orderNum": 10,
      "enableFlag": 1
    }
  ]
}
```

- 分布式 `Long` ID 建议写成字符串，避免 JavaScript 或 JSON 工具精度丢失。
- 初始化数据以主键为幂等键：相同 ID 已存在时跳过，不更新已有记录；不存在时插入。
- 文件内可包含多个实体；没有真实外键约束时，实体顺序无关。
- 关联查询字段仅用于响应投影，不应写入初始化 JSON。

## 2. 启动自动导入

将文件放入微服务模块：

```text
src/main/resources/init/<模块>-default.json
```

默认配置如下：

```yaml
isass:
  nocode:
    initialization:
      enabled: true
      location: classpath*:init/**/*.json
      fail-fast: true
```

`fail-fast` 为 `true` 时，JSON 格式、实体名称或数据库写入错误会阻止服务完成启动，避免出现部分内置数据。开发调试时可临时设为 `false`，框架会记录错误并继续启动。

### 跨微服务初始化

业务服务拥有自己的应用资源、权限与角色定义时，可按业务模块归档 JSON：

```text
src/main/resources/init/asset-service/iimage-tenant.json
```

上例可同时包含 `tenant`、`app`、`authResource` 等 BSP 实体以及其他服务的实体。框架按实体生成的 nocode 合同确定所属服务：BSP 实体在 BSP 进程内直接幂等写入；其他服务实体按服务分组后调用对应的 `/<微服务-service>/init-data/import`。因此一个 JSON 可以表达完整的跨服务初始化包，文件名和目录名不限制实体归属。

跨服务导入依赖调用方具备服务间认证，并能解析目标服务地址：优先配置 `http.<微服务-service>.url`，未配置时通过 Spring Cloud 服务发现获取实例。目标服务的 `/init-data/import` 仅接受其本地实体，调用方不能指定或越过目标服务写入其他微服务的数据。

同进程存在实体所属服务实现时走本地导入，否则调用目标服务的 HTTP 接口。缺少远程端点或服务间认证时，行为遵从 `fail-fast`：默认阻止启动，开发调试可将其设为 `false` 并记录错误后继续启动。实体名称在全部已加载合同中必须唯一；发生重名时需拆分或使用不同的实体名称，避免跨服务路由歧义。

## 3. HTTP 导出和导入

接口必须带微服务稳定前缀：

```text
GET  /<微服务-service>/init-data/export?entities=实体名1&entities=实体名2
POST /<微服务-service>/init-data/import
GET  /<微服务-service>/init-data/entities
```

例如 BSP：

```text
GET /bsp-service/init-data/export?entities=authRole&entities=authResource
POST /bsp-service/init-data/import
```

导出响应 `data` 直接是初始化 JSON 对象；导入请求体使用同一格式。导入响应会返回总处理数、插入数、跳过数，以及失败实体和失败原因。

`/entities` 只返回当前微服务中有本地标准 CRUD 实现的实体，不包含远程依赖服务的代理。返回项中的 `entity` 是导入导出使用的实体名称，`comment` 是生成实体类的 `COMMENT` 常量，值来自数据库表注释，可直接作为管理端的数据模型描述。可先调用该接口构建管理端的可选实体列表。HTTP 导入按实体独立执行，返回总处理数、插入数、跳过数和失败实体/原因；一个实体失败不会阻断其他实体导入。

HTTP 导入和导出会校验 URL 中的微服务与实体的本地服务归属，不允许经由 BSP 导入其他微服务的实体。接口权限由当前服务的权限配置管理；生产环境应仅向平台管理员角色授予这两个接口。

## 4. 推荐流程

1. 在开发环境通过资源、权限、角色等管理界面调试数据。
2. 调用导出接口获取 JSON，并审阅 ID 和敏感字段。
3. 将确认后的文件放入对应微服务的 `resources/init`。
4. 新环境启动时由框架自动插入；已有同 ID 数据保持不变。
# 初始化与完整数据导出

## 完整导出

`GET /{service}/init-data/export` 适合临时按实体导出。需要按条件导出时，调用 `POST /{service}/init-data/export` 并传入 `plans`；`criteria` 的键直接对应生成的 Criteria setter，`${export.实体.字段}` 可引用前序实体结果：

```json
{
  "plans": [
    {
      "service": "bsp-service",
      "entity": "DictionaryType",
      "criteria": { "bizType": "iimage_asset", "typeCode": "crowd" }
    },
    {
      "service": "bsp-service",
      "entity": "DictionaryItem",
      "criteria": { "dictionaryTypeIdIn": "${export.DictionaryType.id}" }
    }
  ]
}
```

`plans` 可跨微服务；执行器会按服务分组，本地服务直接查询，远程服务每个服务只调用一次内部导出接口。返回的 JSON 仍保留实体名称到记录数组的初始化格式。

需要复用固定的数据集时，在协调微服务的 `src/main/resources/export-profiles/` 中新增完整 YAML 档案：

```yaml
code: example-tenant
name: 示例租户完整配置
entities:
  - service: bsp-service
    entity: Tenant
    criteria:
      id: ${input.tenantId}
  - service: bsp-service
    entity: UserTenant
    criteria:
      tenantId: ${input.tenantId}
  - service: bsp-service
    entity: User
    criteria:
      idIn: ${export.UserTenant.userId}
```

调用 `POST /{协调服务}/init-data/export`：

```json
{
  "profileCode": "example-tenant",
  "input": { "tenantId": 10001 }
}
```

`${input.xxx}` 引用调用参数；`${export.实体.字段}` 引用同一服务内前序实体的字段集合，适合传给 `idIn`、`appIdIn` 等 Criteria 条件。执行器按微服务分组：本地服务直接查询，远程服务一次调用 `/{service}/init-data/export-internal` 返回该服务全部实体，最终生成一个按服务分组的 JSON 包。

完整导出不会脱敏或省略字段。密码哈希、服务账号凭证和 API Key 相关字段会原样写入 JSON，以便在新环境恢复后继续有效。因此导出包必须按生产密钥备份管理：只授予专门的导出权限、传输使用 TLS、文件不得提交仓库，并在受控密钥库或加密存储中保管。

将同一包提交到 `POST /{协调服务}/init-data/import-package` 即可恢复。协调服务会按 `services` 分组导入本地实体，并对每个远程服务仅发起一次导入调用；导入仍按 ID 幂等，已存在记录会跳过。
