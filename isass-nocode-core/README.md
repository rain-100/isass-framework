# isass-nocode-core

标准 CRUD 与统一执行。提供 createBatch/update/delete/superCud、page/cursorPage/count/exists，以及可选树查询。

- 写入经 CrudWriteExecutor 事务与生命周期；查询经 CrudQueryExecutor。关联只按声明处理，MERGE 保留旧项、REPLACE 移除未提交项。
- HTTP：/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}。参数用 Java camelCase；loadRelated 展开关联，returnFields 控制读取投影。
- 生命周期承接业务约束；数据库一致性写入放在事务内，缓存和通知放在 afterCommit。直接 Repository/自定义 SQL 需自行接入授权。

[返回模块目录](../README.md)
