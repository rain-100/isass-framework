# NoCode Criteria 关联写入核心实施清单（待审核）

本清单以 `docs/usage/nocode/association-query.md`、`docs/usage/nocode/crud-lifecycle.md` 及已确认的关联更新、关联删除和 `WRITE_NULL` 语义为实施依据。最外层 `returnFields` 的读写边界已同步到关联查询使用文档；MPJ 支持性和 SQL 等价性仍须在实施时验证。

## 1. 条件与写入模式

`isass-core-common/src/main/java/vip/isass/framework/common/criteria/impl/type/Condition.java`

```java
// 保留：ORM 无关的条件操作符和现有 JSON 名称。
sealed interface Condition {
    enum Logical { AND, OR, NOT }
    enum Compare { EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL }
    enum Membership { IN, NOT_IN }
    enum NullCheck { IS_NULL, IS_NOT_NULL }
    enum Text { IS_EMPTY, IS_NOT_EMPTY, START_WITH, LIKE, NOT_LIKE }
    enum Array { CONTAINS_ALL, CONTAINS_ANY }
    enum Json { JSON_OBJECT_PATH_EQUAL, JSON_OBJECT_PATH_LIKE,
                JSON_ARRAY_CONTAINS, JSON_ARRAY_CONTAINS_ANY, JSON_ARRAY_CONTAINS_ALL }
    enum Existence { EXISTS, NOT_EXISTS }

    fromName(String name); // 保留：未知值报错，不默默忽略。
}
```

`isass-core-common/src/main/java/vip/isass/framework/common/criteria/NullValueMode.java`

```java
enum NullValueMode {
    IGNORE_NULL, // 保留：实体的 null 字段不进入 SET。
    WRITE_NULL   // 修改语义：参与 SET 的实体，其非框架维护的可更新持久化字段
                 // 即使为 null 也进入 SET；只有关联值时主实体不参与 SET，
                 // 不再要求该字段被标记为“已提交”；框架维护字段仍由框架维护。
}
```

`isass-core-common/src/main/java/vip/isass/framework/common/criteria/BaseCondition.java`

```java
abstract class BaseCondition<B> {
    String sourceProperty;        // 保留：Java 属性名。
    Condition condition;
    String targetProperty;        // 保留：Java 属性名。
    Object value;
    List<WhereCondition> children;
    ICriteria<?, ?> targetCriteria;

    setSourceProperty(...); setTargetProperty(...); setCondition(...);
    setValue(...); setChildren(...); setTargetCriteria(...);
    // 删除：sourceColumn、targetColumn 字段及其 getter/setter；
    //       setSourceProperty/setTargetProperty 不再清空这两个字段。
    // 保留：普通 JSON 只传 ORM 无关的条件属性；未知条件字段报错。
}
```

`isass-core-common/src/main/java/vip/isass/framework/common/criteria/WhereCondition.java`、`JoinCondition.java`

```java
class WhereCondition extends BaseCondition<WhereCondition> {
    // 保留：现有标量、分组、双字段比较及 EXISTS/NOT_EXISTS/IN/NOT_IN 工厂。
    // 不新增 SQL 列名、表名或 ORM Wrapper 字段。
}

class JoinCondition extends BaseCondition<JoinCondition> {
    JoinType joinType;      // 保留：连接类型。
    String resultProperty; // 保留：查询时将 JOIN 结果装配到主实体的关联属性。
    // 更新/删除不装配查询结果，不读取 resultProperty 定位写入目标。
}
```

`isass-core-common/src/main/java/vip/isass/framework/common/criteria/IUpdateCriteria.java`、`IRelatedQueryCriteria.java`、`impl/type/FullTypeCriteria.java`

