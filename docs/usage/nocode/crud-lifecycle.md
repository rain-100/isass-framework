# NoCode 写入生命周期

所有标准新增、修改和删除最终都构造 `SuperCudReq`，并跨 Bean 进入
`CrudChangeExecutor.superCud`。该方法是唯一标准写事务和生命周期边界，`CrudOperation` 只有
`SUPER_CUD`，不存在旧 `batchSave` 或各专项方法的重复回调。

```text
create/createBatch/update/updateBatch/delete/deleteBatch/createIfAbsent
  -> 构造 SuperCudReq
  -> ILocalCrudService.superCud
  -> CrudChangeExecutor.superCud (@Transactional)
  -> CrudLifecycleRegistry
  -> Repository + 关联协调器
```

监听器实现 `CrudLifecycleListener` 后向 `CrudLifecycleRegistry` 注册。回调上下文提供：

- `service()`：当前 `ILocalCrudService`；
- `entityClass()`：当前实体类型；
- `operation()`：固定为 `SUPER_CUD`；
- `arguments()`：包含完整 `SuperCudReq`；
- `result()`：成功后为 `SuperCudResult`；
- `attributes()`：同一回调链共享的临时数据。

监听器不得重新调用同一服务的标准写方法制造嵌套变更；需要扩充写入时，应在执行前规范化同一个
`SuperCudReq`，或由明确的应用服务编排。
