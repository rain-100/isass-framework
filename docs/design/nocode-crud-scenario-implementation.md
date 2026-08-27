# NoCode 级联、关联与树形 CRUD 设计

## 文档状态

- 状态：第一阶段已实施（破坏式重构，不兼容旧 CRUD 类型与路由）
- 首次记录：2026-08-12
- 最近整理：2026-08-13
- 上位设计：[自动服务入口与 NoCode 边界设计](automatic-service-entry-and-nocode-boundary.md)

本文只描述 NoCode CRUD 中的关联、级联、树形、批量更新和分页语义。Entrypoint 注解、URL、传输选择、
OpenAPI 和 Security 由上位设计统一定义，不在本文重复。

## 1. 范围与原则

NoCode 提供以下通用能力：

1. 当前实体的 DDL 可以声明到目标实体的单体或列表关系；
2. 查询当前实体时，前端可以显式选择需要展开的关系；
3. 新增或修改当前实体时，可以在一个请求和同一个本地事务中保存直接关联对象；
4. 删除当前实体时，可以按 DDL 固定规则沿已声明方向级联删除；
5. `parent_id` 树形实体统一提供父对象、子节点列表及可选的向下级联删除；
6. 普通查询以 `page` 为正式入口，Java 便捷方法复用 `page`，深分页使用基于 ID 的 `cursorPage`。

核心原则：

- 框架不识别业务术语中的“主表”“子表”“中间表”或“资源表”；
- DDL 所在表对应“当前实体”，标记中命名的类型对应“目标实体”；
- 每条关系都是有方向的，只影响当前实体生成的属性和当前方向的 CRUD；
- 反向关系必须在目标实体自己的 DDL 中另行声明；
- 结构和删除生命周期由 DDL 固定，请求不能修改；
- 查询展开及更新合并方式可以由请求决定；
- 不使用数据库外键；
- 通用聚合写入只支持同一微服务、同一数据源的本地事务；
- 第一阶段只处理请求中的直接关联，不递归保存任意深度嵌套对象图。

## 2. DDL 关系元数据

### 2.1 普通方向性关系

普通关系标记只允许写在表级注释中：

```text
[关联表-单体-目标实体; 可选参数]
[关联表-列表-目标实体; 可选参数]
```

例如 `SampleGroup` 声明：

```text
[关联表-列表-SampleImage;
 cascadeDelete=true]
```

等价于：

```text
[关联表-列表-SampleImage;
 property=sampleImages;
 localKey=id;
 targetKey=sampleGroupId;
 cascadeDelete=true]
```

它只在 `SampleGroup` 生成非持久化属性：

```java
private Collection<SampleImage> sampleImages;
```

不会在 `SampleImage` 中自动生成反向属性。如果需要反向查询或保存，必须在 `SampleImage` 的表级注释中
另行声明：

```text
[关联表-单体-SampleGroup]
```

其推断结果为：

```text
[关联表-单体-SampleGroup;
 property=sampleGroup;
 localKey=sampleGroupId;
 targetKey=id;
 cascadeDelete=false]
```

两条声明彼此独立，不自动成对生成，也没有 `reverseProperty` 参数。

### 2.2 参数与推断

| 参数 | 含义 | 默认或推断规则 |
| --- | --- | --- |
| 目标实体 | 公共表名前缀移除后的 Java 实体名 | 必填，例如 `SampleImage` |
| `property` | 生成到当前实体的非持久化属性 | 单体为目标类型 lowerCamel；列表按统一复数规则生成 |
| `localKey` | 当前实体参与关联的属性 | 优先按类型和 `XxxId` 约定推断 |
| `targetKey` | 目标实体参与关联的属性 | 与 `localKey` 配对推断 |
| `cascadeDelete` | 删除当前实体时是否删除该方向的目标记录 | 默认 `false`，永不根据名称推断 |

关联键推断依次检查：

1. 当前实体是否存在 `目标实体名 + Id`，例如 `SampleImage.sampleGroupId -> SampleGroup.id`；
2. 目标实体是否存在 `当前实体名 + Id`，例如 `SampleGroup.id -> SampleImage.sampleGroupId`；
3. 只有一个候选时采用；没有候选或存在歧义时要求在 DDL 显式填写 `localKey/targetKey`；
4. 两侧属性类型必须兼容，目标字段必须能够通过目标 Criteria 查询；
5. 生成器启动前校验属性存在性、类型、重复关系名和关系方向。

`property` 的复数化必须由生成器中的统一命名器完成；无法可靠复数化时要求显式配置。DDL 备注解析在
生成器 Java 代码中完成，Freemarker 模板只消费结构化关系元数据。

### 2.3 方向与级联

`SampleGroup -> SampleGroupTag` 和 `SampleGroupTag -> Tag` 是两条独立关系：