```java
interface IUpdateCriteria<C> {
    UpdateMode updateMode;      // 保留：直接关联写入 MERGE/REPLACE。
    NullValueMode nullValueMode;// 保留字段，按上述新语义解释 WRITE_NULL。
    Collection<String> matchFields;
}

interface IRelatedQueryCriteria<E, C> {
    List<JoinCondition> joinConditions; // 保留：更新/删除筛选复用已有 JOIN 树。
    List<RelatedCondition> loadRelated; // 保留：只属于读取/查询后装配。
    // 保留现有 leftJoin/innerJoin/rightJoin/fullJoin/crossJoin、exists/notExists/in/notIn 方法。
}

class FullTypeCriteria<E, C> {
    // 保留：whereConditions、joinConditions、loadRelated、fromCriteria、returnFields、
    //       orderBy、分页、updateMode、nullValueMode、matchFields；不新增关联写入字段。
}
```

## 2. 实体关联与持久化契约

`isass-core-common/src/main/java/vip/isass/framework/common/entity/EntityAssociation.java`、`IEntity.java`

```java
record EntityAssociation(
    String property, Kind kind, Class<? extends IEntity<?>> targetType,
    String localField, String targetField, boolean cascadeDelete
) {
    enum Kind { ONE, MANY }
    // 保留：由生成器/实体提供固定、ORM 无关的关系元数据。
}

interface IEntity<E> {
    List<EntityAssociation> associations(); // 保留：读取和现有直接关联写入均复用。
    // 保留属性出现性能力，供直接关联 MERGE/REPLACE 等已有场景使用；
    // WRITE_NULL 不再以出现性决定普通字段是否写 null。
}
```

`isass-database-core/src/main/java/vip/isass/framework/database/core/repository/IRepository.java`

```java
interface IRepository<E, C> {
    // 保留签名；更新值由实体及其关联对象提供，范围只由 Criteria 提供；
    // 主实体 ID 是 WHERE 条件，不进入 SET。
    int updateCountByCriteria(E entity, ICriteria<E, C> criteria);
    int deleteCountByCriteria(ICriteria<E, C> criteria);
    // 保留其他 CRUD 签名，不新增同义公共方法。
}
```

`isass-nocode-core/src/main/java/vip/isass/framework/nocode/entity/SuperCudReq.java`、`service/ICrudService.java`

```java
record SuperCudReq<E, C>(
    List<E> addEntities, List<String> addByFields,
    List<E> updateEntities, C updateCriteria,
    List<Serializable> deleteIds, List<C> deleteCriteria
) {
    // 保留字段和 Builder；关联更新、关联删除仍走现有请求结构。
}

interface ICrudService<E, C, PK> {
    Long update(Collection<E> entities, C criteria); // 保留 Entrypoint。
    Long delete(C criteria);                       // 保留 Entrypoint。
    // 不新增同义的 HTTP/gRPC 操作。
}
```

`isass-nocode-generator/src/main/resources/templates/nocode/mapper.java.ftl`

```java
// 保留：生成 XxxMapper extends MPJBaseMapper<Xxx>。
// 核对已生成的 Mapper 契约；本轮无需新增 Mapper 接口、XML 或生成器参数。
```

## 3. Criteria 到 Wrapper 的转换

`isass-database-mybatisplus/src/main/java/vip/isass/framework/database/mybatisplus/orm/WrapperUtil.java`

