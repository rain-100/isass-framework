# NoCode 关联查询

关联由 DDL 表级注释声明，例如：

```text
[关联表-列表-SampleImage; cascadeDelete=true]
```

未填写的 `property/localKey/targetKey` 由生成器按实体和字段命名推断；推断有歧义时生成失败。关系是单向的，
目标实体只有在自己的 DDL 中声明对应关系后才生成反向属性。

关联标记的完整语法、默认推断、方向性级联和树形规则见
[数据库表设计规范](../database/table-design.md#liquibase-注释-dsl)。数据库不创建外键或数据库级联约束；
关系完整性和级联写入由应用事务、NoCode 协调器、唯一索引和业务校验保证。

前端通过 Criteria 的关联展开参数选择本次查询需要的关系。`page`、`cursorPage` 以及 Java 默认方法
`getOne/list/requireOne` 都经过同一个查询协调器；默认不展开。协调器按“当前一批实体 + 一个关系”批量查询
目标记录，不逐行查询。

使用 `loadRelated` 保存要加载的实体成员，嵌套关系通过目标 Criteria 继续声明：

```java
new RoleCriteria().loadRelated("rolePermissions",
        new RolePermissionCriteria().loadRelated("permission"));
```

HTTP QUERY 中，`loadRelated` 是一个完整 JSON 值，由 HTTP 层统一 URL 编码：

```json
[{
  "property": "rolePermissions",
  "criteria": {
    "entityType": "rolePermission",
    "loadRelated": [{ "property": "permission" }]
  }
}]
```

关联键使用生成的 EntityAssociation 元数据；显式 localKey/targetKey 与元数据不一致时报错。
同一 property 在便捷 API 中重复设置会替换；手工列表重复声明则报错。
关系成员是集合还是单体由实体成员类型决定。JOIN 结果由 MPJ 原生的
`selectCollection`/`selectAssociation` 映射；单体关系应由业务或数据库约束保证唯一，
匹配多个目标时 MPJ 不报基数冲突，保留的目标不作为稳定选择规则。
查询直接在传入 Criteria 中补齐主表关联键和 JOIN 结果属性，调用方可以观察这些修改。
JOIN 会在调用 `leftJoin` 等方法的源 Criteria 上补齐装配主键；ON 两侧字段只参与连接条件，
不因 JOIN 自动进入结果投影。目标 Criteria 的显式选列不因外层 JOIN 而追加主键。
目标实体的主键仍会进入 Wrapper 投影以完成关联去重，并可保留在返回实体中。
未设置选列的实体读取会在转换 Wrapper 时将非敏感持久化属性写入可变 Criteria 的 `returnFields`；
显式选列不受默认敏感字段排除规则覆盖。COUNT 和 WHERE 子查询不写入默认选列，
共享的 EmptyCriteria 保持不可变。仅有目标选列不会将普通 JOIN 改为派生表 JOIN。
最外层 `returnFields` 只定义读取投影；写入时不读取它，不用它决定 SET 字段、删除目标或匹配范围，
也不因其非空（包括先前查询自动补列）而报错。`IN/NOT IN` 等子查询内部必需的目标选列仍按子查询语义处理。
loadRelated 分批直接复用目标 Criteria，补齐目标 ID/关联键；目标为空或 EmptyCriteria 时只创建一次具体 Criteria 并写回。
原有 WHERE 整体分组，另加一条框架持有的关联 IN；每批只替换该节点的 value，不查找或覆盖业务同字段 IN。
结束或异常时清理本次临时 IN、恢复原分页值；无其他外层新增条件时解除临时分组，避免重复执行和序列化携带旧批次。
生命周期追加/修改的业务条件、默认排序和补齐的选列继续保留；监听器应幂等设置条件，避免重复分页调用时不断追加。

每层对一批主记录收集关联键，分批 IN 查询目标；目标查询走标准分页及生命周期，关闭 COUNT，
按实际页大小读取后续页，不因单页上限截断。嵌套最多 16 层，不逐主记录发起查询。
本次不新增数据权限或租户过滤，保留已有鉴权、生命周期与逻辑删除规则。

层级实体的 `loadRelated("children")` 只展开一层，显式嵌套才继续展开。
完整树使用 `tree`；后代 ID 使用 `descendantIds`。树查询不能再次声明 parent/children 关联结果。

旧 `association.query`、`association.<path>.criteria.<property>`、`associationQueries`、
`associationCriteria` 和 IAssociationCriteria 已删除，不提供兼容；调用方应同步迁移。

### 条件模型与传输

WHERE 使用 `sourceProperty/condition/value`；旧 `propertyName` 不再接收。
JOIN/WHERE 的双字段关系使用 `sourceProperty/targetProperty`，目标保存 `targetCriteria`。
EmptyCriteria.of(Entity.class) 返回按实体缓存的不可变空实例，不能往其中写请求状态。

普通 JSON 保持对象和数组；Criteria 的 QUERY 参数由 CriteriaQueryParamConverter 统一转换，
whereConditions、joinConditions、loadRelated、fromCriteria 各自是一个 JSON 值，不重复字符串化内层 Criteria。
实体类型通过已注册的 Criteria 与 entityType 小驼峰名称恢复，不加载请求指定的任意 Java 类名。
`ICriteria.getEntityType()` 仅从按 Criteria 类缓存的实体泛型元数据读取名称，不触发注册；
服务类型扫描与具体 Criteria 反序列化负责注册，`EmptyCriteria` 则使用构造时保存的实体类。
HTTP 通过通用 QueryParamConverter 选择扩展，匹配多个扩展时报错；gRPC 复用普通 JSON 行为。

条件构造保留传入对象和集合的引用，不隐式深复制；执行前的修改会反映到引用它们的 Criteria 中。
需要独立条件树时，由调用方显式调用 `criteria.copy()` 或 `CriteriaCopies.copy(condition)` 后传入。
继续增删条件或选列时须传入可变集合；`List.of` 等不可变集合不会自动转为可变集合。
正式查询入口同样使用原 Criteria，保留分页、游标、投影补键和监听器修改；需要隔离时由调用方显式 copy。
布尔语法在转换 Wrapper 的消费过程中校验。
组内缺省 AND，父 condition=OR 改为缺省 OR；独立 AND/OR 只能出现在两个表达式之间，
首尾或连续连接符报错。已有条件缺少值时报错，空筛选控件应在便捷 setter 登记前省略。

`WhereCondition` 提供以下便捷工厂，不改变 JSON 字段或校验时机：

```java
WhereCondition.eq("name", "admin");                      // 属性与常量
WhereCondition.eq(Role::getName, "admin");                // Lambda 属性与常量
WhereCondition.eq(Role::getId, RolePermission::getRoleId); // source 与 target 字段
WhereCondition.or(
    WhereCondition.eq(Role::getName, "admin"),
    WhereCondition.and(
        WhereCondition.eq(Role::getName, "operator"),
        WhereCondition.not(WhereCondition.eq(Role::getId, 1L))));
```

`and/or` 接收可变数量的 WhereCondition，设置父分组的默认连接符；`not` 接收一个条件。
工厂保留传入的值及子条件引用；`and/or/not` 的 children 是可变参数数组的列表视图。
创建空组不立即校验，仍在转换 Wrapper 时拒绝。
双字段 `eq` 的所属表由 JOIN ON 或相关子查询上下文解释；没有同名字符串双字段重载，
避免 `eq("name", "admin")` 被误解为两列比较。字符串双字段比较继续使用原有 setter。

静态反射元数据使用 `ClassValue` 按类型缓存：构造器、字段、JSON 绑定泛型、集合元素类型、
关联定义和属性读写描述。关联定义必须是实体类型的固定元数据，不依赖实例值。
不缓存请求对象、条件值、Wrapper 或查询结果；空根行仍逐次创建可变实体。
公开构造 API、查询执行器和 loadRelated 分批均不复制请求树；显式 copy 等仍需复制的流程继续复用这些静态元数据。

### JOIN、派生表与子查询

这里的动态字段指将 Criteria 的 Java 属性名转换为 MPJ 的 `Column` 对象。例如 `roleId` 搭配
RolePermission 的实体元数据，经持久化属性白名单检查后生成 `new Column(columns, "roleId")`，
MPJ 再解析数据库列 `role_id` 与当前表别名；条件值继续使用参数绑定。框架复用
`AptQueryWrapper` 的字段对象 API，不要求业务使用注解处理器生成 APT 表字段类，也不接受前端传入 SQL 列表达式。
标量 WHERE 和 JOIN ON 条件直接写入 MPJ Wrapper，使用 MPJ 的 `Column` 解析别名与参数空间；
MyBatis-Plus 写入 Wrapper 继续使用相同的条件语义，不再借助临时 QueryWrapper 拼接关联条件。
`Condition` 使用 `Logical`、`Compare`、`Membership`、`NullCheck`、`Text`、`Array`、`Json`、`Existence`
内部分组，例如 Java 中写 `Condition.Compare.EQUAL`；Criteria JSON 仍使用原有的 `"EQUAL"` 等字符串。
`WrapperUtil` 直接消费条件树并调用 MP/MPJ Wrapper；比较、集合、空值、JSON/数组片段及
AND/OR/NOT 分组均在该转换层处理。`BaseCondition` 只保存 ORM 无关的 Java 属性名；解析后的
SQL 列名、表别名及 MPJ `Column` 仅是本次转换的局部值，不写回条件对象或进入 JSON。
IN 值经 `ConvertUtil` 转为集合，JSON 路径经过校验，值由 Wrapper 参数绑定；不支持的条件和方言明确报错。
同一 Criteria 可重复转换为不同 Wrapper，但调用方仍应自行管理并发修改可变 Criteria 的时序。
字符串列 Wrapper 使用原生比较、集合与空值方法；双字段 ON/相关比较使用 MPJ 的
`eqSql/gtSql/geSql/ltSql/leSql`，不等比较使用 `NOT (eqSql(...))`。两侧列名均从实体元数据解析；
普通 MP 同表双字段条件也使用这些方法，两侧列不加别名；MPJ 主表列使用当前 Wrapper 别名，
JOIN 目标列按该连接实例解析别名。
这些方法的右侧 SQL 表达式仅由框架生成，不接收前端传入的 SQL 片段。
写入型 IN/NOT_IN 子查询由 MPJ 的字符串选列方法选择经过实体元数据解析的单列，
不通过 `selectFunc` 组装投影。JSON 数组的 ANY/ALL 条件使用 Wrapper 的 `nested/or`
组成一个整体布尔条件；JSON 对象路径只保留经过校验的列路径表达式，等值和模糊比较交给 Wrapper 的
`eq/like`；达梦的 JSON 模糊匹配回退也使用 Wrapper 的 `like`。
`JSON_CONTAINS` 函数、数组运算符、空集合恒真/恒假及派生表输入仍使用框架控制的 SQL 片段；
列名和别名由 ORM 元数据解析，值通过 Wrapper 参数绑定，不接受前端 SQL。
每个 SQL 表实例通过 `BaseColumnFactory.create(type)` 创建独立的 MPJ `BaseColumn`；同一实体的自连接或多次连接不共享该对象。

`leftJoin/innerJoin/rightJoin/fullJoin/crossJoin` 使用 MPJ 动态字段 Wrapper；
所有 Criteria 查询统一从 WrapperUtil.getQueryWrapper 构造同一种 MPJ Wrapper，不先按普通/关联查询分流。
Criteria 到 Wrapper 的条件、JOIN、FROM、子查询、排序和投影转换均由 WrapperUtil 内部方法完成，不另设编译器类。
内部方法按职责使用 `process`（消费 Criteria 并设置 Wrapper）、`apply`（应用已解析条件）、
`resolve`（解析元数据）、`create`（创建对象）和 `build...Sql`（构建 SQL）命名。
MPJ 映射配置由 `createJoinResultMapping/createNestedJoinResultMapping` 创建，嵌套列由 `selectNestedJoinColumns` 选出。
`applyCondition` 将已解析列对应的条件应用到普通 MP 或 MPJ 写入 Wrapper；APT 条件由 `applyResolvedAptCondition` 应用。
公开 `getQueryWrapper(criteria)` 创建实体查询 Wrapper，再调用完整参数的递归入口。
完整入口按 SQL 顺序调用 `processFrom`、`processJoin`、`processWhereConditions`、`processSelect`、`processOrderBy`。
FROM 输入、带条件或嵌套关联的 JOIN 目标，以及 WHERE 子查询均递归调用同一个完整入口；
每层共享参数绑定，表实例与别名作用域由该层 Wrapper 管理。
SELECT 阶段按 `MpjWrapper.QueryUsage` 一次确定选列：ENTITY 配置非敏感默认字段、必要主键及 MPJ 结果映射，
FROM 保留根表持久化列供外层使用，JOIN 另外输出嵌套关联列，EXISTS 只选择常量 1，SINGLE 必须显式选择一列，
COUNT 不配置关联结果映射且不改写 Criteria 返回字段；不先生成选列再清空重建。
递归构建按对象身份检查当前路径中的 Criteria 和条件节点，循环引用在 SQL 执行前报错；
退出节点时移除路径标记，同一个条件对象仍可在独立分支复用，不复制请求树或预先全量扫描。
没有 JOIN 时不创建 MPJ 关联结果映射，也不启用根单位去重分页；loadRelated 仍在主查询后执行。
COUNT 使用同一构建入口的内部投影参数；普通更新/删除分别经 `getUpdateWrapper`/`getDeleteWrapper`，
关联筛选写入经 `getJoinUpdateWrapper`/`getJoinDeleteWrapper` 转为 MPJ Wrapper。
Repository 的 Criteria 计数统一使用零大小分页：普通查询走 MP 分页计数，当前查询层含 JOIN 时走根实体分页计数；不改动 Wrapper 的选列。
`exists/notExists/in/notIn` 支持直接保存目标 Criteria，不先查询目标 ID。
Class 重载保存 `EmptyCriteria.of(Target.class)`；有目标条件时编译为派生表，目标过滤不移到最终 WHERE。
`fromCriteria` 单独控制主表输入，必须与当前 Criteria 的实体相同。
JOIN 表别名由 MPJ 分配；WHERE/FROM 子查询使用 MPJ 的子查询别名前缀按嵌套层级生成别名，
同级子查询各处于独立 SQL 作用域，不维护全局递增编号。

```java
new IconGroupCriteria()
    .leftJoin(Icon.class, IconGroup::getId, Icon::getIconGroupId)
    .setPageNum(1L).setPageSize(20L);
```

查询需要装配 JOIN 结果时，简单等值 JOIN 根据 DDL 关联元数据推断 resultProperty；复杂条件或歧义时必须显式设置。
`resultProperty` 仅用于查询结果装配；更新和删除不装配 JOIN 结果，也不以它定位关联写入目标。
WrapperUtil 在执行 SQL 前构造 MPJ 的 `selectCollection`/`selectAssociation` 映射，
Repository 直接接收 MPJ 装配的实体，不再读取原始行后自行投影、去重或赋值。
集合按目标 ID 折叠；单体匹配多个目标时遵循 MPJ 原生行为，不执行额外基数查询。
RIGHT/FULL 的空主表行逐行 new 实体、标量字段为 null，再填入关联成员；不复用实体 EMPTY。
JOIN 目标也可以继续声明 JOIN 和查询后执行的 loadRelated；WHERE 子查询不接受 loadRelated 或 orderBy。
嵌套分页仅接受默认值，默认 getter 不生成 LIMIT，非默认分页值报错。IN 子查询必须显式选择一列。
相关 EXISTS/NOT EXISTS 便捷方法直接修改目标 Criteria，将已有 WHERE 整体分组后再 AND 关联键；
例如原有 A OR B 生成 `(A OR B) AND source.id = target.parent_id`。
IN/NOT IN 的选列便捷重载直接设置目标 Criteria 的 returnFields；JOIN 方法直接设置传入 JoinCondition 的 joinType。
loadRelated、JOIN、子查询及 fromCriteria 均保存原引用，同一实例用于多个位置时会共享后续修改。
只有目标是共享的 EmptyCriteria 且需要修改时，才物化为新的具体 Criteria，并保存新引用。

分页/count 使用“匹配根 ID 去重 + 每个空根行”口径，分页后保留完整关联集合。
分页 SQL 内部用展开结果位置、分页单位起始位置和分页单位序号三列组织结果；这些别名不属于 Criteria 公共接口。
`AdvJoinPaginationInnerInterceptor` 调用 `AdvJoinPageSql`，对 MPJ SQL 使用 JSQLParser AST 添加窗口函数，不使用 pageByMain，也不先查询一批 ID；
分页所需根主键及关联主键的最终列别名从 MPJ Wrapper 的选列读取，不维护第二套结果装配计划。
因此需要支持窗口函数的数据库（MySQL 8+）。当前会处理符合条件的连接结果，再按根单位截取，
尚未验证百万级数据性能；应使用有效筛选与关联键索引，不将此实现视为自动选择最优执行计划。
ID 游标不支持产生空根行的 RIGHT/FULL。MySQL 的 FULL JOIN 在执行前拒绝，不用 UNION 模拟。
写入 Criteria 中的 JOIN/EXISTS 等只筛选根实体；当前写入转换支持 LEFT/INNER JOIN、目标条件派生表、
嵌套 JOIN，以及 WHERE 中的 EXISTS/NOT_EXISTS/IN/NOT_IN。筛选 JOIN 不要求 `resultProperty`。
写入 Wrapper 不读取最外层 `returnFields`；子查询 IN/NOT_IN 仍必须显式选择一列。
RIGHT/FULL/CROSS JOIN、`fromCriteria` 及不能等价转换的作用域在 SQL 执行前明确拒绝，不删掉条件后继续写入。
写入 SQL 的数据库方言由 MPJ 负责；H2 的 MySQL 兼容模式不支持 MySQL 的 `UPDATE ... JOIN` 语法。
`update(entity)` 从实体 ID 生成 Criteria 的 ID 条件；`update(entity, criteria)` 由最终 Criteria 确定写入范围。
仅当最终 Criteria 明确限定唯一主实体 ID 时，才允许随主实体更新已提交的关联对象；
多个带 ID 的关联对象分别按各自 ID 更新。仅由其他条件匹配时只能更新主实体字段；
即使运行时恰好匹配一条、关联对象有 ID，或实体 ID 非空但最终 Criteria 未以它收窄，也不得关联写入。
实体 ID 与 Criteria ID 同时存在但不一致时，在执行前报错；ID 不进入 SET。
若请求仅提交关联属性，主实体不参与 SET；`WRITE_NULL` 只作用于
参与 SET 的实体的非框架维护可更新字段，不得因主实体用作关联容器而清空其 null 字段。
`MANY` 集合即使只有一个元素，也按其 ID 逐对象更新或新增：`MERGE` 保留未提交的旧目标，
`REPLACE` 在保存已提交目标后删除未提交的旧目标，不把单元素集合当作统一更新模板。
关联删除时，JOIN/子查询只筛选根实体；NoCode 先固定匹配的根 ID，再按
`cascadeDelete=true` 的方向逐层处理应级联对象。根 ID 每 500 个一批，直接外键目标聚合批量处理，
更深层仍经目标服务的删除生命周期；最后按固定 ID 删除根实体。
每条 SQL 只删除一个目标表，不默认使用 MPJ `deleteAll()`；删除数量只计根实体。

Mapper 统一继承 MPJBaseMapper，业务 Service 不改为 MPJ 的 Service 基类。
关联查询的字段、执行和结果装配合同以本文为准。

写入时，实体关系属性的请求出现性决定是否处理：

- 未提交关系属性：不处理；
- `MERGE`：新增无 ID 对象、更新有 ID 对象、保留未提交的已有关系；对于以当前实体 ID 关联从表外键的单体关系，
  无 ID 对象会先按关联键查找唯一目标，存在时更新该目标，不存在时才新增，查出多条时按一对一数据异常失败；
- `REPLACE`：请求值代表该方向最终结果，移除未提交的旧目标；
- 显式空集合：`MERGE` 不处理，`REPLACE` 清空；
- 单体关系显式 `null`：`REPLACE` 清空，`MERGE` 不处理。

直接关联写入只处理当前实体声明的一层关系，不递归保存任意深度对象图。跨聚合复杂流程由应用服务编排，
跨微服务或跨数据源关系不能使用通用关联写入。