- 删除 `SampleGroup` 是否删除 `SampleGroupTag`，只看 `SampleGroup` DDL 中该关系的
  `cascadeDelete`；
- 删除 `SampleGroupTag` 是否删除 `Tag`，只看 `SampleGroupTag` DDL 中该关系的
  `cascadeDelete`；
- 框架不因为目标看起来像中间表或资源表而改变行为；
- 删除目标实体绝不沿未声明的反向方向删除当前实体。

`cascadeDelete=true` 是强生命周期声明，应谨慎用于共享资源。数据库仍不创建外键或数据库级联约束。

## 3. 树形实体

### 3.1 统一模型

`parent_id` 固定表示一条记录最多一个父节点、一个父节点可以有多个直接子节点。树形实体实现：

```java
public interface IParentIdEntity<PK extends Serializable,
        E extends IParentIdEntity<PK, E>> extends IPkEntity<PK, E> {

    PK getParentId();

    void setParentId(PK parentId);

    E getParent();

    void setParent(E parent);

    List<E> getChildren();

    void setChildren(List<E> children);
}
```

`parentId` 是持久化字段，`parent` 和 `children` 是固定的非持久化关联属性，无需
`[树结构-添加父节点]` 或 `[树结构-添加子节点]` 标记：

```text
parent   : parentId -> id
children : id -> parentId
```

如果自关联不是父子树，不实现 `IParentIdEntity`，改用普通方向性关系。

### 3.2 删除子孙节点

删除当前节点时是否递归删除全部子孙节点，由表级注释决定：

```text
[树结构-cascadeDelete=true]
```

规则：

- 未声明时默认为 `false`；
- `true` 时先删除全部子孙节点，再删除当前节点；
- 只允许向下删除，删除子节点永远不能删除父节点；
- 必须检测循环和异常深度，发现脏数据形成环时终止并回滚；
- 实现应按层或批次查询和删除，避免每个节点执行一次 SQL；
- 删除行为使用实体标准删除语义，保留逻辑删除、审计和 CRUD 生命周期处理。

新增和修改 `parentId` 时必须阻止把节点设为自己的父节点或自己的子孙节点。

## 4. 方向性关联查询

### 4.1 请求参数

默认查询不展开任何关联。前端使用重复 Query 参数选择关系：

```text
association.query=sampleImages&association.query=sampleGroup
```

关系自己的 Criteria 使用属性命名空间：

```text
association.sampleImages.criteria.status=ENABLED
```

树形实体使用固定属性名：

```text
association.query=children
association.query=parent
```

`association.query` 遵循 Entrypoint 的集合 Query 规则，使用同名重复参数，不使用逗号字符串或 `[]` 后缀。

### 4.2 加载规则

- 只能展开当前实体元数据已经声明的属性；未知属性返回参数错误；
- 当前实体和目标实体通过 `localKey/targetKey` 批量加载，禁止 N+1 SQL；
- 每个关联 Criteria 与框架的数据权限条件共同生效；
- 单体关联不能携带分页参数；列表关联可以按受控规则过滤；
- 默认只展开一层，不自动继续展开目标实体的其他关系；
- 后续若允许显式多层展开，必须记录已访问关系路径并设置最大深度，防止双方关系造成循环；
- 序列化器不能因为双方都拥有属性而自动递归输出完整对象图。

反向查询只有在目标实体自己的 DDL 已声明对应属性时才可使用。

## 5. 一个请求保存当前实体及直接关联

### 5.1 请求属性存在性

当前实体的 Java 便捷方法 `create`、正式入口 `createBatch`/`superCud` 和 `update` 都可以接收生成实体
及其非持久化关联属性。是否处理某条关系，以原始请求是否携带该属性为准：

| 请求状态 | 行为 |
| --- | --- |
| 属性未出现 | 不查询、不新增、不修改、不删除该关系 |
| 属性为 `null` | 不处理该关系 |
| 属性包含对象 | 有 ID 更新，无 ID 新增 |
| 属性为空列表 | `MERGE` 不处理；`REPLACE` 清空该方向已有目标记录 |

不能把属性存在性实现成对“原始 JSON”的依赖，因为同一服务还支持 gRPC 和本地 Java 调用。Entrypoint
绑定层必须把各协议的字段出现信息规范化为传输无关的写入属性掩码，并与每个实体一起传给
`CrudWriteExecutor`：HTTP 根据 JSON 字段名生成，gRPC 根据字段 presence 生成，本地 Java 调用由生成的
实体 setter 跟踪或显式写入构造器生成。属性掩码属于单次调用的瞬态元数据，不持久化，也不作为业务响应
字段序列化。无法可靠取得属性掩码时，涉及 `WRITE_NULL`、空集合或关联属性的写入必须拒绝，不能猜测。