```java
class WrapperUtil {
    ClassValue<List<String>> DEFAULT_RETURN_FIELDS; // 保留：默认返回的非敏感 Java 属性名。
    // 查询 Wrapper 的使用位置由 MpjWrapper.QueryUsage 标识。

    validateWriteOptions(ICriteria<E, C> criteria) {
        // 新增：四个更新/删除 Wrapper 入口共用；非默认排序、分页和计数设置
        // 不作为写入范围，遇到时在执行 SQL 前明确报错。
    }

    Wrapper<E> getQueryWrapper(ICriteria<E, C> criteria) {
        // 修改：精简入口创建 MpjWrapper 和当前递归路径身份集合，调用完整入口。
        // 单表/关联查询统一构建，不深复制 Criteria。
    }

    QueryBuildResult getQueryWrapper(MpjWrapper<?> wrapper, ICriteria<?, ?> criteria,
                                BaseColumn<?> outer, QueryUsage usage, Set<Object> activePath) {
        // 修改：进入当前对象路径，重复进入同一对象时抛循环引用异常。
        try {
            validateQueryUsage(criteria, usage);
            processFrom(wrapper, criteria, activePath);
            joinedTargets = processJoin(wrapper, criteria, activePath);
            processWhereConditions(wrapper, criteria, outer, activePath);
            derivedFields = processSelect(wrapper, criteria, joinedTargets, usage);
            processOrderBy(wrapper, criteria);
            return QueryBuildResult(joinedTargets, derivedFields);
        } finally {
            // 退出路径，允许同一对象在独立分支复用。
            activePath.remove(criteria);
        }
        // FROM/JOIN/WHERE 子查询均递归调用此入口，共享参数绑定。
        // SELECT 按用途一次设置选列和映射，不先生成再清空重建。
    }

    UpdateWrapper<E> getUpdateWrapper(ICriteria<E, C> criteria) {
        // 修改：仅编译普通更新条件，不把关联写入塞进 MP UpdateWrapper。
        // 与删除共用底层的普通 WHERE 遍历，不再作为删除的对外转换入口。
        // 最外层 returnFields 只用于查询投影；写入不读取，也不因其非空而报错。
    }

    resolveWritableBusinessColumns(Class<E> entityType) {
        // 修改：普通更新与关联更新共用可更新字段判定；排除框架维护字段。
        // WRITE_NULL 只为参与 SET 的实体生成这些字段的 null SET，
        // 框架维护字段继续走原有维护逻辑。
    }

    UpdateWrapper<E> getDeleteWrapper(ICriteria<E, C> criteria) {
        // 新增：普通删除条件入口；返回 MP Wrapper，不启用关联删除。
        // 保留有效 WHERE 校验，不允许无筛选的远程全表删除。
    }

    UpdateJoinWrapper<E> getJoinUpdateWrapper(ICriteria<E, C> criteria) {
        // 新增：按已有 JoinCondition/WhereCondition/targetCriteria 编译关联筛选。
        // 不读取最外层 returnFields，不构造 SELECT 投影或结果映射；
        // 筛选型 JOIN 不要求 resultProperty；它也不用于定位关联 SET 目标。
        // 同类多次 JOIN/自连接仅用于筛选时照常编译；若关联 SET 要求写入
        // 其中一个实例，须能将目标关系唯一绑定到 MPJ 的表别名，
        // 无法唯一绑定或 MPJ 无法指定该别名时在执行 SQL 前报错。
        // 保留相关子查询选列、别名和参数绑定。
        // JOIN ON 与 WHERE 按原布尔树消费时校验；所有属性先转持久化列。
        // 修改：逐项验证 RIGHT/FULL/CROSS JOIN、fromCriteria、派生表和嵌套 JOIN
        // 是否能由 MPJ 写入 Wrapper 保持相同的写入目标与 SET 值；
        // 无法等价表达时，在执行 SQL 前指出具体条件位置并报错，不忽略条件。
        // 仅更新根实体时，匹配目标按去重后的根实体 ID 判断；
        // 同时更新关联对象时，还须保持关联目标的表实例、记录 ID 和写入值。
        // 这里是转换正确性约束，不在生产写入前另查两组 ID 并比较。
        // SQL 方言由 MPJ 负责；若生成结果不符合目标数据库要求，修复 MPJ，
        // ISASS 不维护写入方言白名单或自行拼接替代 SQL。
    }

    DeleteJoinWrapper<E> getJoinDeleteWrapper(ICriteria<E, C> criteria) {
        // 新增：按相同 Criteria 编译关联删除的筛选 Wrapper；
        // 不读取最外层 returnFields，不生成 SELECT 投影，且不因选列非空而报错。
        // 只指定根实体为删除目标，不调用 deleteAll()/delete(关联表)。
        // 删除目标按去重后的根实体 ID 判断；无有效 WHERE、
        // MPJ 无法等价表达的连接/子查询均在执行前指出条件位置并失败。
    }

    processJoinWriteCriteria(writeWrapper, criteria) {
        validateWriteOptions(criteria);
        // 写入 JOIN 逐项调用 processWriteJoin；ON 由 processWriteJoinCondition 处理。
        processWriteWhereConditions(...);
        // WHERE 子查询调用 processWriteExistsCondition/processWriteInSubqueryCondition。
        // 普通 MP WHERE 由 processMpWhereConditions 消费，APT 查询 WHERE 由 processWhereConditions 消费。
        // applyColumnComparison 应用双字段比较，applyCondition 应用已解析列对应的条件，
        // applyResolvedAptCondition 应用已解析的 APT 条件；String 指列参数表示，不限制 value 类型。
        // sourceProperty/targetProperty 始终保留 Java 名；依据当前表实例与别名解析
        // 本次所需的列名或 MPJ Column，作为局部变量直接调用 Wrapper，不写回条件对象。
        // 数组值、JSON 值和路径不直接拼入 SQL；参数绑定或严格验证。
        // 不支持的 JSON/数组方言抛异常，不能返回 null 片段并跳过条件。
    }

    private record SqlFragment(String sql, List<Object> parameters) {}
    private static String[] parseJsonPropertyPath(String sourceProperty) { ... }
    private static String buildJsonPathExpression(...) { ... }
    private static JsonArrayConditions resolveJsonArrayConditions(...) { ... }
    private static SqlFragment buildArrayOrJsonConditionSql(...) { ... }
    // 修改：原 ConditionApplier 的 Fragment、JSON/数组路径校验、方言片段及参数绑定逻辑
    //       一并迁入此类的私有结构和方法；不引入另一套条件编译器。
    // 保留：静态实体列元数据缓存、自连接时独立表实例、查询投影与分页所需列。
}
```

