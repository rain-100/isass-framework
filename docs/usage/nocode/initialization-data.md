# 零代码初始化数据导入导出

零代码初始化数据用于把开发环境已确认的角色、资源、权限或业务基础数据，以 JSON 形式随微服务发布。框架在服务启动后自动读取模块的 `resources/init/**/*.json`：根目录文件通过本地 Repository 插入数据，目标微服务目录中的文件通过目标服务的通用初始化接口插入数据。

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
      remote-enabled: false
```

`fail-fast` 为 `true` 时，JSON 格式、实体名称或数据库写入错误会阻止服务完成启动，避免出现部分内置数据。开发调试时可临时设为 `false`，框架会记录错误并继续启动。

### 跨微服务初始化

业务服务拥有自己的应用资源、权限与角色定义时，应将 JSON 放入目标服务目录：

```text
src/main/resources/init/bsp-service/<业务应用>-auth-resources.json
```

框架会将 `bsp-service` 识别为目标服务，并调用目标服务的通用 `/bsp-service/init-data/import` 接口。目标服务校验本地实体归属后，通过本地 Repository 幂等写入 JSON；业务项目无需修改 BSP 项目，也不会直接访问 `bsp_auth_*` 表或调用 `IAuthResourceService` 等业务 CRUD 接口。

跨服务导入依赖调用方具备服务间认证，并能解析目标服务地址：优先配置 `http.<微服务-service>.url`，未配置时通过 Spring Cloud 服务发现获取实例。目标服务的 `/init-data/import` 仅接受其本地实体，调用方不能指定或越过目标服务写入其他微服务的数据。

确认远程端点和服务间认证已配置后，业务服务将 `remote-enabled` 设置为 `true`，启动时即可导入目标服务目录中的 JSON。默认值为 `false`，避免缺少远程认证配置时导致业务服务无法启动。

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
