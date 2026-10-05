# NoCode CRUD 统一执行与生命周期

## 基础类型与模块边界

`IEntity`、实体特征接口、`EntityAssociation` 位于 `isass-core-common` 的
`vip.isass.framework.common.entity`；Criteria 条件模型位于 `common.criteria`，
属性 Getter 位于 `common.property`。`IRepository` 位于
`isass-database-core` 的 `vip.isass.framework.database.core.repository`，表前缀注册类
`TablePrefixUtil` 位于 `vip.isass.framework.database.core`。生成器模板和当前生成代码
均使用这些包名。`isass-nocode-core` 持有
`ICrudService`、查询/写入执行器和 Entrypoint 参数适配，依赖数据库契约；
`isass-database-mybatisplus` 只依赖 core-common 与 database-core，不依赖 NoCode CRUD。
其表元数据与 Repository 适配实现使用 `vip.isass.framework.database.mybatisplus`
及 `vip.isass.framework.database.mybatisplus.orm` 包，不放在 `framework.nocode` 下。
表元数据只扫描 `IRepository` 实现 Bean 的泛型来解析实体；CRUD Service 不参与注册。
使用自定义本地 CRUD Service 时，也应为对应实体提供 Repository 实现 Bean。

高级响应字段由 `isass-core-common` 的 `AdvancedFeatureProjector` 根据 `AdvancedFeature`
生成，实体不再继承 `IAnyJsonEntity`，也不保存响应专用字段。Web 层解析
`dateFormat.<field>`、`decimalPlaces.<field>`、`dictTranslation.<field>` 请求参数后，
只为实际序列化的实体字段附加 `<field>Text`；字典翻译由可选的
`IDictTranslationProvider` Bean 提供。非 Web 调用可显式构造 Projector 并传入
实体、已序列化字段名和 Feature，不使用静态全局 Provider。

## 1. 边界与地址

`ICrudService<E, C, PK>` 是围绕单个聚合提供标准 CRUD 的应用入口，并继承 `IEntrypoint`。生成的本地实现统一命名为 `${Entity}Service`。业务首先复用 NoCode 的八个基础 CRUD 入口及实体具备的可选通用能力；只要需求能由标准 CRUD、Criteria、关联、能力接口和生命周期完整表达，就不得新增同义的查询、新增、修改或删除 Entrypoint。生命周期可以协调同一限界上下文内其他聚合或领域。只有无法用这些机制表达的独立业务用例才增加手写 Entrypoint，并显式声明 `@EntrypointOperation`。

标准地址固定为：

```text
/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}
```

原则上不使用业务 Path 参数；条件、ID 和分页参数使用 Query，实体和变更集使用 Body。业务分页统一返回 ORM 无关的 `vip.isass.framework.common.page.Page<E>`，MyBatis-Plus Repository 在基础设施边界完成分页转换。

只有 `@EntrypointOperation` 标注的方法生成 HTTP/gRPC 路由和 OpenAPI。未标注的默认方法只提供 Java/远程代理调用便利。

## 2. 正式入口与可选能力

`ICrudService` 固定发布八个 HTTP/gRPC 基础入口：

| 类型 | 正式入口 | 统一执行边界 |
| --- | --- | --- |
| 写入 | `createBatch`、`update`、`delete`、`superCud` | `CrudWriteExecutor.superCud` |
| 查询 | `page`、`cursorPage`、`count`、`exists` | `CrudQueryExecutor.query` |

通用能力使用原子接口按实体特征组合，不把不适用的方法塞入 `ICrudService`。当前层级实体可额外实现
`ITreeQueryService`，发布 `tree` 和 `descendantIds` 查询入口；本地实现组合 `ILocalTreeQueryService`。后续通用能力也应使用
独立能力接口，禁止建立 `ITreeExportCrudService` 一类组合接口。