`isass-database-mybatisplus/src/main/java/vip/isass/framework/database/mybatisplus/orm/MpjWrapper.java`

```java
final class MpjWrapper<T> extends AptQueryWrapper<T> {
    enum QueryUsage { ENTITY, FROM, JOIN, EXISTS, SINGLE, COUNT } // 查询 Wrapper 的使用位置。
    // FROM 保留根表列；JOIN 另外输出嵌套关联列；EXISTS 选择常量 1。
    // ENTITY 配置默认返回字段、必要主键和 MPJ 映射；SINGLE 只选一列；COUNT 不配置映射。
    // 保留：查询专用的 JOIN、CROSS、FROM、子查询别名及共享参数空间扩展。
    // 不把 UpdateJoinWrapper/DeleteJoinWrapper 强行转换为 AptQueryWrapper。
}
```

`isass-core-common/src/main/java/vip/isass/framework/common/criteria/ConditionApplier.java`，以及 `isass-database-mybatisplus/src/main/java/vip/isass/framework/database/mybatisplus/orm/` 下三个实现类

```java
// 删除：ConditionApplier 及其中的逐操作符接口、Fragment 和静态辅助方法。
// 删除：MybatisPlusConditionApplier、MpjAptConditionApplier、
//       JoinAbstractWrapperConditionApplier。
// 修改：WrapperUtil、MpjWrapper 的调用点直接使用 WrapperUtil 的条件编译方法；
//       列名和别名只存在于本次转换的局部上下文，不写回 BaseCondition。
```

## 4. Repository 写入分流

`isass-database-mybatisplus/src/main/java/vip/isass/framework/database/mybatisplus/orm/MybatisPlusRepository.java`