目标对象提交的关联键不作为权威值，服务端根据当前实体和关系元数据重新赋值，防止越权转移关系。

只保存当前实体直接声明的关系。目标对象内部即使还携带其他关联属性，第一阶段也不递归保存；复杂对象图
使用显式应用服务编排。

### 5.2 写入顺序

写入顺序取决于关联键方向：

- `当前实体.id -> 目标实体.currentId`：先保存当前实体，再把当前实体 ID 写入目标关联字段并保存目标；
- `当前实体.targetId -> 目标实体.id`：无 ID 目标先保存并取得 ID，再把目标 ID 写入当前实体后保存当前实体；
- 更新有 ID 目标前，必须校验目标存在、写权限以及当前关系归属；
- 一个请求中的依赖顺序必须由结构化关系元数据计算，不能依赖客户端提交顺序。

当前实体和所有直接关联修改必须在同一数据源的一个本地事务提交。

### 5.3 `IUpdateCriteria`

更新范围和更新策略由独立 Criteria 能力表达，不污染最基础的 `ICriteria`：

```java
public interface IUpdateCriteria<
        E extends IEntity<E>,
        C extends IUpdateCriteria<E, C>> extends IWhereConditionCriteria<E, C> {

    UpdateMode getUpdateMode();

    C setUpdateMode(UpdateMode updateMode);

    NullValueMode getNullValueMode();

    C setNullValueMode(NullValueMode nullValueMode);

    @Transient
    default UpdateMode resolveUpdateMode() {
        return getUpdateMode() == null ? UpdateMode.MERGE : getUpdateMode();
    }

    @Transient
    default NullValueMode resolveNullValueMode() {
        return getNullValueMode() == null ? NullValueMode.IGNORE_NULL : getNullValueMode();
    }
}

public enum UpdateMode {
    MERGE,
    REPLACE
}

public enum NullValueMode {
    IGNORE_NULL,
    WRITE_NULL
}
```

`FullTypeCriteria` 实现 `IUpdateCriteria`，生成的 Criteria 自动获得可空的 `updateMode` 和
`nullValueMode`。只有执行更新时才解析默认值，避免对象 Query 序列化器主动发送默认策略。两个字段都不是
实体字段，不能进入 `whereConditions` 或映射成数据库列。

- `updateMode` 控制直接关联对象：默认为 `MERGE`，也可以选择 `REPLACE`；
- `nullValueMode` 控制当前实体普通字段：`IGNORE_NULL` 忽略请求中值为 `null` 的字段，`WRITE_NULL` 把请求
  中显式提交的 `null` 写入数据库；
- 无论采用哪种 `nullValueMode`，请求中完全没有出现的属性都不参与更新；服务端仍需保留属性存在性；
- `nullValueMode` 取代旧 `updateAllColumnsById` 的独立远程入口。

以后新增由请求动态决定的通用更新策略，也放入 `IUpdateCriteria`；分页、排序和响应字段选择不属于更新
策略。

### 5.4 新增

单体新增是未标注 `@EntrypointOperation` 的 Java 默认方法，不生成 HTTP/gRPC 路由；批量新增是带入口
注解的默认方法。两者形成本地固定委托链：

```text
create
  -> superCud(addEntities=[entity])
    -> CrudWriteExecutor.superCud
```

```java
default E create(E entity) {
    superCud(SuperCudReq.add(entity));
    return entity;
}

@EntrypointOperation(
        operationName = "createBatch",
        displayName = "增-批量",
        description = "批量新增数据",
        displayOrder = 101,
        httpMethod = HttpMethod.POST)
default Long createBatch(@BodyParam Collection<E> entities) {
    return superCud(SuperCudReq.addAll(entities)).addedCount();
}
```

实际新增流程由 `CrudWriteExecutor` 执行：

1. 校验当前实体和请求携带的直接关联对象；
2. 根据关联键依赖确定目标先保存还是当前实体先保存；
3. 生成 ID 并写入服务端权威关联键；
4. 有 ID 的关联对象校验存在性、归属和写权限后更新，无 ID 的关联对象新增；
5. 在同一个本地事务提交；
6. 任一步失败，全部回滚。

`createBatch` 拒绝 `null`、空集合和空元素，返回新增数量。`create` 返回调用方传入并由持久化层回填 ID、
审计字段等内容的实体；`SuperCudResult` 不回传实体副本或逐项结果，避免大批量写入产生无用响应数据。

### 5.5 不存在时新增

`createIfAbsent` 保留为未标注入口注解的 Java 默认方法，用于微服务初始化内置参数、字典、默认配置等内部
幂等场景，不生成独立 HTTP/gRPC 路由：