`create(E)`、`createIfAbsent(...)`、`update(E)`、`update(E,C)`、`delete(PK)`、`getById`、`getOne`、
`existsById`、`requireOne`、`list` 等方法仅为 Java 便捷方法，不标注 `@EntrypointOperation`，也不产生独立
HTTP、gRPC 或 OpenAPI 入口。正式入口是同名重载 `update(Collection<E>,C)` 与 `delete(C)`。

`list` 通过 `page` 实现且最多返回 `9999` 条；更多数据必须使用 `page`、`cursorPage` 或异步导出。

## 3. 写入统一执行

全部标准新增、修改和删除先规范化为一个 `SuperCudReq`，再跨 Bean 进入唯一的事务与生命周期边界：

```text
create(E) / createBatch(Collection<E>) / createIfAbsent(...) /
update(E) / update(E,C) / update(Collection<E>,C) / delete(PK) / delete(C) / superCud(...)
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

`delete` 必须拒绝空 Criteria 或没有有效 WHERE 条件的 Criteria，不提供无保护的远程全表修改或删除。

实体表达“更新成什么数据”，Criteria 表达“更新哪些记录以及如何更新”。`update(entity)` 根据实体 ID 构造 Criteria；显式传入 Criteria 时以最终 Criteria 为范围，实体 ID 不自动进入 SET 或叠加 WHERE。只有提交关联属性时才要求最终 Criteria 明确限定唯一主实体 ID，且实体 ID 如有值须与其一致。`IUpdateCriteria.updateMode` 控制直接关联的 `MERGE/REPLACE`；`IGNORE_NULL` 忽略 null，`WRITE_NULL` 为参与 SET 的实体写入全部非框架维护可更新字段（包括 null），框架维护字段仍由框架维护。只有关联属性被提交时，主实体不参与 SET。

## 4. 查询统一执行

四个基础查询及可选的树查询都规范化为 `CrudQueryReq`：

```text
page/cursorPage/count/exists/tree/descendantIds
  -> CrudQueryReq(queryType, criteria, cursorId, pageSize)
  -> CrudQueryExecutor.query
  -> 查询生命周期 + Repository + 关联查询协调器
  -> CrudQueryResult