```java
abstract class MybatisPlusRepository<E, C, M extends MPJBaseMapper<E>> implements IRepository<E, C> {
    int updateCountByCriteria(E entity, ICriteria<E, C> criteria) {
        // 修改：校验实体、Criteria 和有效更新范围；禁止空 SET。
        // 根实体是否参与 SET，由请求是否提交了至少一个非框架维护的可更新
        // 根实体属性决定；ID、关联属性和框架维护字段不算根实体更新值。
        // 无关联筛选：WrapperUtil.getUpdateWrapper(criteria) -> MP BaseMapper.update(entity, wrapper)。
        //   IGNORE_NULL：沿用非 null 字段更新。
        //   WRITE_NULL：所有非框架维护的可更新持久化字段的 null 也写入；
        //               删除现有 presentProperties 非空要求和
        //               isPropertyPresent(field) 才写 null 的判断；框架维护字段不写 null。
        // 有 JOIN/关系子查询：WrapperUtil.getJoinUpdateWrapper(criteria)。
        //   JOIN/子查询只负责筛选匹配的根实体；不能据此推断允许写入关联对象。
        //   关联对象写入要求最终 Criteria 明确限定唯一主实体 ID；
        //   update(entity) 由实体 ID 生成该 Criteria，update(entity, criteria)
        //   可直接在 Criteria 指定 ID；仅由其他条件匹配时只写根实体。
        //   对采用 MPJ SET 的关联模板，以请求提交的关联属性和 EntityAssociation
        //   关系元数据定位目标 JOIN 实例；不读取查询装配用的 resultProperty。
        //   Criteria 明确限定唯一主实体 ID 时，单个关联对象可作为统一 SET 模板，
        //   多个不同对象不传给同一条 MPJ SET。
        //   IGNORE_NULL：关联对象 -> wrapper.setUpdateEntity(...);
        //                主实体 -> mapper.updateJoin(entity, wrapper)。
        //   WRITE_NULL：关联对象和主实体仅对非框架维护字段写入 null；
        //               不能直接让 setUpdateEntityAndNull/updateJoinAndNull 覆盖框架维护字段。
        //   仅提交关联值、没有根实体业务属性时：只为关联目标构造 SET，
        //   以 mapper.updateJoin(null, wrapper) 执行，不写根实体的 null 字段。
        //   根实体参与 SET 且选择 WRITE_NULL 时，其全部非框架维护的可更新
        //   持久化字段均参与 SET，包括未提交而值为 null 的字段。
        //   空 SET、目标歧义、关系与 JOIN 不匹配均在执行前报错。
    }

    int deleteCountByCriteria(ICriteria<E, C> criteria) {
        // 修改：普通条件 -> WrapperUtil.getDeleteWrapper -> BaseMapper.delete。
        // JOIN/关系子查询 -> WrapperUtil.getJoinDeleteWrapper -> MPJBaseMapper.deleteJoin。
        //   仅供无需 NoCode 级联协调的直接条件删除；只删除匹配的根实体。
        //   不默认 deleteAll，也不把关联表作为本 SQL 的删除目标。
        // 无有效筛选报错，不能因不支持的条件被跳过而退化成全表删除。
    }

    int deleteCountByIds(Collection<? extends Serializable> ids) {
        // 保留：NoCode 先固定匹配的根实体 ID、完成级联后，按这些 ID 删除根实体。
    }

    // 保留：查询、分页、COUNT、EXISTS 和 ID 写入既有入口。
}
```

## 5. NoCode 关联协调与统一执行

`isass-nocode-core/src/main/java/vip/isass/framework/nocode/service/AssociationWriteCoordinator.java`

```java
final class AssociationWriteCoordinator {
    Map<Class<?>, ILocalCrudService<?, ?, ?>> services; // 保留。
    ThreadLocal<Integer> nesting;                        // 保留：抑制嵌套重复协调。

    beforeSave(IEntity<?> source, boolean creating, Serializable updateRootId,
               Set<String> handledByJoinUpdate) {
        // 修改：已由本次 MPJ SET 处理的关联属性不再执行单独的创建/更新；
        // 更新路径的 updateRootId 来自最终 Criteria，不要求 source.id 非空；
        // 创建路径维持现有 ID 生成与关联写入时序。
        // 其他关联保持现有归属检查和 MERGE/REPLACE 处理。
    }

    afterSave(IEntity<?> source, IUpdateCriteria<?> criteria,
              boolean creating, Serializable updateRootId,
              Set<String> handledByJoinUpdate) {
        // 修改：跳过已由 MPJ SET 处理的关联；
        // 更新路径用 updateRootId 定位根实体，不从 source.id 猜测写入范围。
        // 多个对象各自对应不同记录时，不加入同一条 UPDATE JOIN，
        // 按现有直接关联写入流程逐对象处理。
        // MANY 即使只提交一个对象，也按该对象 ID 更新或新增，不作为
        // 更新全部匹配子对象的统一模板；ONE 的单个对象可走关联 SET 模板。
        // MANY 的 MERGE 保留未提交目标；REPLACE 在逐对象保存后删除
        // 未提交的旧目标，不把清理动作并入 MPJ UPDATE。
    }

    beforeDelete(ILocalCrudService<?, ?, ?> service, Collection<? extends Serializable> ids) {
        // 修改：只沿 EntityAssociation.cascadeDelete=true 的方向处理；
        // 先按固定根 ID 定位全部应级联的子对象，而不只删除筛选 JOIN 命中的子行。
        // 按最深子表到根表的顺序分层、分批删除，每条 SQL 只删除一个目标表；
        // 直接外键关系使用普通批量删除，跨层定位才使用 MPJ JOIN 条件。
        // 必须经过目标服务删除生命周期、无法安全批量定位或有外键顺序约束时，
        // 保留现有逐层服务处理；所有步骤沿用同一事务。
        // 不使用 deleteAll() 或一条 SQL 同时删除多个表。
    }
}
```