```java
default boolean createIfAbsent(
        E entity,
        PropertyGetter<E, ?> firstField,
        PropertyGetter<E, ?>... remainingFields) {
    return superCud(SuperCudReq.<E, C>builder()
            .addEntity(entity)
            .addByFields(firstField, remainingFields)
            .build()).addedCount() == 1;
}
```

`addByFields` 是整个 `addEntities` 批次共用的一组 Java 属性名。执行器逐条从当前实体提取这些字段的值，
按等值条件判断：不存在则新增，已存在则跳过；返回的 `addedCount` 表示实际新增数量。Java Builder 支持
getter Lambda，HTTP/gRPC 传输仍使用字符串属性名。

业务应优先选择能唯一标识记录的字段，并根据并发强度在 Liquibase 中建立相应唯一索引。框架会校验属性
存在且可读取，但不在代码层强制这些字段必须匹配唯一索引，也不禁止业务在明确接受语义的情况下使用非唯一
字段。并发竞争下，数据库唯一约束仍是防止重复数据的最终保证；插入遇到唯一冲突时，执行器仅在相同条件
已经存在时把它视为“未新增”，否则继续抛出真实数据库异常。

### 5.6 批量修改

`update` 是正式 Criteria 更新入口。单实体按 ID 修改作为未发布的 Java 默认方法，通过
`SuperCudReq.updateEntities` 调用 `superCud`；带 Criteria 的更新使用批次共用的
`SuperCudReq.updateCriteria`：

```java
@EntrypointOperation(
        operationName = "update",
        displayName = "改-批量",
        description = "根据实体 ID 或更新条件批量修改数据",
        displayOrder = 201,
        httpMethod = HttpMethod.PUT)
default Long update(
        @BodyParam Collection<E> entities,
        @QueryParam C criteria) {
    return superCud(SuperCudReq.updateByCriteria(entities, criteria))
            .updatedCount();
}

default boolean update(E entity) {
    return superCud(SuperCudReq.update(entity)).updatedCount() > 0;
}

default long update(E entity, C criteria) {
    return update(List.of(entity), criteria);
}

default long updateAllColumns(E entity) {
    entity.markAllPresentProperties();
    return update(entity, newCriteria().setNullValueMode(NullValueMode.WRITE_NULL));
}

default long requireUpdate(E entity, C criteria) {
    long count = update(entity, criteria);
    if (count == 0) {
        throw new AbsentException("没有符合更新条件的记录");
    }
    return count;
}
```

正式请求的 Body 始终是集合，返回值表示受影响的当前实体记录数。一个 `SuperCudReq` 只有一组
`updateEntities` 和一个批次共用的 `updateCriteria`。

当前实体定位规则：

- `updateCriteria` 为空时，每个实体必须有 ID，并按各自 ID 更新；
- `updateCriteria` 含普通范围条件时，该条件作为整个批次共用的更新范围；例如 `createTimeLt` 可以表达
  `create_time < ?`；
- `IUpdateCriteria.matchFields` 声明需要从每个实体读取并附加到公共 Criteria 的等值字段。例如
  `matchFields=["code"]` 时，每个实体分别执行 `公共 Criteria AND code = 当前实体.code`；
- Criteria 没有任何有效普通条件和 `matchFields` 时，执行器回退到实体 ID，防止意外全表更新；
- 多个实体命中相同或重叠范围时按输入顺序执行。是否允许覆盖、是否应建立唯一索引，由业务负责，框架不做
  额外限制；
- 属性名必须存在且可读取，空 Criteria、无 ID 且不能形成有效 Where 的请求必须拒绝；
- 数据权限、关联归属、乐观锁和事务回滚规则仍由统一执行器保证。

关联策略：

- `MERGE`：更新已提交的有 ID 对象，新增无 ID 对象，保留未提交的旧目标记录；空集合不处理；
- `REPLACE`：请求集合代表该方向最终结果；删除当前实体下未出现在请求 ID 集合中的旧目标记录，再更新
  有 ID 对象并新增无 ID 对象；空集合表示清空；
- 属性未提交或为 `null` 时，两种模式都不处理；
- `REPLACE` 删除前必须先校验全部已提交 ID 的存在性、归属和写权限；
- 单体关系的 `REPLACE` 使用同一语义：新对象成为唯一目标，旧目标按该方向的替换规则移除；共享资源关系
  不应由通用 `REPLACE` 管理其生命周期，应使用显式应用服务。

列表关系的差集条件等价于：

```sql
DELETE FROM target_table
WHERE relation_key = :currentKey
  AND id NOT IN (:submittedIds)
```

提交 ID 为空时省略 `NOT IN`。实现必须使用 Criteria 和参数化批量操作，不能拼接客户端 SQL，也不能绕过
目标实体的逻辑删除、数据权限或 CRUD 生命周期。

