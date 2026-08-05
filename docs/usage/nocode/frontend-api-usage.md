# 零代码 HTTP 接口：前端使用说明

本文面向前端调用方。接口的最终字段、枚举值、必填项与自定义业务接口以业务服务提供的 OpenAPI / Knife4j 文档为准。

## 1. 基础约定

标准零代码接口的基础路径为：

```text
/{service}/{entity}
```

- `service`：服务名称，例如 `attachment-service`。
- `entity`：实体名首字母小写，例如 `attachment`、`iconGroup`。
- 标准接口的成功响应为 `Resp<T>`；`data` 才是业务数据。
- 业务自定义接口由其 `IXxxService` 方法的 `@http` 定义，路径与参数以 OpenAPI 文档为准。
- `GET`、`DELETE` 的复杂查询条件使用 query 参数；`POST`、`PUT` 的实体或复杂对象使用 JSON body，除非 OpenAPI 标记为 `multipart/form-data`。

通用成功响应：

```json
{
  "success": true,
  "status": 0,
  "message": "成功",
  "detailMessage": null,
  "data": {}
}
```

失败时依据 `success`、`status` 和 `message` 判断；开发环境的 `detailMessage` 仅用于排查，不应作为前端业务逻辑依据。

## 2. 标准接口

下表中的 `{base}` 指 `/{service}/{entity}`。

### 增

| 接口 | 方法与路径 | 参数 |
| --- | --- | --- |
| 新增单体 | `POST {base}` | body：实体 JSON |
| 批量新增 | `POST {base}/batch` | body：实体 JSON 数组 |
| 指定批次大小批量新增 | `POST {base}/batch/batchSize/{batchSize}` | path：`batchSize`；body：实体数组 |
| 条件不存在则新增 | `POST {base}/absent/criteria` | body：实体与 Criteria |
| 唯一列不存在则新增 | `POST {base}/absent/{uniqueColumns}` | path：`uniqueColumns`；body：实体 |
| 条件不存在则批量新增 | `POST {base}/batch/absent/criteria` | body：实体数组与 Criteria |
| 唯一列不存在则批量新增 | `POST {base}/batch/absent/{uniqueColumns}` | path：唯一列；body：实体数组 |
| 条件新增或更新 | `POST {base}/add-update/criteria` | body：实体与 Criteria |
| 唯一列新增或更新 | `POST {base}/add-update/{uniqueColumns}` | path：唯一列；body：实体 |
| 唯一列批量新增或更新 | `POST {base}/add-update/batch/{uniqueColumns}` | path：唯一列；body：实体数组 |

### 改

| 接口 | 方法与路径 | 参数 |
| --- | --- | --- |
| 按主键更新非空字段 | `PUT {base}` | body：实体 JSON（含 `id`） |
| 按主键更新全部字段 | `PUT {base}/allColumns` | body：实体 JSON（含 `id`） |
| 按主键更新，不存在即报错 | `PUT {base}/exception` | body：实体 JSON |
| 按条件更新 | `PUT {base}/criteria` | body：实体与 Criteria |
| 按条件更新，不匹配即报错 | `PUT {base}/criteria/exception` | body：实体与 Criteria |
| 批量保存 | `POST {base}/batchSave` | body：`BatchSave` JSON |

### 查

| 接口 | 方法与路径 | 参数 |
| --- | --- | --- |
| 按 id 查询 | `GET {base}/{id}` | path：`id` |
| 按 id 查询，不存在即报错 | `GET {base}/exception/{id}` | path：`id` |
| 条件查询一条 | `GET {base}/1/criteria` | query：Criteria 等值字段 |
| 条件查询一条，多条时告警 | `GET {base}/warn/criteria` | query：Criteria 等值字段 |
| 条件查询唯一一条 | `GET {base}/exception/criteria` | query：Criteria 等值字段 |
| 条件列表 | `GET {base}/criteria` | query：Criteria 等值字段 |
| 条件分页列表 | `GET {base}/page` | query：Criteria 等值字段、`pageNum`、`pageSize` |
| 查询全部列表 | `GET {base}/all` | 无 |
| 条件数量 | `GET {base}/count/criteria` | query：Criteria 等值字段 |
| 全部数量 | `GET {base}/count/all` | 无 |
| 按 id 是否存在 | `GET {base}/present/{id}` | path：`id` |
| 按属性是否存在 | `GET {base}/present/{propertyName}/{value}` | path：实体属性名和值 |
| 按条件是否存在 | `GET {base}/present/criteria` | query：Criteria 等值字段 |
| 按属性是否不存在 | `GET {base}/absent/{propertyName}/{value}` | path：实体属性名和值 |
| 按条件是否不存在 | `GET {base}/absent/criteria` | query：Criteria 等值字段 |
| 校验条件存在 | `GET {base}/exception-if-present/criteria` | query：Criteria 等值字段 |
| 校验条件不存在 | `GET {base}/exception-if-absent/criteria` | query：Criteria 等值字段 |