`isass-nocode-core/src/main/java/vip/isass/framework/nocode/service/CrudWriteExecutor.java`

```java
final class CrudWriteExecutor {
    SuperCudResult superCud(ILocalCrudService<E, C, PK> service, SuperCudReq<E, C> request) {
        // 保留：统一事务、写生命周期和 add/update/delete 顺序。
        for (E entity : request.updateEntities()) {
            C criteria = effectiveUpdateCriteria(service, request.updateCriteria(), entity);
            // 修改：先检查更新实体的关联属性是否由请求提交。
            // 已提交关联属性时，最终 Criteria 必须明确限定唯一主实体 ID；
            // 仅凭其他条件运行时恰好匹配一条、或仅有 entity.id 非空但
            // 最终 Criteria 未以该 ID 收窄，均不能允许关联写入。
            // 不满足时在 beforeSave、Repository 及任何关联写入前报错。
            // Criteria 内的 JOIN/EXISTS 等关联筛选条件不算关联属性写入。
            PK updateRootId = validateAndResolveRelatedUpdateRootId(entity, criteria);
            // 修改：用请求提交的关联属性、EntityAssociation 和实体值识别
            // 单模板关联；不以查询装配用的 resultProperty 决定写入目标。
            // 明确唯一主实体 ID 且多个关联对象各有 ID 时，更新主实体后由
            // AssociationWriteCoordinator 按各关联对象 ID 分别更新。
            Set<String> handledByJoinUpdate = resolveHandledProperties(entity, criteria);
            associations.beforeSave(entity, false, updateRootId, handledByJoinUpdate);
            int count = repository.updateCountByCriteria(entity, criteria);
            associations.afterSave(entity, criteria, false, updateRootId, handledByJoinUpdate);
            result.updatedCount += count;
        }
        for (C criteria : request.deleteCriteria()) {
            // 修改：在删除子对象前先固定本次匹配的根实体 ID；
            // 空根行不能作为待删 ID，重复根 ID 只处理一次。
            List<PK> rootIds = resolveExistingRootIds(criteria);
            for (List<PK> batch : partition(rootIds)) {
                associations.beforeDelete(service, batch);
                // 关联筛选依赖的子行可能已被级联删除，不能再次按原 JOIN Criteria
                // 删除根实体；按先前固定的 ID 删除，deletedCount 只统计根实体。
                result.deletedCount += repository.deleteCountByIds(batch);
            }
        }
        // 保留 beforeExecute/afterExecute/afterCommit/afterRollback 顺序。
    }

    C effectiveUpdateCriteria(service, C source, E entity) {
        // 保留：update(entity) 未提供 Criteria 时，根据 entity.id 创建 ID 条件；
        // update(entity, criteria) 以最终 Criteria 为 WHERE 范围，实体 ID 不进入 SET。
        // 保留 matchFields 与无有效条件时的现有安全收窄语义；
        // 不因存在 JOIN 就视为已具备安全的 WHERE，也不让子查询条件被忽略。
    }

    PK validateAndResolveRelatedUpdateRootId(E entity, C criteria) {
        // 新增：使用现有关系元数据和请求属性出现性判断是否提交关联属性；
        // 未提交关联属性时返回 null，不限制 Criteria 条件更新。
        // 已提交关联属性时，从最终 Criteria 提取明确的唯一主实体 ID；
        // 没有这样的 ID 条件，或 OR 分支扩大匹配范围时抛参数异常。
        // 若 entity.id 也有值但与 Criteria 的 ID 不一致，执行前报错。
        // 返回 ID 供关联协调器使用，不要求修改 entity.id。
    }
}
```