### 5.7 超级增删改

旧 `batchSave(BatchSave<E>)` 升级并重命名为超级增删改正式入口 `superCud`：

```java
@EntrypointOperation(
        operationName = "superCud",
        displayName = "超级增删改",
        description = "在一个事务中执行普通新增、条件新增、按 ID 或 Criteria 修改、按 ID 或 Criteria 删除",
        displayOrder = 202,
        httpMethod = HttpMethod.POST)
SuperCudResult superCud(@BodyParam SuperCudReq<E, C> req);
```

`SuperCudReq` 是请求实体 Record。它用批次级 `addByFields` 描述条件新增，用批次级 `updateCriteria`
描述普通范围和逐实体匹配字段，同时保留按 ID 删除与 Criteria 删除，从而让同一个请求任意组合全部标准
写能力。类型归属 `isass-nocode-core` 的
`vip.isass.framework.nocode.entity` 包，但不实现持久化 `IEntity`，不参与 ORM 映射：

```java
public record SuperCudReq<E, C>(
        List<E> addEntities,
        List<String> addByFields,
        List<E> updateEntities,
        C updateCriteria,
        List<Serializable> deleteIds,
        List<C> deleteCriteria
) {
    public SuperCudReq {
        addEntities = immutableList(addEntities);
        addByFields = normalizedFields(addByFields);
        updateEntities = immutableList(updateEntities);
        deleteIds = immutableList(deleteIds);
        deleteCriteria = immutableList(deleteCriteria);
    }
}

public record SuperCudResult(
        long addedCount,
        long updatedCount,
        long deletedCount
) {
}
```

`immutableList` 表示实现时使用统一工具把 `null` 规范化为空不可变列表，并对非空列表执行防御性复制。
`SuperCudReq` 提供静态工厂和 Builder。Java 内部通过 `PropertyGetter<E, ?>` getter Lambda 设置
`addByFields` 和 `updateCriteria.matchFields`，最终仍解析为稳定的 Java 属性名，便于远程序列化。

在线文档、表格编辑等协作场景由客户端先把本地操作日志归并成净变更集：新建后继续修改归入
`addEntities`，已有记录的多次修改归并到 `updateEntities`，新建后又删除从请求移除，已有记录删除归入
`deleteIds`。如果业务使用条件新增、条件修改或条件删除，则分别设置 `addByFields`、`updateCriteria` 和
`deleteCriteria`。归并后的操作可以在同一个请求中同时出现，不要求客户端按新增、修改、删除拆成多次调用。

例如一次协作保存可以提交如下结构；未使用的分组仍显式按空数组处理：

```json
{
  "addEntities": [
    {"title": "新增记录"}
  ],
  "addByFields": ["key"],
  "updateEntities": [
    {"id": 101, "title": "按 ID 修改"}
  ],
  "updateCriteria": {
    "createTimeLt": "2026-08-01T00:00:00",
    "matchFields": ["id"],
    "updateMode": "MERGE"
  },
  "deleteIds": [104],
  "deleteCriteria": [
    {"status": "DRAFT", "ownerId": 1001}
  ]
}
```

这些变更不是多次请求，而是一个 `superCud` 请求中的可组合操作，共享同一次授权检查、静态校验和本地
数据库事务。

处理规则：

1. `addEntities` 普通新增；非空 `addByFields` 使整个新增批次改为按实体字段值条件新增；
2. `updateEntities` 在 `updateCriteria` 为空时按 ID 更新；非空时按公共 Criteria 及其 `matchFields` 更新；
3. `deleteIds` 按 ID 删除；`deleteCriteria` 按 Criteria 删除；空 Criteria 或没有有效 Where 的删除必须拒绝；
4. 在写入前校验列表空元素、属性名、必要 Where、数据权限和关系归属；
5. 执行顺序固定为普通新增、条件新增、按 ID 更新、Criteria 更新、按 ID 删除、Criteria 删除；Criteria
   操作可以看到前序操作产生的数据，该顺序属于公开合同；
6. 同一已有 ID 不能同时出现在 `updateEntities` 和 `deleteIds`；客户端应在提交协作变更集前合并同一记录
   的多次编辑，并移除“新增后又删除”的净零变更；Criteria 与其他分组可能重叠时按固定顺序执行；
7. 所有操作在同一个本地事务中完成，任一真实失败全部回滚；“条件新增时记录已存在”是正常分支，不是
   失败；
8. 执行器直接使用 Repository 和关联写入能力，不能反向调用 `createBatch`、`update` 或
   `delete`，防止递归；
9. `SuperCudResult` 仅返回新增、修改、删除三类汇总影响数量，不回传实体或逐项结果；
10. 全部静态校验必须在任何写入前完成；执行期数据库异常仍导致整个请求回滚。
11. 所有操作为空时按合法的幂等 no-op 处理，不开启无意义的数据库写入，直接返回三个零值计数；
    这允许协作客户端在归并后没有净变更时安全提交保存。

