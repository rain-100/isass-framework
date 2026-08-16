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

`create`、`createIfAbsent`、`update`、`delete`、`getById`、`getOne`、`list`、`requireOne` 等都是 Java
便捷方法，不生成 HTTP/gRPC 路由。

## SuperCudReq

一个请求可以同时提交六类变化：

```json
{
  "addEntities": [],
  "addIfAbsentItems": [],
  "updateEntities": [],
  "updateByCriteriaItems": [],
  "deleteIds": [],
  "deleteCriteria": []
}
```

六组结果在 `SuperCudResult` 中按请求索引对齐，并在服务端同一事务提交。空请求是合法幂等 no-op。

Query 集合使用同名重复参数，例如 `idIn=1&idIn=2`；对象 Query 会把非空成员展开为同名参数，服务端按同一
规则反向绑定。`WRITE_NULL` 会根据请求字段出现性区分“未提交”和“显式 null”。
