# isass-nocode-core

标准 CRUD 与统一执行。提供 createBatch/update/delete/superCud、page/cursorPage/count/exists，以及可选树查询。

- 写入经 CrudWriteExecutor 事务与生命周期；查询经 CrudQueryExecutor。关联只按声明处理，MERGE 保留旧项、REPLACE 移除未提交项。
- 关联可递归保存；外键目标在依赖方生命周期前保存并回填真实 ID。嵌套增改删分别经过功能授权与生命周期，ID-only 引用不重复修改。REPLACE/级联先解析实际子记录 ID，再按 ID 删除；根关联更新加事务记录锁，失败整批回滚。
- EntityAssociation 的 readOnly() 禁止关系写入；where(field,value) 为关联读取、写入归属和删除增加不可覆盖的固定条件（按目标 Java 属性类型转换）。共享目标不声明 cascadeDelete，不随绑定移除而删除。
- ICrudService/ILocalCrudService 的 E 仅要求 IEntity，Criteria 不强制 IIdCriteria。无主键实体支持新增、普通查询/分页/统计，以及带有效条件或 matchFields 的修改、带有效条件的删除；不允许无条件写入。
- ID 是可选能力：getById/existsById、按 ID 修改/删除、updateAllColumns、deleteIds、ID 游标/树查询在运行时要求 IIdEntity。不支持的操作在数据库写入前明确拒绝；无主键条件删除直接执行 Criteria。loadRelated 目标去重/分页、嵌套关联写入及级联根删除仍依赖 ID，不以猜测主键代替能力校验。
- SuperCudReq<E, C, PK> 不限制 E 必须有主键；deleteIds 为 List<PK>，入口解析泛型后完成反序列化，不由业务生命周期转换 ID。无主键服务可用 Serializable 占位，不提交 deleteIds。JSON 结构不变，Long 主键可用字符串精确传输。
- HTTP：/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}。参数用 Java camelCase；loadRelated 展开关联，returnFields 控制读取投影。标量集合 Query 使用英文逗号分隔，按属性定义解析，不受 lambda/varargs 便捷重载影响。
- 生命周期承接业务约束；数据库一致性写入放在事务内，缓存和通知放在 afterCommit。直接 Repository/自定义 SQL 需自行接入授权。
- beforeQuery 使用 context.addReadRestriction 追加服务器构造的 AND 约束，不修改请求 Criteria；统一执行器按查询对象打开、恢复 TrustedReadContext，覆盖分页、游标、统计与树查询。不能把客户端条件搬入可信列表；固定租户边界及输出字段保护仍独立执行。
- 入口与嵌套写入的权限执行器在调用时解析；注册入口时不提前加载依赖入口目录的授权组件，避免启动循环依赖，授权异常仍中止调用。
- security 包的 DataReadProjection 处理关联读取的内部补键，配合 core-common 隐藏字段；读取和租户策略契约来自 isass-security-core。
- TenantReadLifecycleListener 为读取建立固定隔离：ITenantEntity 限制当前 tenantId，无租户时拒绝租户数据；TenantReadPolicyProvider 可补关联边界及可信平台豁免。仅内部主体的后台调用保留内部访问语义，带业务主体的 HMAC 调用仍受隔离；此机制不代替写入校验。

[返回模块目录](../README.md)