不能把 `superCud` 改成客户端依次调用新增、修改和删除入口，否则会丢失单事务语义。

### 5.8 统一执行器与切面边界

如果 `createBatch` 的默认方法在同一 Bean 内调用 `this.superCud(...)`，Spring 代理不会再次拦截这个内部
调用。因此不能把事务、权限或生命周期只写成 `superCud` Service 方法上的 Spring AOP，然后期待所有
默认方法自动触发该切面。

确定增加两个独立执行器 Bean。写入统一进入 `CrudWriteExecutor`，查询统一进入 `CrudQueryExecutor`：

```java
public interface ILocalCrudService<E, C, PK> extends ICrudService<E, C, PK> {
    @Override
    default SuperCudResult superCud(SuperCudReq<E, C> req) {
        return CrudWriteExecutorProvider.getRequired().superCud(this, req);
    }

    @Override
    default Page<E> page(C criteria) {
        return CrudQueryExecutorProvider.getRequired()
                .query(this, CrudQueryReq.page(criteria)).page();
    }
}
```

```text
create(E) / createBatch(Collection<E>) / createIfAbsent(...) /
update(E) / update(E,C) / update(Collection<E>,C) / delete(PK) / delete(C)
  -> 构造 SuperCudReq
    -> superCud
      -> 独立 Spring Bean CrudWriteExecutor.superCud
        -> 事务、授权、校验、生命周期、关联、Repository、审计和事件
```

`CrudWriteExecutor` 是真正的统一事务和切面边界。需要 Spring AOP 的能力拦截该独立 Bean；更核心
的固定步骤直接由执行器模板编排。这样即使 Service 内部发生自调用，也一定会跨 Bean 进入执行器代理。

四个查询入口同样不能各自维护生命周期。`page/cursorPage/count/exists` 必须构造 `CrudQueryReq` 并进入
`CrudQueryExecutor.query`；`getById/getOne/requireOne/list` 等 Java 便捷方法继续复用正式查询入口。
写监听器实现 `CrudWriteLifecycleListener`，查询监听器实现 `CrudQueryLifecycleListener`，两者都由 Spring
按 `Ordered` 顺序自动收集，不提供静态注册表。写生命周期的事务阶段固定为 `beforeExecute`、
`afterExecute`、`afterCommit`、`afterRollback`；查询阶段固定为 `beforeQuery`、`afterQuery`、`onFailure`。
只抑制同一 Service 的生命周期重入，跨 Service 调用仍执行各自的完整生命周期。

客户端代理的规则也必须区分：带 `@EntrypointOperation` 的默认方法执行远程调用；只有未标注入口注解的
便捷默认方法才在客户端通过 `InvocationHandler.invokeDefault` 执行。

### 5.9 删除

单体删除是未标注入口注解的 Java 默认方法，Criteria 批量删除是带入口注解的默认方法：

```text
delete(id)
  -> superCud(deleteIds=[id])
    -> CrudWriteExecutor.superCud
```

```java
default boolean delete(PK id) {
    return superCud(SuperCudReq.delete(id)).deletedCount() == 1;
}

@EntrypointOperation(
        operationName = "delete",
        displayName = "删-批量",
        description = "根据查询条件批量删除数据",
        displayOrder = 401,
        httpMethod = HttpMethod.DELETE)
default Long delete(@QueryParam C criteria) {
    return superCud(SuperCudReq.deleteByCriteria(criteria))
            .deletedCount();
}
```

`delete` 拒绝没有任何有效 Where 条件的 Criteria。单 ID 删除直接构造 `SuperCudReq.delete(id)`。

删除当前实体时读取生成的方向性级联元数据：

- 普通关系 `cascadeDelete=true`：先删除该方向目标记录，再删除当前实体；
- 普通关系 `cascadeDelete=false`：只删除当前实体；业务若不允许遗留关系，应由显式业务规则拒绝删除；
- 树结构 `cascadeDelete=true`：先删除全部子孙节点，再删除当前节点；
- 不处理其他实体指向当前实体的反向关系；
- 不根据目标实体名称或用途继续推断下一层删除；
- 多条明确开启的级联关系按依赖顺序执行并处于同一事务。

跨微服务和跨数据源关系不进入通用级联，使用显式应用服务、消息、补偿和幂等机制处理。

## 6. 标准查询方法

### 6.1 `page` 是普通分页入口

```java
@EntrypointOperation(
        operationName = "page",
        displayName = "查-分页列表",
        description = "根据查询条件返回分页列表",
        displayOrder = 301,
        httpMethod = HttpMethod.GET)
Page<E> page(@QueryParam C criteria);
```

