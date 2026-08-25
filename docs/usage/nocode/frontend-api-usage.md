# NoCode 前端 API

NoCode 基础路径固定为：

```text
/{serviceName}/nocode/{contextName}/{resourceName}
```

自定义业务入口不带 `nocode` 段，具体地址由 `EntrypointInfo` 与 `EntrypointOperation` 决定。前端应以
`/v3/api-docs` 为最终合同，不拼接旧实体路径，也不使用 Path 参数。

## 八个正式标准入口

| 操作 | 方法与路径 | 参数 |
| --- | --- | --- |
| 批量新增 | `POST {base}/createBatch` | Body：实体数组 |
| 超级增删改 | `POST {base}/superCud` | Body：`SuperCudReq` |
| 批量修改 | `PUT {base}/updateBatch` | Body：实体数组；Query：Criteria 与更新策略 |
| 批量删除 | `DELETE {base}/deleteBatch` | Query：Criteria |
| 分页查询 | `GET {base}/page` | Query：Criteria、页码与排序 |
| 游标分页 | `GET {base}/cursorPage` | Query：Criteria、`cursorId`、`pageSize`、`orderBy=id asc|desc` |
| 数量 | `GET {base}/count` | Query：Criteria |
| 是否存在 | `GET {base}/exists` | Query：Criteria |

`create`、`createIfAbsent`、`update`、`delete`、`getById`、`getOne`、`existsById`、`list`、`requireOne` 等都是 Java
便捷方法，不生成 HTTP/gRPC 路由。

## SuperCudReq

一个请求可以同时提交新增、修改和删除，并为同一批新增指定统一的不存在判断字段，为同一批修改指定统一
Criteria：

```json
{
  "addEntities": [],
  "addByFields": [],
  "updateEntities": [],
  "updateCriteria": null,
  "deleteIds": [],
  "deleteCriteria": []
}
```

`addByFields` 与 `updateCriteria.matchFields` 都使用 Java 属性名；Java Builder 同时支持 getter Lambda。
`addByFields` 非空时，每个新增实体按相同字段组合判断不存在后新增。`updateCriteria` 的普通条件作为每次更新
的公共范围，`matchFields` 再从当前更新实体提取等值条件。框架不限制这些字段是否唯一，也不判断多个实体的
更新范围是否重叠；唯一性、覆盖顺序和并发幂等性由业务负责。

`SuperCudResult` 只返回 `addedCount`、`updatedCount`、`deletedCount` 三个汇总影响数量，避免大批量写入返回
实体副本。全部操作在服务端同一事务提交；空请求是合法幂等 no-op。

Query 集合使用同名重复参数，例如 `idIn=1&idIn=2`；对象 Query 会把非空成员展开为同名参数，服务端按同一
规则反向绑定。`WRITE_NULL` 会根据请求字段出现性区分“未提交”和“显式 null”。