### 删

| 接口 | 方法与路径 | 参数 |
| --- | --- | --- |
| 按 id 删除 | `DELETE {base}/id/{id}` | path：`id` |
| 批量删除 | `DELETE {base}/{ids}` | path：`ids`；具体数组格式以 OpenAPI 为准 |
| 按条件删除 | `DELETE {base}/criteria` | query：Criteria 等值字段 |

## 3. Criteria 与分页规则

Criteria 是实体的查询条件。前端只应使用 API 文档列出的实体等值字段，例如：

```text
GET /attachment-service/iconGroup/criteria?id=9&iconGroupName=默认分组
```

- 不要提交 `Like`、`Or`、`setOr` 等内部增强条件字段。
- `selectColumns`、`orderBy`、`present/absent` 路径中的字段一律使用实体的驼峰属性名，例如 `nickName`、`createTime`；不要传 `NICK_NAME`、`create_time` 等数据库列名。ORM 会在基础设施层自动映射为物理列。
- 只有分页接口 `GET {base}/page` 使用 `pageNum`、`pageSize`；普通列表接口不要传分页参数。
- 枚举字段使用文档给出的值；不要假设枚举可以用整数或名称互换。

## 4. 自定义业务接口与文件接口

自定义方法可使用 `@http` 暴露。例如附件上传通常为：

```text
POST /attachment-service/attachment/upload
Content-Type: multipart/form-data
```

`multipart/form-data` 以 OpenAPI 中列出的表单字段为准；文件字段必须以 `file` 类型提交。不要把文件内容写入 JSON 或 Base64 后提交。

下载、预览、打包下载等返回文件流的接口不返回 `Resp`：

- 文件存在：直接消费响应流；
- 文件不存在：`404`；
- 参数错误：`400`；
- 服务端异常：`5xx`。

前端应使用浏览器下载、`blob` 或流式读取处理这类接口，而不是按 JSON 解析。

## 5. 高级响应格式化

查询接口支持高级响应格式化功能。通过 query 参数按字段声明，可单独或组合使用；格式化结果会在原值之外增加 `{字段名}Text`，方便页面直接展示。

支持以下三项功能：

1. 日期时间字段格式化：`dateFormat.{字段名}`。
2. 小数保留位数：`decimalPlaces.{字段名}`。
3. 字典翻译：`dictTranslation.{字段名}`。

### 5.1 日期时间字段格式化：`dateFormat.{字段名}`

用于把日期、时间、时间戳字段转换为页面直接展示的日期文本，原字段保留，便于前端直接展示。

```text
GET /bsp-service/iconGroup/criteria?dateFormat.createTime=yyyy-MM-dd
```

```json
{
  "createTime": "2026-07-12T15:00:00",
  "createTimeText": "2026-07-12"
}
```

日期格式遵循服务端日期格式规则，例如 `yyyy-MM-dd`、`yyyy-MM-dd HH:mm:ss`。

### 5.2 小数保留位数：`decimalPlaces.{字段名}`

用于按指定小数位展示金额、数量、比例等数值。原数值保持精度不变，`Text` 字段是页面展示用的格式化值。

```text
GET /attachment-service/icon/criteria?decimalPlaces.orderNum=2
```

```json
{
  "orderNum": 12.345,
  "orderNumText": "12.35"
}
```

小数位必须是整数；无效值会被忽略，不会导致业务请求失败。

### 5.3 字典翻译：`dictTranslation.{字段名}`

用于把枚举、状态码、类型编码转换为可展示的中文（或其他字典文本）。参数值是后端定义的字典类型编码。

```text
GET /attachment-service/iconGroup/criteria?dictTranslation.groupType=icon_group_type
```

```json
{
  "groupType": 1,
  "groupTypeText": "公开"
}
```

字典未配置、字段为空或字典查询失败时，不保证存在对应 `Text` 字段；前端应保留原编码的兜底展示策略。

### 5.4 组合使用

```text
GET /attachment-service/iconGroup/criteria
  ?dateFormat.createTime=yyyy-MM-dd
  &decimalPlaces.orderNum=2
  &dictTranslation.groupType=icon_group_type
```

普通实体、列表、Map 与分页记录均可投影。

## 6. 调用建议

1. 优先从 Knife4j/OpenAPI 生成客户端或请求类型，避免手写实体字段。
2. 标准 CRUD 使用本文路径；业务动作使用当前服务 OpenAPI 的自定义路径。
3. 前端统一解包 `Resp.data`，统一处理 `success=false`。
4. 对分页响应，以 API 文档中的分页结构为准；不要假设所有列表均分页。
5. 需要展示格式化值时按需添加高级响应格式化参数，避免在默认列表请求中增加不需要的字典查询开销。