### 6.2 `getOne`、`list` 和 `requireOne` 是默认方法

它们不标注 `@EntrypointOperation`，不生成路由和 OpenAPI。远程代理在客户端执行默认方法，再调用正式的
`page` 入口：

泛型默认方法不能通过反射式 `criteriaClass()` 构造 Criteria，因此 `ICrudService` 声明不带入口注解的
`C newCriteria()`；生成的具体 `IXxxService` 提供类型安全的默认实现：

```java
@Override
default SampleGroupCriteria newCriteria() {
    return new SampleGroupCriteria();
}
```

生成 Criteria 必须提供 `id/idIn`、分页、更新和 `copy()` 能力。

```java
default E getOne(C criteria) {
    C copy = criteria.copy()
            .setPageNum(1L)
            .setPageSize(1L)
            .setSearchCountFlag(false);
    List<E> records = page(copy).getRecords();
    return records.isEmpty() ? null : records.getFirst();
}

default List<E> list(C criteria) {
    return page(criteria.copy()
            .setPageNum(1L)
            .setPageSize(9999L)
            .setSearchCountFlag(false))
            .getRecords();
}

default E requireOne(C criteria) {
    E entity = getOne(criteria);
    if (entity == null) {
        throw new AbsentException("实体不存在");
    }
    return entity;
}
```

Criteria 必须提供分页能力和 `copy()`，默认方法不能修改调用方对象。`list` 上限固定为 `9999`。

### 6.3 基于 ID 的游标分页

```java
@EntrypointOperation(
        operationName = "cursorPage",
        displayName = "查-游标分页",
        description = "按 ID 指定方向从游标之后查询下一页",
        displayOrder = 302,
        httpMethod = HttpMethod.GET)
CursorPage<E, PK> cursorPage(
        @QueryParam C criteria,
        @QueryParam("cursorId") PK cursorId,
        @QueryParam("pageSize") Long pageSize);
```

排序复用 Criteria 的 `orderBy`，只接受 `id asc` 或 `id desc`；未提供时默认 `id asc`。不接受其他字段、多
字段或缺少方向的表达式。

```sql
-- id asc
WHERE id > :cursorId AND 其他 Criteria 条件
ORDER BY id ASC
LIMIT :pageSize + 1

-- id desc
WHERE id < :cursorId AND 其他 Criteria 条件
ORDER BY id DESC
LIMIT :pageSize + 1
```

规则：

- 第一页允许 `cursorId` 为空；
- 后续请求使用上一页的 `nextCursorId`；
- 下一游标取当前页最后一条记录 ID；
- 多取一条计算 `hasMore`，不执行 `count(*)`；
- 连续翻页必须保持 Criteria 和排序方向不变；切换方向时清空游标；
- 非法 `orderBy` 返回参数错误，不能静默降级；
- ID 必须稳定、唯一、可比较且写入后不变化；
- SQL 由 Criteria 构建，不为游标分页单独维护 XML SQL。

```java
public record CursorPage<E, PK>(
        List<E> records,
        PK nextCursorId,
        boolean hasMore
) {
}
```

游标分页不提供事务快照保证；并发新增或修改可能影响后续页面可见内容，但稳定 ID 边界保证同一方向不会
因为 offset 漂移而重复读取已经越过的 ID。

### 6.4 联合索引

仅按 ID 查询时使用主键索引。高频附加条件原则上使用“等值字段在前、ID 在后”的联合索引：

```text
查询：tenant_id = ? AND status = ? AND id > ? ORDER BY id ASC
索引：(tenant_id, status, id)
```

同一索引可以正向和反向扫描，无需分别建立升序、降序索引。模糊条件或多个范围条件可能失去性能优势。
生成器不能为任意 Criteria 组合自动建索引，业务应根据真实查询在 Liquibase 中声明。第一阶段不因缺少
推荐索引拒绝请求，但 OpenAPI 和运维文档应提示性能条件。

## 7. 安全、事务与校验

- 当前实体和直接关联对象写入必须经过各自的数据权限校验；
- 客户端提交的关联 ID 不能把其他当前实体的目标记录转移过来；
- 关联键由服务端覆盖，不能信任客户端值；
- `REPLACE` 必须先完整校验再删除，避免校验中途留下部分结果；
- 批量请求任一步失败即整体回滚，不允许静默部分成功；
- 乐观锁、逻辑删除、审计字段和 CRUD 生命周期对级联目标同样生效；
- 属性未提交、`null` 和空集合必须有可测试的不同语义；
- 查询展开和保存都必须限制为已注册关系，禁止任意反射属性或客户端 SQL；
- 跨服务关系只能最终一致，不能伪装成本地事务。

## 8. 第一阶段实施结果