## 6. 同步范围

- 调用方：`MybatisPlusRepository`、`CrudWriteExecutor`、`AssociationWriteCoordinator`、`WrapperUtil` 的测试和直接调用点；现有 `ICrudService.update/delete/superCud` 地址和请求字段保持不变。
- 文档：最外层 `returnFields` 仅用于查询、写入时不读取的规则已同步到 `docs/usage/nocode/association-query.md`；实现时继续同步该文档与 `docs/usage/nocode/crud-lifecycle.md`，删除“关联写入条件一律拒绝”“未提交字段永不参与 WRITE_NULL”以及条件对象保存瞬时列名、依赖 `ConditionApplier` 的旧规则；同步 `docs/60.changelog/ChangeLog4.x.md`。
- 测试：普通更新/删除不回退；关联 JOIN、相关 EXISTS/IN、嵌套条件及参数绑定；最外层 `returnFields` 非空或曾由查询自动补列时均不影响写入、子查询必需选列仍生效；`IGNORE_NULL/WRITE_NULL` 的主表与关联表写入，尤其验证业务字段的 null 写入及框架维护字段不被清空；同一 Criteria 复用时按当前 Wrapper 重新解析列别名且不改写条件对象；删除依赖 `getSourceColumn/getTargetColumn` 的断言，改为验证最终 SQL 与 JSON 契约；`update(entity)` 从实体 ID 生成 Criteria、`update(entity, criteria)` 从条件取得唯一主实体 ID、关联对象按各自 ID 逐对象更新且不重复执行；只有其他条件或 OR 扩大范围时即使运行时匹配一条也拒绝关联写入，实体 ID 与 Criteria ID 冲突时拒绝；仅以 JOIN/EXISTS 筛选根实体的条件更新可正常执行；空条件防全表写入，非默认排序、分页和计数设置明确拒绝；逻辑删除和级联；用独立基准查询与固定数据逐项验证已支持的 RIGHT/FULL/CROSS JOIN、fromCriteria、派生表和嵌套 JOIN 的命中根 ID，不支持的形态验证执行 SQL 前拒绝；涉及关联 SET 时同时核对目标表实例、记录 ID 与写入值，不依赖 UPDATE 影响行数代替匹配集合；MySQL 实库与隔离 JDBC 回归分别记录。
- 同类多次 JOIN 与自连接测试：仅作筛选时不要求 `resultProperty`；采用关联 SET 时验证请求关联属性与关系元数据定位到正确别名，无法唯一定位时在写入前报错；按关联对象 ID 逐对象更新不依赖 JOIN 别名。
- 级联删除测试：筛选 JOIN 只命中部分子对象时，删除选中根实体的全部 `cascadeDelete=true` 子对象；按固定根 ID 删除，不因先删子对象使原条件失效；`cascadeDelete=false` 的目标不删除；分层批量、需目标服务生命周期的回退、事务回滚与 `deletedCount` 仅计根实体均覆盖。
- 关联写入模式测试：仅提交关联值并选择 `WRITE_NULL` 时只产生关联表 SET，不清空主表；明确提交根实体业务属性时才让主表参与 SET，框架维护字段始终不被清空；MANY 单元素的 `MERGE` 更新已提交对象并保留其他对象，`REPLACE` 更新已提交对象并删除未提交旧对象，均不将单元素集合当作统一模板。
- 生成器：`mapper.java.ftl` 已生成 `MPJBaseMapper`，本轮核对即可，不修改数据源、DDL 或生成模板。