```

`CrudQueryExecutor` 直接使用调用方 Criteria，不做深复制；仅入参为空时新建 Criteria。
生命周期、关联补键、默认排序和游标条件等修改保留在同一个对象中，表示本次实际执行的查询条件。
查询失败也不回滚已完成的内存修改；需要保留初始条件或并发执行时，由调用方显式传入 `criteria.copy()`。
本地 Java 调用可观察这些修改；HTTP/gRPC 服务端修改的是反序列化后的对象，不自动回传客户端 Criteria。
loadRelated 的批次 IN 和内部分页属于临时调度状态，结束或异常时清理/恢复；业务筛选、补键等修改仍保留，详见关联查询文档。
`CrudQueryType` 包含
`PAGE`、`CURSOR_PAGE`、`COUNT`、`EXISTS`、`TREE`；生命周期替换查询结果时，结果类型必须与请求类型一致。
`CrudQueryResult.records()` 统一返回本次查询涉及的全部实体，树结果按先序展开，结果脱敏等监听器不应只处理
`page` 与 `cursorPage` 两种容器。

业务监听器实现 `CrudQueryLifecycleListener` 并注册为 Spring Bean，可使用以下回调：

- `beforeQuery`：Repository 查询前，可实施数据范围、默认过滤条件或查询观测；
- `afterQuery`：查询和关联装配后，可规范化结果；
- `onFailure`：查询或查询生命周期失败时执行。

`CrudQueryLifecycleContext` 提供当前服务、实体类型、强类型 `CrudQueryReq`、`CrudQueryResult` 和共享属性。

### 存在性查询

`exists(criteria)` 在 MyBatis-Plus 适配层使用第一页、每页一条且关闭总数统计的 ORM 查询，
不再通过 `COUNT > 0` 判断存在性。实现 `IIdEntity` 的实体仅查询主键，并忽略存在性查询不需要的排序；
该 ORM 优化本身不改变传入 Criteria 的筛选、排序和投影；此前生命周期对 Criteria 的修改仍保留。权限与查询生命周期沿用原链路，逻辑删除仍由 ORM 处理。
Repository 仅在 Criteria 实现 `IOrderByCriteria`、需要移除排序时复制对象；不支持排序的 Criteria 直接用于构造查询。
`existsById`、按属性存在性检查及内部 Wrapper 便捷方法同样限一条、不统计总数，分页语法由方言生成。
无匹配记录时仍可能扫描较多数据，需为高频筛选条件配置合适索引。

### 游标分页

- 只允许 `orderBy=id asc` 或 `orderBy=id desc`，默认 `id asc`；禁止其他字段、多字段或缺少方向的排序。
- 第一页 `cursorId` 可为空，后续使用上一页 `nextCursorId`；实现多取一条计算 `hasMore`，不执行 `count(*)`。
- NoCode 授权上下文保留可选参数的 `null` 值和原始位置；空游标或缺省 `pageSize` 不会跳过权限检查。
- 连续翻页必须保持基础筛选和排序方向不变；ID 必须稳定、唯一、可比较且写入后不变化。
- 游标执行直接更新本次 Criteria：追加 ID 边界、规范化排序、设置 pageNum=1、pageSize=请求大小+1、searchCountFlag=false。
  从初始条件重新查询或需要每页独立条件时，保存基础 Criteria 并逐次传入 `baseCriteria.copy()`，避免复用已有游标边界。
- 高频附加过滤条件应建立与查询匹配的联合索引，否则游标分页只能消除 offset 成本，不能消除过滤扫描成本。

### 树查询

- 只有同时实现 `IIdEntity`、`IParentIdEntity` 的实体服务才组合 `ITreeQueryService`；生成器根据 `parent_id`
  自动选择能力接口，普通实体不暴露 `tree`。
- `tree(criteria)` 先查询符合 Criteria 的全部实体，再按 `id/parentId` 在内存中组装完整森林，不应用分页参数；
  `orderBy` 同时决定根节点与同级子节点顺序，未指定时使用 `id asc`。
- `id` 与 `parentId` 是树装配必需字段。调用方使用 `returnFields` 时，执行器自动补充这两个 Java 属性。
- `parentId` 为 `null`、`0`，或父节点不在当前过滤结果中时，节点作为当前结果森林的根；因此 Criteria 可以只
  返回一个局部结果集。查询会拒绝空 ID、重复 ID 和父子循环，并在每次装配前清空 `parent`、重建 `children`，
  避免序列化时形成双向循环。
- `children` 与 `parent` 由树查询负责，不得同时通过 `loadRelated` 请求；其他显式关联仍对全部树节点
  批量装载。
- `descendantIds(rootId, criteria)` 复用同一次 `tree(criteria)` 查询和生命周期，在过滤后的森林中按广度优先顺序
  返回指定节点的全部后代 ID，不包含节点自身；`rootId` 不在结果森林中时明确失败。该派生查询不增加新的
  `CrudQueryType`，其排序、数据范围、缺失/重复 ID 和循环校验语义与 `tree` 一致。

## 5. 关联写入与查询

直接关联的请求出现性、`MERGE/REPLACE`、关系键重写和批量关联查询规则见
[NoCode 关联查询](association-query.md)。关联键由服务端依据当前实体和 DDL 关系元数据写入，不能信任客户端提交值；更新已有目标前必须校验目标存在、归属和写权限。

## 6. 嵌套调用规则

执行器只抑制“同一个 Service 实例”的生命周期重入，防止监听器重新调用本服务造成无限递归；底层操作仍会
执行。一个业务服务调用另一个 Service 时，后者仍拥有完整、独立的生命周期。这能保留跨聚合应用编排，
同时避免使用全局 ThreadLocal 粗暴跳过所有嵌套服务。

监听器一般不应重新调用当前服务。若只是规范化本次写入，应直接处理同一个 `SuperCudReq`；若需要协调
其他聚合，应调用对应的应用服务或另一个 `ILocalCrudService`，并明确事务边界。