1. 解析表级 `[关联表-单体-*]`、`[关联表-列表-*]` 和可选参数；
2. 生成当前实体的单向非持久化属性及结构化查询、保存、删除元数据；
3. 让 `IParentIdEntity` 固定提供 `parentId/parent/children`；
4. 实现 `[树结构-cascadeDelete=true]` 的向下递归删除；
5. 实现一层方向性关联批量查询和循环防护；
6. 实现一个请求内当前实体与直接关联对象的事务保存；
7. 实现 HTTP、gRPC 和本地 Java 调用统一的瞬态写入属性掩码，保证未提交字段和显式空值能够双向区分；
8. 新增 `IUpdateCriteria`、`MERGE/REPLACE`、`NullValueMode` 和正式 `update`；
9. 将单体新增、不存在时新增和单体删除定义为未标注入口注解的 Java 默认方法；将批量新增、批量修改和
   批量删除定义为正式入口；
10. 将 `batchSave` 升级为超级增删改事务入口 `superCud`；`SuperCudReq` 使用 `addEntities`、
    `addByFields`、`updateEntities`、`updateCriteria`、`deleteIds`、`deleteCriteria`，返回三类汇总数量的
    `SuperCudResult`；
11. 实现独立 `CrudWriteExecutor.superCud`，让普通新增、条件新增、按 ID 或 Criteria 修改、按 ID 或
    Criteria 删除在同一个事务中完成；所有专项写方法只构造对应的 `SuperCudReq`；
12. 实现基于实体字段的 `createIfAbsent`；Java 调用支持 getter Lambda，唯一索引由业务按并发要求设计；
13. 实现 `getOne/list/requireOne` 等默认方法、`newCriteria()` 及 Criteria `copy()`；
14. 实现支持 `id asc/id desc` 的 `cursorPage`；
15. 增加 DDL 推断歧义、越权 ID、属性存在性、空集合、空值写入、条件新增并发、混合批量回滚、乐观锁、批量性能和
    树循环测试。

暂不实现跨微服务级联、跨数据中心事务、任意深度嵌套保存、共享资源生命周期推断和复杂业务状态机；这些
使用显式应用服务处理。

## 9. 验收标准

- DDL 每条关系只生成当前实体属性，反向属性必须由目标 DDL 声明；
- 可选参数能可靠推断，歧义配置在生成阶段失败；
- `cascadeDelete` 默认 `false`，删除只沿明确开启的方向执行；
- 树形实体固定拥有 `parent/children`，只允许向下级联删除；
- 关联查询默认关闭，显式展开时使用批量 SQL 且不产生循环；
- 请求属性未出现、`null`、空集合和非空集合的写入语义符合本文定义；
- HTTP、gRPC 和本地 Java 调用都把字段出现信息转换为同一种瞬态写入属性掩码，执行器不依赖原始 JSON；
- 当前实体和直接关联对象在一个本地事务提交；
- `MERGE/REPLACE`、`IGNORE_NULL/WRITE_NULL`、批量定位、归属校验和回滚行为可重复验证；
- 只有批量新增、批量修改、批量删除和 `superCud` 保留标准写入 HTTP/gRPC 入口；单体新增、不存在时新增
  和单体删除仅作为 Java 默认方法，并分别复用正式入口；
- `SuperCudReq` 包含 `addEntities`、`addByFields`、`updateEntities`、`updateCriteria`、`deleteIds` 和
  `deleteCriteria`；
- 所有操作可以在同一个请求中任意组合；空变更集返回三个零值计数且不产生数据库写入；
- `SuperCudResult` 仅返回 `addedCount`、`updatedCount`、`deletedCount`；
- `superCud` 在一个本地事务内执行全部变更，支持前端汇总协作编辑产生的新增、修改、删除后一次
  保存；
- `createIfAbsent` 接受实体属性名或 getter Lambda；业务对唯一性和并发幂等负责，框架不强制唯一索引；
- 所有标准写方法都构造 `SuperCudReq`，最终跨 Bean 进入 `CrudWriteExecutor.superCud`，事务、授权、校验、生命周期、关联、审计
  和事件不依赖 Service 自调用触发 Spring AOP；
- 远程代理遇到带 `@EntrypointOperation` 的默认方法时发起远程请求，只在未标注的便捷默认方法上执行
  Java 默认方法体；
- `getOne/list/requireOne` 不生成远程入口并且不污染调用方 Criteria；
- `newCriteria()` 由生成的具体 Service 类型安全实现，不恢复运行时反射构造；
- `cursorPage` 支持 ID 升降序、无总数查询、无 offset 漂移，并拒绝非法排序；
- 普通和树形级联都不依赖数据库外键；
- 跨微服务关系不会进入通用级联或本地事务。
