# NoCode CRUD 统一执行与生命周期

## 1. 正式入口

NoCode 只发布八个 HTTP/gRPC 正式入口：

| 类型 | 正式入口 | 统一执行边界 |
| --- | --- | --- |
| 写入 | `createBatch`、`updateBatch`、`deleteBatch`、`superCud` | `CrudWriteExecutor.superCud` |
| 查询 | `page`、`cursorPage`、`count`、`exists` | `CrudQueryExecutor.query` |

`create`、`createIfAbsent`、`update`、`delete`、`getById`、`getOne`、`existsById`、`requireOne`、`list` 等方法仅为
Java 便捷方法，不标注 `@EntrypointOperation`，也不产生独立 HTTP、gRPC 或 OpenAPI 入口。

## 2. 写入统一执行

全部标准新增、修改和删除先规范化为一个 `SuperCudReq`，再跨 Bean 进入唯一的事务与生命周期边界：

```text
create/createBatch/createIfAbsent/update/updateBatch/delete/deleteBatch/superCud
  -> 构造 SuperCudReq
  -> ILocalCrudService.superCud
  -> CrudWriteExecutor.superCud (@Transactional)
  -> 校验 + 写生命周期 + 关联协调器 + Repository
```

`SuperCudReq` 由 `addEntities/addByFields/updateEntities/updateCriteria/deleteIds/deleteCriteria` 组成。
`addByFields` 为空时普通新增，非空时所有新增实体按同一组 Java 属性判断不存在后新增；`updateCriteria`
为空时按实体 ID 修改，非空时使用公共 Criteria，并把其中的 `matchFields` 从每个当前实体提取为附加等值
条件。Java Builder 可以用 getter Lambda 设置 `addByFields` 和 `matchFields`，传输时仍统一为属性名字符串。
各专项入口只构造对应请求，因此事务、校验、关联写入、审计和业务生命周期只需围绕 `SuperCudReq` 实现
一次。

框架只校验属性合法、请求非空元素，以及修改和删除能够形成有效 WHERE；不会检查匹配字段是否对应唯一
索引，也不会阻止多个更新实体命中重叠范围。业务应根据并发与覆盖需求自行设计唯一索引和条件。结果仅含
`addedCount/updatedCount/deletedCount` 三项汇总数量，不返回逐项实体数据。

业务监听器实现 `CrudWriteLifecycleListener` 并注册为 Spring Bean。框架按 Spring `Ordered` 顺序自动收集，
无需静态 Registry、构造器注册或手工注销。`CrudWriteLifecycleContext` 提供：

- `service()`：当前本地 `ILocalCrudService`；
- `entityClass()`：当前实体类型；
- `request()`：完整、强类型的 `SuperCudReq`；
- `result()`：执行成功后的 `SuperCudResult`；
- `failure()`：执行器内部失败时捕获的异常；
- `attributes()`：同一次执行全部回调共享的临时数据。

回调时机：

| 回调 | 时机 | 适合用途 |
| --- | --- | --- |
| `beforeExecute` | 完整请求校验后、写数据库前，事务内 | 业务校验、补齐执行上下文 |
| `afterExecute` | Repository 与关联写入完成后、提交前，事务内 | 必须与主写入原子提交的数据库同步 |
| `afterCommit` | 外层真实事务提交后 | 缓存失效、刷新运行时配置、发送外部通知 |
| `afterRollback` | 外层事务回滚后 | 清理临时状态、回滚观测 |

`afterCommit` 不得承担数据库一致性写入；此时事务已经提交，其异常只记录日志，不能反向改变已提交结果。
若执行器位于更外层事务中，提交/回滚回调跟随最外层实际事务结果，不会在 `superCud` 方法返回时提前执行。

## 3. 查询统一执行

四个查询正式入口都规范化为 `CrudQueryReq`：

```text
page/cursorPage/count/exists
  -> CrudQueryReq(queryType, criteria, cursorId, pageSize)
  -> CrudQueryExecutor.query
  -> 查询生命周期 + Repository + 关联查询协调器
  -> CrudQueryResult
```

`CrudQueryExecutor` 会复制调用方 Criteria，防止分页、游标和监听器处理污染原对象。`CrudQueryType` 固定为
`PAGE`、`CURSOR_PAGE`、`COUNT`、`EXISTS`；生命周期替换查询结果时，结果类型必须与请求类型一致。

业务监听器实现 `CrudQueryLifecycleListener` 并注册为 Spring Bean，可使用以下回调：

- `beforeQuery`：Repository 查询前，可实施数据范围、默认过滤条件或查询观测；
- `afterQuery`：查询和关联装配后，可规范化结果；
- `onFailure`：查询或查询生命周期失败时执行。

`CrudQueryLifecycleContext` 提供当前服务、实体类型、强类型 `CrudQueryReq`、`CrudQueryResult` 和共享属性。

## 4. 嵌套调用规则

执行器只抑制“同一个 Service 实例”的生命周期重入，防止监听器重新调用本服务造成无限递归；底层操作仍会
执行。一个业务服务调用另一个 Service 时，后者仍拥有完整、独立的生命周期。这能保留跨聚合应用编排，
同时避免使用全局 ThreadLocal 粗暴跳过所有嵌套服务。

监听器一般不应重新调用当前服务。若只是规范化本次写入，应直接处理同一个 `SuperCudReq`；若需要协调
其他聚合，应调用对应的应用服务或另一个 `ILocalCrudService`，并明确事务边界。
