// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.orm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.yulichang.config.ConfigProperties;
import com.github.yulichang.extension.apt.AptAbstractWrapper;
import com.github.yulichang.extension.apt.matedata.BaseColumn;
import com.github.yulichang.extension.apt.matedata.Column;
import com.github.yulichang.extension.apt.resultmap.MybatisLabel;
import com.github.yulichang.extension.apt.resultmap.MybatisLabelFree;
import com.github.yulichang.extension.apt.resultmap.Result;
import com.github.yulichang.query.MPJQueryWrapper;
import com.github.yulichang.toolkit.WrapperUtils;
import com.github.yulichang.toolkit.support.ColumnCache;
import com.github.yulichang.wrapper.DeleteJoinWrapper;
import com.github.yulichang.wrapper.JoinAbstractLambdaWrapper;
import com.github.yulichang.wrapper.JoinAbstractWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.github.yulichang.wrapper.UpdateJoinWrapper;
import com.github.yulichang.wrapper.interfaces.MConsumer;
import com.github.yulichang.wrapper.resultmap.Label;
import com.github.yulichang.wrapper.segments.SelectCache;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import vip.isass.framework.common.converter.ConvertUtil;
import vip.isass.framework.common.criteria.BaseCondition;
import vip.isass.framework.common.criteria.ConditionTraversal;
import vip.isass.framework.common.criteria.CriteriaEntityTypes;
import vip.isass.framework.common.criteria.CriteriaMetadata;
import vip.isass.framework.common.criteria.EmptyCriteria;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.IRelatedQueryCriteria;
import vip.isass.framework.common.criteria.JoinCondition;
import vip.isass.framework.common.criteria.JoinResultPropertyResolver;
import vip.isass.framework.common.criteria.JoinType;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.criteria.type.IOrderByCriteria;
import vip.isass.framework.common.criteria.type.IPageCriteria;
import vip.isass.framework.common.criteria.type.IReturnFieldCriteria;
import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.SensitiveDataProperty;
import vip.isass.framework.common.support.BeanProviderUtil;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.security.data.DataReadContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 将 Criteria 的普通条件、关联条件及投影统一转换为查询或写入 Wrapper。
 *
 * @author Rain
 */
public class WrapperUtil {

    private static final ClassValue<List<String>> DEFAULT_RETURN_FIELDS = new ClassValue<>() {
        /** 为实体缓存默认返回的非敏感 Java 属性名。 */
        @Override
        protected List<String> computeValue(Class<?> entityType) {
            return ColumnCache.getListField(entityType).stream()
                    .filter(SelectCache::isSelect)
                    .map(SelectCache::getColumProperty)
                    .filter(property -> !SensitiveDataProperty.PROPERTIES.contains(property))
                    .toList();
        }
    };

    /**
     * 普通条件与关联条件使用同一个查询 Wrapper，按 Criteria 内容逐步装配。
     *
     * @param criteria 实体查询条件
     * @return 包含筛选、关联和选列的查询 Wrapper
     */
    public static <E extends IEntity<E>, C extends ICriteria<E, C>> Wrapper<E> getQueryWrapper(ICriteria<E, C> criteria) {
        MpjWrapper<E> wrapper = new MpjWrapper<>(resolveEntityClass(criteria));
        getQueryWrapper(wrapper, criteria, null, MpjWrapper.QueryUsage.ENTITY,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        return wrapper;
    }

    /**
     * 创建计数或存在性查询使用的 Wrapper，不配置关联对象结果映射。
     *
     * @param criteria 待统计的实体查询条件
     * @return 计数查询 Wrapper
     */
    static <E extends IEntity<E>, C extends ICriteria<E, C>> Wrapper<E> getCountQueryWrapper(ICriteria<E, C> criteria) {
        MpjWrapper<E> wrapper = new MpjWrapper<>(resolveEntityClass(criteria));
        getQueryWrapper(wrapper, criteria, null, MpjWrapper.QueryUsage.COUNT,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        return wrapper;
    }

    /**
     * 受信任写授权条件：只选择指定记录的主键，不复用读取投影或读取字段限制。
     */
    static <E extends IEntity<E>> Wrapper<E> authorizationWrapper(Class<E> type, Serializable id,
                                                                  List<WhereCondition> conditions) {
        MpjWrapper<E> wrapper = new MpjWrapper<>(type);
        String key = TableInfoHelper.getTableInfo(type).getKeyProperty();
        wrapper.select(BaseColumnFactory.column(wrapper.getBaseColumn(), key));
        processWhereConditions(wrapper, List.of(WhereCondition.eq(key, id),
                WhereCondition.and(conditions.toArray(WhereCondition[]::new))), Condition.Logical.AND, null,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        return wrapper;
    }

    /**
     * 按 FROM、JOIN、WHERE、SELECT、ORDER BY 顺序编译当前查询层；子查询递归复用此入口。
     *
     * @param wrapper    当前查询的 Wrapper
     * @param criteria   当前查询条件
     * @param outer      相关子查询可引用的直接外层表；非相关查询为 null
     * @param usage      当前 Wrapper 的查询用途
     * @param activePath 本次构建正在消费的对象身份集合；退出当前节点时移除，允许独立分支复用对象
     * @return 当前层的 JOIN 信息及输出给外层的嵌套关联列
     */
    private static QueryBuildResult getQueryWrapper(MpjWrapper<?> wrapper,
                                               ICriteria<?, ?> criteria,
                                               BaseColumn<?> outer,
                                               MpjWrapper.QueryUsage usage,
                                               Set<Object> activePath) {
        enterQueryPath(criteria, activePath);
        try {
            validateQueryUsage(criteria, usage);
            if (DataReadContext.current() != null && criteria instanceof IWhereConditionCriteria<?, ?> where) {
                Set<Object> inputs = Collections.newSetFromMap(new IdentityHashMap<>());
                for (WhereCondition node : where.getWhereConditions()) {
                    validateQueryInput(node, wrapper.getEntityClass(), outer, inputs);
                }
            }

            // FROM：确定当前根表输入，子查询使用独立的别名作用域。
            processFrom(wrapper, criteria, activePath);

            // JOIN：建立连接和 ON 条件，收集 SELECT 阶段需要的关联信息。
            List<JoinedTarget> joinedTargets = processJoin(wrapper, criteria, activePath);

            // WHERE：消费条件树，遇到目标 Criteria 时递归构建相关子查询。
            if (criteria instanceof IWhereConditionCriteria<?, ?> where) {
                if (DataReadContext.current() != null && !where.getWhereConditions().isEmpty()) {
                    wrapper.nested(group -> processWhereConditions((MpjWrapper<?>) group,
                            where.getWhereConditions(), Condition.Logical.AND, outer, activePath));
                } else {
                    processWhereConditions(wrapper, where.getWhereConditions(), Condition.Logical.AND, outer, activePath);
                }
            }
            if (DataReadContext.current() != null) {
                List<WhereCondition> restrictions = DataReadContext.current().conditions(wrapper.getEntityClass());
                if (!restrictions.isEmpty()) {
                    wrapper.and(group -> processWhereConditions((MpjWrapper<?>) group,
                            restrictions, Condition.Logical.AND, null, activePath));
                }
            }

            // SELECT：按用途一次确定返回字段、派生表输出和 MPJ 关联结果映射。
            List<DerivedField> derivedFields = processSelect(wrapper, criteria, joinedTargets, usage);

            // ORDER BY：追加本查询层的排序。
            processOrderBy(wrapper, criteria);
            return new QueryBuildResult(joinedTargets, derivedFields);
        } finally {
            activePath.remove(criteria);
        }
    }

    /**
     * 只校验调用者提供的条件树；策略自身的范围字段仍可作为受信任的 SQL 约束。
     */
    private static void validateQueryInput(WhereCondition node, Class<?> type,
                                            BaseColumn<?> outer, Set<Object> activePath) {
        enterQueryPath(node, activePath);
        try {
            if (node.getChildren() != null) {
                for (WhereCondition child : node.getChildren()) {
                    validateQueryInput(child, type, outer, activePath);
                }
            } else {
                if (node.getSourceProperty() != null) {
                    Class<?> source = node.getTargetProperty() != null && outer != null ? outer.getColumnClass() : type;
                    DataReadContext.current().requireQueryable(source, resolveSourceProperty(node));
                }
                if (node.getTargetProperty() != null) {
                    DataReadContext.current().requireQueryable(type, node.getTargetProperty());
                }
            }
        } finally {
            activePath.remove(node);
        }
    }

    /**
     * 按对象身份检测当前递归路径中的循环引用，不扫描未消费的分支或复制 Criteria。
     */
    private static void enterQueryPath(Object node, Set<Object> activePath) {
        Objects.requireNonNull(node, "查询条件节点不能为 null");
        if (!activePath.add(node)) {
            throw new IllegalArgumentException("查询条件存在循环引用: " + node.getClass().getSimpleName());
        }
    }

    /**
     * 在消费当前查询层时校验嵌套分页，以及 WHERE 子查询不支持的排序和关联加载。
     */
    private static void validateQueryUsage(ICriteria<?, ?> criteria, MpjWrapper.QueryUsage usage) {
        if (usage != MpjWrapper.QueryUsage.ENTITY && usage != MpjWrapper.QueryUsage.COUNT) {
            validateNestedPage(criteria);
        }
        if (usage == MpjWrapper.QueryUsage.EXISTS || usage == MpjWrapper.QueryUsage.SINGLE) {
            if (criteria instanceof IOrderByCriteria<?, ?> order
                    && order.getOrderBy() != null && !order.getOrderBy().isBlank()) {
                throw new IllegalArgumentException("WHERE 子查询不接受 orderBy");
            }
            if (criteria instanceof IRelatedQueryCriteria<?, ?> related && !related.getLoadRelated().isEmpty()) {
                throw new IllegalArgumentException("WHERE 子查询不装配 loadRelated");
            }
        }
    }

    /**
     * 编译显式 FROM 输入；RIGHT/FULL JOIN 的逻辑删除根表也先作为过滤后的 FROM 输入。
     */
    private static void processFrom(MpjWrapper<?> wrapper, ICriteria<?, ?> criteria, Set<Object> activePath) {
        if (criteria instanceof FullTypeCriteria<?, ?> full && full.getFromCriteria() != null) {
            ICriteria<?, ?> from = full.getFromCriteria();
            if (CriteriaEntityTypes.entityClass(from) != wrapper.getEntityClass()) {
                throw new IllegalArgumentException("fromCriteria 必须与当前 Criteria 的实体一致");
            }
            MpjWrapper<?> input = wrapper.subquery(wrapper.getEntityClass(), false);
            getQueryWrapper(input, from, null, MpjWrapper.QueryUsage.FROM, activePath);
            wrapper.from(input);
            return;
        }
        if (criteria instanceof IRelatedQueryCriteria<?, ?> related
                && wrapper.getLogicSql() && TableInfoHelper.getTableInfo(wrapper.getEntityClass()).isWithLogicDelete()
                && related.getJoinConditions().stream().anyMatch(join -> {
            Objects.requireNonNull(join, "joinConditions 不能包含 null");
            return join.getJoinType() == JoinType.RIGHT || join.getJoinType() == JoinType.FULL;
        })) {
            MpjWrapper<?> input = wrapper.subquery(wrapper.getEntityClass(), false);
            getQueryWrapper(input, EmptyCriteria.of(resolveEntityClass(criteria)), null,
                    MpjWrapper.QueryUsage.FROM, activePath);
            wrapper.from(input);
        }
    }

    /**
     * 编译当前查询层的所有 JOIN；嵌套连接由目标 Criteria 的递归构建处理。
     */
    private static List<JoinedTarget> processJoin(MpjWrapper<?> wrapper, ICriteria<?, ?> criteria,
                                                  Set<Object> activePath) {
        if (!(criteria instanceof IRelatedQueryCriteria<?, ?> related) || related.getJoinConditions().isEmpty()) {
            return List.of();
        }
        wrapper.disableSubLogicDel();
        List<JoinedTarget> joinedTargets = new ArrayList<>(related.getJoinConditions().size());
        for (JoinCondition join : related.getJoinConditions()) {
            joinedTargets.add(processJoin(wrapper, Objects.requireNonNull(join, "joinConditions 不能包含 null"), activePath));
        }
        return joinedTargets;
    }

    /**
     * 从当前 Criteria 读取 WHERE 条件并开始本层条件树的消费。
     */
    private static void processWhereConditions(MpjWrapper<?> wrapper, ICriteria<?, ?> criteria,
                                               BaseColumn<?> outer, Set<Object> activePath) {
        if (criteria instanceof IWhereConditionCriteria<?, ?> where) {
            processWhereConditions(wrapper, where.getWhereConditions(), Condition.Logical.AND, outer, activePath);
        }
    }

    /**
     * 按查询用途配置全部选列和关联结果映射；派生表一次输出外层需要的列。
     *
     * @return JOIN 派生表输出的嵌套关联列；其他用途返回空集合
     */
    private static List<DerivedField> processSelect(MpjWrapper<?> wrapper,
                                                    ICriteria<?, ?> criteria,
                                                    List<JoinedTarget> joinedTargets,
                                                    MpjWrapper.QueryUsage usage) {
        if (usage == MpjWrapper.QueryUsage.FROM || usage == MpjWrapper.QueryUsage.JOIN) {
            wrapper.selectAll();
            return usage == MpjWrapper.QueryUsage.JOIN ? selectNestedJoinColumns(wrapper, joinedTargets) : List.of();
        }
        if (usage == MpjWrapper.QueryUsage.EXISTS) {
            wrapper.select("1");
            return List.of();
        }
        if (usage == MpjWrapper.QueryUsage.ENTITY) {
            for (JoinedTarget joined : joinedTargets) {
                wrapper.addLabel(createJoinResultMapping(wrapper.getEntityClass(), joined),
                        Collection.class.isAssignableFrom(resolveJoinResultField(wrapper.getEntityClass(), joined.condition()).getType()));
            }
        }
        Collection<String> returnFields = criteria instanceof IReturnFieldCriteria<?, ?> fields
                ? fields.getReturnFields() : null;
        if (usage == MpjWrapper.QueryUsage.SINGLE && (returnFields == null || returnFields.size() != 1)) {
            throw new IllegalArgumentException("IN/NOT_IN 子查询必须显式选择且只选择一个字段");
        }
        if (usage == MpjWrapper.QueryUsage.SINGLE && DataReadContext.current() != null) {
            DataReadContext.current().requireQueryable(wrapper.getEntityClass(), returnFields.iterator().next());
        }
        boolean entityResult = usage == MpjWrapper.QueryUsage.ENTITY;
        boolean rootIdentity = !joinedTargets.isEmpty()
                && (entityResult || usage == MpjWrapper.QueryUsage.COUNT);
        if (entityResult || rootIdentity) {
            returnFields = resolveReturnFields(criteria, wrapper.getEntityClass(), rootIdentity, entityResult);
        } else if (returnFields == null || returnFields.isEmpty()) {
            returnFields = DEFAULT_RETURN_FIELDS.get(wrapper.getEntityClass());
        }
        String rootKey = rootIdentity ? TableInfoHelper.getTableInfo(wrapper.getEntityClass()).getKeyProperty() : null;
        for (String property : returnFields) {
            if (property.equals(rootKey)) {
                wrapper.selectAs(BaseColumnFactory.column(wrapper.getBaseColumn(), property), property);
            } else {
                wrapper.select(BaseColumnFactory.column(wrapper.getBaseColumn(), property));
            }
        }
        return List.of();
    }

    /**
     * 确定要返回的 Java 属性名；按需补主键，并可将默认属性及主键写回调用方的 Criteria。
     *
     * @param criteria       返回字段条件
     * @param entityType     实体类型
     * @param includeId      是否补齐实体主键
     * @param updateCriteria 是否将补齐的属性写回 Criteria
     * @return 最终要返回的 Java 属性名
     */
    static Collection<String> resolveReturnFields(ICriteria<?, ?> criteria, Class<?> entityType,
                                                  boolean includeId, boolean updateCriteria) {
        IReturnFieldCriteria<?, ?> fields = criteria instanceof IReturnFieldCriteria<?, ?> value ? value : null;
        Collection<String> returnFields = fields == null ? null : fields.getReturnFields();
        if (returnFields == null || returnFields.isEmpty()) {
            List<String> defaults = DEFAULT_RETURN_FIELDS.get(entityType);
            if (fields != null && updateCriteria) {
                fields.getReturnFields().addAll(defaults);
                returnFields = fields.getReturnFields();
            } else {
                returnFields = defaults;
            }
        }
        if (includeId) {
            String id = TableInfoHelper.getTableInfo(entityType).getKeyProperty();
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("JOIN 实体装配需要主键: " + entityType.getName());
            }
            if (!returnFields.contains(id)) {
                if (fields != null && updateCriteria) {
                    fields.addReturnField(id);
                    returnFields = fields.getReturnFields();
                } else {
                    Collection<String> result = new ArrayList<>(returnFields);
                    result.add(id);
                    returnFields = result;
                }
            }
        }
        if (DataReadContext.current() != null) {
            Collection<String> withPolicyKeys = new LinkedHashSet<>(returnFields);
            withPolicyKeys.addAll(DataReadContext.current().requiredProperties(entityType));
            return withPolicyKeys;
        }
        return returnFields;
    }

    /**
     * 对尚未指定选列的普通查询 Wrapper 设置非敏感字段。
     */
    static <E> void applyDefaultSelectColumns(Wrapper<E> wrapper, Class<E> entityType) {
        if (wrapper instanceof QueryWrapper<E> query && wrapper.getSqlSelect() == null) {
            query.select(entityType, field -> !SensitiveDataProperty.PROPERTIES.contains(field.getProperty()));
        }
    }

    /**
     * 查找允许业务写入的字段，排除自动填充、逻辑删除和版本字段。
     */
    static List<TableFieldInfo> resolveWritableBusinessColumns(Class<?> entityType) {
        return TableInfoHelper.getTableInfo(entityType).getFieldList().stream()
                .filter(field -> field.getFieldFill() == FieldFill.DEFAULT)
                .filter(field -> field.getUpdateStrategy() != FieldStrategy.NEVER)
                .filter(field -> !field.isLogicDelete() && !field.isVersion())
                .toList();
    }

    /**
     * 将 Criteria 中的排序属性转换为当前查询表的 ORDER BY 列。
     */
    private static void processOrderBy(MpjWrapper<?> wrapper, ICriteria<?, ?> criteria) {
        if (criteria instanceof IOrderByCriteria<?, ?> order && order.getOrderBy() != null) {
            for (String part : order.getOrderBy().split(",")) {
                if (part.isBlank()) {
                    continue;
                }
                String[] parts = part.trim().split("\\s+");
                if (DataReadContext.current() != null) {
                    DataReadContext.current().requireQueryable(wrapper.getEntityClass(), parts[0]);
                }
                if (parts.length > 2 || parts.length == 2
                        && !parts[1].equalsIgnoreCase("ASC") && !parts[1].equalsIgnoreCase("DESC")) {
                    throw new IllegalArgumentException("orderBy 参数错误: " + part);
                }
                wrapper.orderBy(true, parts.length == 1 || parts[1].equalsIgnoreCase("ASC"),
                        BaseColumnFactory.column(wrapper.getBaseColumn(), parts[0]));
            }
        }
    }

    /**
     * 编译一个 JOIN；目标有筛选或嵌套关联时先生成目标派生表。
     *
     * @return JOIN 表实例及嵌套映射信息
     */
    private static JoinedTarget processJoin(MpjWrapper<?> wrapper, JoinCondition join, Set<Object> activePath) {
        Objects.requireNonNull(join.getJoinType(), "JOIN 缺少 joinType");
        ICriteria<?, ?> target = Objects.requireNonNull(join.getTargetCriteria(), "JOIN 缺少 targetCriteria");
        validateNestedPage(target);
        if (join.getJoinType() == JoinType.FULL) {
            String databaseId = TableInfoHelper.getTableInfo(wrapper.getEntityClass()).getConfiguration().getDatabaseId();
            if (databaseId == null || !List.of("postgresql", "oracle", "sqlserver", "dm", "kingBase", "Highgo").contains(databaseId)) {
                throw new UnsupportedOperationException("当前方言不支持或尚未确认 FULL JOIN: " + databaseId);
            }
        }
        Class<?> type = CriteriaEntityTypes.entityClass(target);
        BaseColumn<?> table = BaseColumnFactory.create(type);
        boolean derived = hasDerivedTableConditions(target)
                || TableInfoHelper.getTableInfo(type).isWithLogicDelete()
                || DataReadContext.current() != null && !DataReadContext.current().conditions(type).isEmpty();
        String inputSql;
        MpjWrapper<?> input;
        List<JoinedTarget> nested = List.of();
        List<DerivedField> derivedFields = List.of();
        if (derived) {
            input = wrapper.subquery(type, false);
            QueryBuildResult result = getQueryWrapper(input, target, null, MpjWrapper.QueryUsage.JOIN, activePath);
            nested = result.joinedTargets();
            derivedFields = result.derivedFields();
            inputSql = input.querySql();
        } else {
            input = null;
            inputSql = null;
        }
        if (join.getJoinType() == JoinType.CROSS && (join.getCondition() != null || join.getChildren() != null
                || join.getSourceProperty() != null || join.getTargetProperty() != null || join.getValue() != null)) {
            throw new IllegalArgumentException("CROSS JOIN 不接受 ON 条件");
        }
        wrapper.join(join.getJoinType().name() + " JOIN", table, on -> {
            if (inputSql != null) {
                on.setTableName(ignored -> "(" + inputSql + ")");
            }
            if (join.getJoinType() != JoinType.CROSS) {
                processJoinCondition((MpjWrapper<?>) on, join, wrapper.getBaseColumn(), table, activePath);
            }
            return on;
        });
        return new JoinedTarget(join, table, nested, derivedFields);
    }

    /**
     * 解析 JOIN 结果在源实体中的接收字段。
     */
    private static Field resolveJoinResultField(Class<?> sourceType, JoinCondition join) {
        String property = JoinResultPropertyResolver.resolve(sourceType, join).property();
        Field field = CriteriaMetadata.field(sourceType, property);
        if (field == null) {
            throw new IllegalArgumentException("JOIN 结果字段不存在: " + sourceType.getName() + "." + property);
        }
        return field;
    }

    /**
     * 校验接收字段的单体类型或集合元素类型是否可容纳 JOIN 目标。
     */
    private static void validateJoinResultField(Field field, Class<?> sourceType, Class<?> targetType) {
        String property = field.getName();
        boolean collection = Collection.class.isAssignableFrom(field.getType());
        if (collection) {
            Class<?> elementType = CriteriaMetadata.elementType(sourceType, property);
            if (elementType == null || !elementType.isAssignableFrom(targetType)) {
                throw new IllegalArgumentException("JOIN 集合元素类型不匹配: " + property);
            }
            if (!field.getType().isAssignableFrom(ArrayList.class)
                    && !field.getType().isAssignableFrom(LinkedHashSet.class)) {
                throw new IllegalArgumentException("不支持的 JOIN 集合字段类型: " + property);
            }
        } else if (!field.getType().isAssignableFrom(targetType)) {
            throw new IllegalArgumentException("JOIN 结果字段类型不匹配: " + property);
        }
    }

    /**
     * 记录嵌套关联列在派生表中的路径、属性、输出别名与映射信息。
     */
    private record DerivedField(List<Integer> path, String property, String alias, SelectCache column) {
    }

    /**
     * 记录一次 JOIN 的表实例、目标条件及嵌套关联。
     */
    private record JoinedTarget(JoinCondition condition,
                                BaseColumn<?> table,
                                List<JoinedTarget> nested,
                                List<DerivedField> derivedFields) {
    }

    /**
     * 保存当前查询构建产生的 JOIN 信息和派生表输出列，供外层查询继续构建。
     */
    private record QueryBuildResult(List<JoinedTarget> joinedTargets, List<DerivedField> derivedFields) {
    }

    /**
     * 将嵌套 JOIN 的结果列输出到当前派生表，保留供外层映射使用的路径和别名。
     *
     * @return 当前派生表输出的嵌套关联列
     */
    private static List<DerivedField> selectNestedJoinColumns(MpjWrapper<?> wrapper, List<JoinedTarget> nested) {
        List<DerivedField> fields = new ArrayList<>();
        for (int branch = 0; branch < nested.size(); branch++) {
            JoinedTarget joined = nested.get(branch);
            Class<?> type = joined.table().getColumnClass();
            for (String property : resolveReturnFields(joined.condition().getTargetCriteria(), type, true, false)) {
                String alias = "__isass_related_" + fields.size();
                wrapper.selectAs(BaseColumnFactory.column(joined.table(), property), alias);
                fields.add(new DerivedField(List.of(branch), property, alias,
                        ColumnCache.getMapField(type).get(property)));
            }
            for (DerivedField descendant : joined.derivedFields()) {
                String alias = "__isass_related_" + fields.size();
                wrapper.derivedColumn(joined.table(), descendant.alias(), alias, descendant.column());
                List<Integer> path = new ArrayList<>();
                path.add(branch);
                path.addAll(descendant.path());
                fields.add(new DerivedField(path, descendant.property(), alias, descendant.column()));
            }
        }
        return fields;
    }

    /**
     * 为直接 JOIN 目标创建 MPJ 的单体或集合结果映射。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Label<?> createJoinResultMapping(Class<?> sourceType, JoinedTarget joined) {
        JoinCondition join = joined.condition();
        BaseColumn<?> table = joined.table();
        Field field = resolveJoinResultField(sourceType, join);
        Class<?> targetType = table.getColumnClass();
        validateJoinResultField(field, sourceType, targetType);
        MybatisLabel.Builder builder = new MybatisLabel.Builder(field.getName(), table, field.getType(), targetType, false);
        for (String selected : resolveReturnFields(join.getTargetCriteria(), targetType, true, false)) {
            Column column = BaseColumnFactory.column(table, selected);
            if (ColumnCache.getMapField(targetType).get(selected).isPk()) {
                builder.id(column);
            } else {
                builder.result(column);
            }
        }
        for (int branch = 0; branch < joined.nested().size(); branch++) {
            builder.build().getMybatisLabels().add(createNestedJoinResultMapping(targetType, table, joined.nested().get(branch),
                    joined.derivedFields(), List.of(branch)));
        }
        return builder.build();
    }

    /**
     * 根据派生表输出的列别名，递归创建更深层关联的结果映射。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Label<?> createNestedJoinResultMapping(Class<?> sourceType, BaseColumn<?> outerTable, JoinedTarget joined,
                                         List<DerivedField> projected, List<Integer> path) {
        Field field = resolveJoinResultField(sourceType, joined.condition());
        Class<?> targetType = joined.table().getColumnClass();
        validateJoinResultField(field, sourceType, targetType);
        MybatisLabelFree.Builder builder = new MybatisLabelFree.Builder(field.getName(), field.getType(), targetType);
        String outerKey = TableInfoHelper.getTableInfo(outerTable.getColumnClass()).getKeyProperty();
        // MPJ Free.Builder 在 build 时要求至少一个字段；先用根主键初始化，再换成派生表的实际投影。
        MybatisLabelFree<?> label = builder.id(BaseColumnFactory.column(outerTable, outerKey)).build();
        label.getResultList().clear();
        for (DerivedField projection : projected) {
            if (!projection.path().equals(path)) {
                continue;
            }
            SelectCache original = projection.column();
            TableFieldInfo fieldInfo = original.isPk() ? null
                    : TableInfoHelper.getTableInfo(original.getClazz()).getFieldList().stream()
                    .filter(info -> info.getProperty().equals(projection.property())).findFirst().orElse(null);
            SelectCache exposed = new SelectCache(original.getClazz(), original.isPk(), projection.alias(),
                    original.getColumnType(), projection.property(), true, fieldInfo);
            label.getResultList().add(new Result.Builder(original.isPk(), outerTable, exposed).build());
        }
        for (int branch = 0; branch < joined.nested().size(); branch++) {
            List<Integer> childPath = new ArrayList<>(path);
            childPath.add(branch);
            label.getMybatisLabels().add(createNestedJoinResultMapping(targetType, outerTable, joined.nested().get(branch),
                    projected, childPath));
        }
        return label;
    }

    /**
     * 判断目标 Criteria 是否含有需要先构建派生表的条件或排序。
     */
    private static boolean hasDerivedTableConditions(ICriteria<?, ?> criteria) {
        return criteria instanceof IWhereConditionCriteria<?, ?> where && !where.getWhereConditions().isEmpty()
                || criteria instanceof IRelatedQueryCriteria<?, ?> related && !related.getJoinConditions().isEmpty()
                || criteria instanceof FullTypeCriteria<?, ?> full && full.getFromCriteria() != null
                || criteria instanceof IOrderByCriteria<?, ?> order && order.getOrderBy() != null && !order.getOrderBy().isBlank();
    }

    /**
     * 递归编译查询 JOIN 的 ON 条件，包括分组、双字段比较和标量比较。
     */
    private static void processJoinCondition(MpjWrapper<?> wrapper, BaseCondition<?> node,
                                             BaseColumn<?> source, BaseColumn<?> target, Set<Object> activePath) {
        enterQueryPath(node, activePath);
        try {
            if (!(node instanceof JoinCondition) && node.getTargetCriteria() != null) {
                throw new IllegalArgumentException("ON 子条件共用 JOIN 目标，不重复声明 targetCriteria");
            }
            if (node.getChildren() != null) {
                if (node.getChildren().isEmpty() || node.getSourceProperty() != null
                        || node.getTargetProperty() != null || node.getValue() != null) {
                    throw new IllegalArgumentException("JOIN 条件分组不能为空或同时声明比较字段");
                }
                if (node.getCondition() == Condition.Logical.NOT) {
                    if (node.getChildren().size() != 1) {
                        throw new IllegalArgumentException("NOT 需要一个子表达式");
                    }
                    wrapper.not(group -> processJoinCondition((MpjWrapper<?>) group,
                            node.getChildren().get(0), source, target, activePath));
                } else {
                    wrapper.nested(group -> ConditionTraversal.consume(node.getChildren(), node.getCondition(), (link, child) -> {
                        if (link == Condition.Logical.OR) {
                            group.or();
                        }
                        processJoinCondition((MpjWrapper<?>) group, child, source, target, activePath);
                    }));
                }
            } else {
                if (DataReadContext.current() != null) {
                    if (node.getSourceProperty() != null) {
                        DataReadContext.current().requireQueryable(source.getColumnClass(), node.getSourceProperty());
                    }
                    if (node.getTargetProperty() != null) {
                        DataReadContext.current().requireQueryable(target.getColumnClass(), node.getTargetProperty());
                    }
                }
                if (node.getSourceProperty() != null && node.getTargetProperty() != null) {
                    if (node.getValue() != null) {
                        throw new IllegalArgumentException("JOIN ON 双字段比较不能同时声明 value");
                    }
                    applyColumnComparison(wrapper, node, BaseColumnFactory.column(source, node.getSourceProperty()),
                            BaseColumnFactory.column(target, node.getTargetProperty()));
                } else {
                    boolean sourceSide = node.getSourceProperty() != null;
                    WhereCondition scalar = new WhereCondition(sourceSide ? node.getSourceProperty() : node.getTargetProperty(),
                            node.getCondition(), node.getValue());
                    validateScalar(scalar);
                    BaseColumn<?> table = sourceSide ? source : target;
                    applyAptCondition(wrapper, scalar, table, null);
                }
            }
        } finally {
            activePath.remove(node);
        }
    }

    /**
     * 按条件树中的连接符将一组 WHERE 条件追加到 MPJ 查询 Wrapper。
     */
    private static void processWhereConditions(MpjWrapper<?> wrapper,
                                               List<WhereCondition> whereConditions,
                                               Condition connector,
                                               BaseColumn<?> outer,
                                               Set<Object> activePath) {
        ConditionTraversal.consume(whereConditions, connector, (link, node) -> {
            if (link == Condition.Logical.OR) {
                wrapper.or();
            }
            processWhereCondition(wrapper, node, outer, activePath);
        });
    }

    /**
     * 处理单个 WHERE 节点，区分分组、子查询、外层字段比较和标量条件。
     */
    private static void processWhereCondition(MpjWrapper<?> wrapper, WhereCondition whereCondition,
                                              BaseColumn<?> outer, Set<Object> activePath) {
        enterQueryPath(whereCondition, activePath);
        try {
            if (whereCondition.getChildren() != null) {
                if (whereCondition.getChildren().isEmpty() || whereCondition.getSourceProperty() != null || whereCondition.getTargetProperty() != null
                        || whereCondition.getTargetCriteria() != null || whereCondition.getValue() != null) {
                    throw new IllegalArgumentException("条件分组不能为空或同时声明比较字段/目标");
                }
                if (whereCondition.getCondition() == Condition.Logical.NOT) {
                    if (whereCondition.getChildren().size() != 1) {
                        throw new IllegalArgumentException("NOT 需要一个子表达式");
                    }
                    wrapper.not(group -> processWhereConditions((MpjWrapper<?>) group,
                            whereCondition.getChildren(), Condition.Logical.AND, outer, activePath));
                } else {
                    wrapper.nested(group -> processWhereConditions((MpjWrapper<?>) group,
                            whereCondition.getChildren(), whereCondition.getCondition(), outer, activePath));
                }
                return;
            }
            Objects.requireNonNull(whereCondition.getCondition(), "条件缺少 condition");
            if (whereCondition.getTargetCriteria() != null) {
                applyAptCondition(wrapper, whereCondition, wrapper.getBaseColumn(),
                        condition -> buildWhereSubquerySql(wrapper, (WhereCondition) condition, activePath));
            } else if (whereCondition.getTargetProperty() != null) {
                if (outer == null || whereCondition.getValue() != null) {
                    throw new IllegalArgumentException("双字段比较需要直接外层查询，且不能同时声明 value");
                }
                applyColumnComparison(wrapper, whereCondition,
                        BaseColumnFactory.column(outer, whereCondition.getSourceProperty()),
                        BaseColumnFactory.column(wrapper.getBaseColumn(), whereCondition.getTargetProperty()));
            } else {
                validateScalar(whereCondition);
                applyAptCondition(wrapper, whereCondition, wrapper.getBaseColumn(), null);
            }
        } finally {
            activePath.remove(whereCondition);
        }
    }

    /**
     * 校验并生成 EXISTS 或 IN 条件使用的相关子查询 SQL。
     */
    private static String buildWhereSubquerySql(MpjWrapper<?> wrapper, WhereCondition node, Set<Object> activePath) {
        Condition op = node.getCondition();
        boolean exists = op == Condition.Existence.EXISTS || op == Condition.Existence.NOT_EXISTS;
        if (!exists && op != Condition.Membership.IN && op != Condition.Membership.NOT_IN) {
            throw new IllegalArgumentException("targetCriteria 只允许 EXISTS/NOT_EXISTS/IN/NOT_IN");
        }
        if (node.getValue() != null || node.getTargetProperty() != null || exists && node.getSourceProperty() != null) {
            throw new IllegalArgumentException("子查询条件不能同时声明标量或双字段比较");
        }
        MpjWrapper<?> child = wrapper.subquery(CriteriaEntityTypes.entityClass(node.getTargetCriteria()), true);
        getQueryWrapper(child, node.getTargetCriteria(), wrapper.getBaseColumn(),
                exists ? MpjWrapper.QueryUsage.EXISTS : MpjWrapper.QueryUsage.SINGLE, activePath);
        return child.querySql();
    }

    /**
     * 使用 MPJ 字段对象构建两个字段之间的比较条件。
     */
    private static void applyColumnComparison(MpjWrapper<?> wrapper, BaseCondition<?> node, Column source, Column target) {
        switch (node.getCondition().name()) {
            case "EQUAL" -> wrapper.eq(source, target);
            case "NOT_EQUAL" -> wrapper.ne(source, target);
            case "GREATER_THAN" -> wrapper.gt(source, target);
            case "GREATER_THAN_EQUAL" -> wrapper.ge(source, target);
            case "LESS_THAN" -> wrapper.lt(source, target);
            case "LESS_THAN_EQUAL" -> wrapper.le(source, target);
            default -> throw new IllegalArgumentException("不支持的双字段比较: " + node.getCondition());
        }
    }

    /**
     * 拒绝嵌套 Criteria 中非默认的分页与计数参数。
     */
    private static void validateNestedPage(ICriteria<?, ?> criteria) {
        if (criteria instanceof IPageCriteria<?, ?> page
                && (!Objects.equals(page.getPageNum(), IPageCriteria.DEFAULT_PAGE_NUM)
                || !Objects.equals(page.getPageSize(), IPageCriteria.DEFAULT_PAGE_SIZE)
                || !Objects.equals(page.getSearchCountFlag(), IPageCriteria.DEFAULT_SEARCH_COUNT_FLAG))) {
            throw new IllegalArgumentException("嵌套 Criteria 不支持分页；仅允许默认分页值");
        }
    }

    /**
     * 根据不含关联条件的 Criteria 创建普通更新 Wrapper。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends IEntity<E>, C extends ICriteria<E, C>> UpdateWrapper<E> getUpdateWrapper(ICriteria<E, C> criteria) {
        return createPlainWriteWrapper(criteria);
    }

    /**
     * 校验写入条件，并将普通 WHERE 条件编译为 MP 更新 Wrapper。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends IEntity<E>, C extends ICriteria<E, C>> UpdateWrapper<E> createPlainWriteWrapper(ICriteria<E, C> criteria) {
        validateWriteOptions(criteria);
        validatePlainWriteCriteria(criteria);
        Class<E> entityClass = resolveEntityClass(criteria);
        UpdateWrapper<E> wrapper = new UpdateWrapper<>();
        wrapper.setEntityClass(entityClass);

        if (criteria instanceof IWhereConditionCriteria) {
            processWhereConditionCriteria(wrapper, (IWhereConditionCriteria) criteria);
        }

        return wrapper;
    }

    /**
     * 创建供普通删除使用、仅包含筛选条件的 MP Wrapper。
     */
    public static <E extends IEntity<E>, C extends ICriteria<E, C>> UpdateWrapper<E> getDeleteWrapper(ICriteria<E, C> criteria) {
        return createPlainWriteWrapper(criteria);
    }

    /**
     * 根据关联筛选条件创建 MPJ 连表更新 Wrapper。
     */
    public static <E extends IEntity<E>, C extends ICriteria<E, C>> UpdateJoinWrapper<E> getJoinUpdateWrapper(ICriteria<E, C> criteria) {
        UpdateJoinWrapper<E> wrapper = new CriteriaUpdateJoinWrapper<>(resolveEntityClass(criteria));
        processJoinWriteCriteria(wrapper, criteria);
        return wrapper;
    }

    /**
     * 根据关联筛选条件创建 MPJ 连表删除 Wrapper。
     */
    public static <E extends IEntity<E>, C extends ICriteria<E, C>> DeleteJoinWrapper<E> getJoinDeleteWrapper(ICriteria<E, C> criteria) {
        DeleteJoinWrapper<E> wrapper = new CriteriaDeleteJoinWrapper<>(resolveEntityClass(criteria));
        processJoinWriteCriteria(wrapper, criteria);
        return wrapper;
    }

    /**
     * 编译写入 JOIN 与 WHERE，并要求根查询包含有效 WHERE 条件。
     */
    private static <E, W extends JoinAbstractLambdaWrapper<E, W>> void processJoinWriteCriteria(
            W wrapper, ICriteria<?, ?> criteria) {
        validateWriteOptions(criteria);
        if (criteria instanceof FullTypeCriteria<?, ?> full && full.getFromCriteria() != null) {
            throw new UnsupportedOperationException("写入不支持 fromCriteria");
        }
        if (criteria instanceof IRelatedQueryCriteria<?, ?> related) {
            int index = 0;
            for (JoinCondition join : related.getJoinConditions()) {
                processWriteJoin(wrapper, join, index++);
            }
        }
        if (criteria instanceof IWhereConditionCriteria<?, ?> where) {
            processWriteWhereConditions(wrapper, where.getWhereConditions(), Condition.Logical.AND,
                    resolveEntityClass(criteria), wrapper.getAlias(), null, null,
                    (WriteSubqueryProvider) wrapper);
        }
        if (wrapper.getExpression().getNormal().isEmpty()) {
            throw new IllegalArgumentException("关联更新/删除必须包含有效的主查询 WHERE 条件");
        }
    }

    /**
     * 编译一项写入 JOIN；仅接受 LEFT/INNER，并按目标条件生成派生表。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void processWriteJoin(JoinAbstractLambdaWrapper<?, ?> wrapper, JoinCondition join, int index) {
        JoinType type = Objects.requireNonNull(join.getJoinType(), "JOIN 缺少 joinType");
        if (type != JoinType.LEFT && type != JoinType.INNER) {
            throw new UnsupportedOperationException("写入 JOIN[" + index + "] 不支持 " + type);
        }
        ICriteria<?, ?> target = Objects.requireNonNull(join.getTargetCriteria(), "JOIN 缺少 targetCriteria");
        validateNestedPage(target);
        validateWriteOptions(target);
        if (target instanceof FullTypeCriteria<?, ?> full && full.getFromCriteria() != null) {
            throw new UnsupportedOperationException("写入 JOIN[" + index + "] 不支持 targetCriteria.fromCriteria");
        }
        Class<?> targetType = CriteriaEntityTypes.entityClass(target);
        Class<?> sourceType = wrapper.getEntityClass();
        String sourceAlias = wrapper.getAlias();
        MConsumer<MPJLambdaWrapper> derived = hasDerivedTableConditions(target) ? table -> {
            if (target instanceof IRelatedQueryCriteria<?, ?> related) {
                int nested = 0;
                for (JoinCondition nestedJoin : related.getJoinConditions()) {
                    processWriteJoin(table, nestedJoin, nested++);
                }
            }
            if (target instanceof IWhereConditionCriteria<?, ?> where) {
                processWriteWhereConditions(table, where.getWhereConditions(), Condition.Logical.AND,
                        targetType, table.getAlias(), null, null,
                        wrapper instanceof WriteSubqueryProvider provider ? provider : null);
            }
            table.selectAll();
        } : null;
        ((JoinAbstractLambdaWrapper) wrapper).join(type.name() + " JOIN", targetType, derived, null,
                (on, parent) -> {
                    JoinAbstractWrapper<?, ?> onWrapper = (JoinAbstractWrapper<?, ?>) on;
                    String targetAlias = onWrapper.getTableList().getPrefix(onWrapper.getIndex(), targetType, false);
                    processWriteJoinCondition(onWrapper, join, sourceType, sourceAlias, targetType, targetAlias);
                });
    }

    /**
     * 递归编译写入 JOIN 的 ON 条件并解析两侧表别名。
     */
    private static void processWriteJoinCondition(JoinAbstractWrapper<?, ?> wrapper, BaseCondition<?> node,
                                       Class<?> sourceType, String sourceAlias,
                                       Class<?> targetType, String targetAlias) {
        if (!(node instanceof JoinCondition) && node.getTargetCriteria() != null) {
            throw new IllegalArgumentException("JOIN ON 子条件不能重复声明 targetCriteria");
        }
        if (node.getChildren() != null) {
            if (node.getChildren().isEmpty() || node.getSourceProperty() != null
                    || node.getTargetProperty() != null || node.getValue() != null) {
                throw new IllegalArgumentException("JOIN ON 条件分组不能为空或同时声明比较字段");
            }
            if (node.getCondition() == Condition.Logical.NOT) {
                if (node.getChildren().size() != 1) {
                    throw new IllegalArgumentException("NOT 需要一个子表达式");
                }
                wrapper.not(true, group -> processWriteJoinCondition(group, node.getChildren().getFirst(),
                        sourceType, sourceAlias, targetType, targetAlias));
            } else {
                wrapper.nested(true, group -> ConditionTraversal.consume(node.getChildren(), node.getCondition(), (link, child) -> {
                    if (link == Condition.Logical.OR) {
                        group.or(true);
                    }
                    processWriteJoinCondition(group, child, sourceType, sourceAlias, targetType, targetAlias);
                }));
            }
            return;
        }
        if (node.getSourceProperty() != null && node.getTargetProperty() != null) {
            if (node.getValue() != null) {
                throw new IllegalArgumentException("JOIN ON 双字段比较不能同时声明 value");
            }
            String left = sourceAlias + "." + resolveColumnName(sourceType, node.getSourceProperty());
            String right = targetAlias + "." + resolveColumnName(targetType, node.getTargetProperty());
            applyColumnComparison(wrapper, node.getCondition(), left, right);
            return;
        }
        if (node.getTargetCriteria() != null) {
            throw new UnsupportedOperationException("写入 JOIN ON 不支持子查询条件");
        }
        WhereCondition scalar = new WhereCondition(node.getSourceProperty() != null
                ? node.getSourceProperty() : node.getTargetProperty(), node.getCondition(), node.getValue());
        validateScalar(scalar);
        boolean source = node.getSourceProperty() != null;
        String column = (source ? sourceAlias : targetAlias) + "."
                + resolveColumnName(source ? sourceType : targetType, resolveSourceProperty(scalar));
        applyCondition(scalar, column, wrapper);
    }

    /**
     * 使用 MPJ 写入 Wrapper 的字段比较方法构造双字段条件。
     */
    private static void applyColumnComparison(JoinAbstractWrapper<?, ?> wrapper, Condition condition,
                                       String sourceColumn, String targetColumn) {
        if (!(condition instanceof Condition.Compare compare)) {
            throw new IllegalArgumentException("不支持的双字段比较: " + condition);
        }
        switch (compare) {
            case EQUAL -> wrapper.eqSql(true, sourceColumn, targetColumn);
            case NOT_EQUAL -> wrapper.not(true, group -> group.eqSql(true, sourceColumn, targetColumn));
            case GREATER_THAN -> wrapper.gtSql(true, sourceColumn, targetColumn);
            case GREATER_THAN_EQUAL -> wrapper.geSql(true, sourceColumn, targetColumn);
            case LESS_THAN -> wrapper.ltSql(true, sourceColumn, targetColumn);
            case LESS_THAN_EQUAL -> wrapper.leSql(true, sourceColumn, targetColumn);
        }
    }

    /**
     * 递归编译连表写入的 WHERE，包括相关子查询与外层字段比较。
     */
    private static void processWriteWhereConditions(JoinAbstractWrapper<?, ?> wrapper, List<WhereCondition> nodes,
                                          Condition connector, Class<?> rootType, String rootAlias,
                                          Class<?> outerType, String outerAlias,
                                          WriteSubqueryProvider subqueries) {
        ConditionTraversal.consume(nodes, connector, (link, node) -> {
            if (link == Condition.Logical.OR) {
                wrapper.or(true);
            }
            if (node.getChildren() != null) {
                if (node.getChildren().isEmpty() || node.getSourceProperty() != null
                        || node.getTargetProperty() != null || node.getTargetCriteria() != null || node.getValue() != null) {
                    throw new IllegalArgumentException("写入 WHERE 条件分组格式错误");
                }
                if (node.getCondition() == Condition.Logical.NOT) {
                    if (node.getChildren().size() != 1) {
                        throw new IllegalArgumentException("NOT 需要一个子表达式");
                    }
                    wrapper.not(true, group -> processWriteWhereConditions(group, node.getChildren(), Condition.Logical.AND,
                            rootType, rootAlias, outerType, outerAlias, subqueries));
                } else {
                    wrapper.nested(true, group -> processWriteWhereConditions(group, node.getChildren(), node.getCondition(),
                            rootType, rootAlias, outerType, outerAlias, subqueries));
                }
            } else {
                if (node.getTargetCriteria() != null) {
                    if (node.getCondition() == Condition.Membership.IN || node.getCondition() == Condition.Membership.NOT_IN) {
                        processWriteInSubqueryCondition(wrapper, node, rootType, rootAlias, subqueries);
                    } else {
                        processWriteExistsCondition(wrapper, node, rootType, rootAlias, subqueries);
                    }
                    return;
                }
                if (node.getTargetProperty() != null) {
                    if (outerType == null || node.getValue() != null) {
                        throw new IllegalArgumentException("写入 WHERE 双字段条件需要直接外层查询");
                    }
                    String left = outerAlias + "." + resolveColumnName(outerType, node.getSourceProperty());
                    String right = rootAlias + "." + resolveColumnName(rootType, node.getTargetProperty());
                    applyColumnComparison(wrapper, node.getCondition(), left, right);
                    return;
                }
                validateScalar(node);
                String column = rootAlias + "." + resolveColumnName(rootType, resolveSourceProperty(node));
                applyCondition(node, column, wrapper);
            }
        });
    }

    /**
     * 编译写入 WHERE 中的 EXISTS/NOT_EXISTS 子查询。
     */
    private static void processWriteExistsCondition(JoinAbstractWrapper<?, ?> wrapper, WhereCondition node,
                                           Class<?> outerType, String outerAlias,
                                           WriteSubqueryProvider subqueries) {
        Condition operation = node.getCondition();
        if (operation != Condition.Existence.EXISTS && operation != Condition.Existence.NOT_EXISTS) {
            throw new UnsupportedOperationException("写入 WHERE 子查询暂不支持 " + operation);
        }
        if (node.getValue() != null || node.getTargetProperty() != null || node.getSourceProperty() != null) {
            throw new IllegalArgumentException("EXISTS 子查询不能同时声明比较值或外层字段");
        }
        ICriteria<?, ?> target = node.getTargetCriteria();
        validateWriteOptions(target);
        if (target instanceof FullTypeCriteria<?, ?> full && full.getFromCriteria() != null) {
            throw new UnsupportedOperationException("写入 WHERE 子查询不支持 fromCriteria");
        }
        if (subqueries == null) {
            throw new UnsupportedOperationException("当前写入子查询作用域不支持 EXISTS/NOT_EXISTS");
        }
        Class<?> type = CriteriaEntityTypes.entityClass(target);
        MPJLambdaWrapper<?> child = subqueries.subquery(type, outerAlias);
        if (target instanceof IRelatedQueryCriteria<?, ?> related) {
            int index = 0;
            for (JoinCondition join : related.getJoinConditions()) {
                processWriteJoin(child, join, index++);
            }
        }
        if (target instanceof IWhereConditionCriteria<?, ?> where) {
            processWriteWhereConditions(child, where.getWhereConditions(), Condition.Logical.AND,
                    type, child.getAlias(), outerType, outerAlias, subqueries);
        }
        child.selectAll();
        String sql = WrapperUtils.buildUnionSqlByWrapper(type, child);
        if (operation == Condition.Existence.EXISTS) {
            wrapper.exists(true, sql);
        } else {
            wrapper.notExists(true, sql);
        }
    }

    /**
     * 在写入 Wrapper 的当前别名作用域中创建子查询。
     */
    private interface WriteSubqueryProvider {
        /**
         * 创建可引用指定外层别名的 MPJ 子查询 Wrapper。
         */
        MPJLambdaWrapper<?> subquery(Class<?> type, String outerAlias);
    }

    /**
     * 提供保留更新 Wrapper 参数上下文的子查询创建方式。
     */
    private static final class CriteriaUpdateJoinWrapper<E> extends UpdateJoinWrapper<E> implements WriteSubqueryProvider {
        /**
         * 创建指定实体类型的连表更新 Wrapper。
         */
        private CriteriaUpdateJoinWrapper(Class<E> type) {
            super(type);
        }

        /**
         * 在当前更新 Wrapper 的参数上下文中创建子查询。
         */
        @Override
        public MPJLambdaWrapper<?> subquery(Class<?> type, String outerAlias) {
            return subInstance(type, createWriteSubqueryAlias(outerAlias));
        }
    }

    /**
     * 提供保留删除 Wrapper 参数上下文的子查询创建方式。
     */
    private static final class CriteriaDeleteJoinWrapper<E> extends DeleteJoinWrapper<E> implements WriteSubqueryProvider {
        /**
         * 创建指定实体类型的连表删除 Wrapper。
         */
        private CriteriaDeleteJoinWrapper(Class<E> type) {
            super(type);
        }

        /**
         * 在当前删除 Wrapper 的参数上下文中创建子查询。
         */
        @Override
        public MPJLambdaWrapper<?> subquery(Class<?> type, String outerAlias) {
            return subInstance(type, createWriteSubqueryAlias(outerAlias));
        }
    }

    /**
     * 为写入子查询生成不会遮蔽外层表的别名。
     */
    private static String createWriteSubqueryAlias(String outerAlias) {
        return "t".equals(outerAlias) ? ConfigProperties.subQueryAlias
                : outerAlias + ConfigProperties.subQueryAlias;
    }

    /**
     * 编译写入 WHERE 中只选择单列的 IN/NOT_IN 子查询。
     */
    private static void processWriteInSubqueryCondition(JoinAbstractWrapper<?, ?> wrapper, WhereCondition node,
                                               Class<?> sourceType, String sourceAlias,
                                               WriteSubqueryProvider subqueries) {
        if (subqueries == null) {
            throw new UnsupportedOperationException("当前写入子查询作用域不支持 IN/NOT_IN");
        }
        if (node.getValue() != null || node.getTargetProperty() != null || node.getSourceProperty() == null) {
            throw new IllegalArgumentException("IN/NOT_IN 子查询需要 sourceProperty，不能同时声明 value/targetProperty");
        }
        ICriteria<?, ?> target = node.getTargetCriteria();
        validateWriteOptions(target);
        if (!(target instanceof IReturnFieldCriteria<?, ?> selected) || selected.getReturnFields().size() != 1) {
            throw new IllegalArgumentException("IN/NOT_IN 子查询必须显式选择且只选择一个字段");
        }
        if (target instanceof FullTypeCriteria<?, ?> full && full.getFromCriteria() != null) {
            throw new UnsupportedOperationException("写入 IN/NOT_IN 子查询不支持 fromCriteria");
        }
        Class<?> type = CriteriaEntityTypes.entityClass(target);
        MPJLambdaWrapper<?> child = subqueries.subquery(type, sourceAlias);
        if (target instanceof IRelatedQueryCriteria<?, ?> related) {
            int index = 0;
            for (JoinCondition join : related.getJoinConditions()) {
                processWriteJoin(child, join, index++);
            }
        }
        if (target instanceof IWhereConditionCriteria<?, ?> where) {
            processWriteWhereConditions(child, where.getWhereConditions(), Condition.Logical.AND,
                    type, child.getAlias(), null, null, subqueries);
        }
        String selectedProperty = selected.getReturnFields().iterator().next();
        String selectedColumn = child.getAlias() + "." + resolveColumnName(type, selectedProperty);
        child.select(selectedColumn);
        String sql = WrapperUtils.buildUnionSqlByWrapper(type, child);
        String sourceColumn = sourceAlias + "." + resolveColumnName(sourceType, node.getSourceProperty());
        if (node.getCondition() == Condition.Membership.IN) {
            wrapper.inSql(true, sourceColumn, sql);
        } else {
            wrapper.notInSql(true, sourceColumn, sql);
        }
    }

    /**
     * 判断 Criteria 是否包含 JOIN、FROM 或 WHERE 子查询等关联筛选。
     */
    static boolean hasRelationalFilter(ICriteria<?, ?> criteria) {
        if (criteria instanceof IRelatedQueryCriteria<?, ?> related && !related.getJoinConditions().isEmpty()) {
            return true;
        }
        if (criteria instanceof FullTypeCriteria<?, ?> full && full.getFromCriteria() != null) {
            return true;
        }
        return criteria instanceof IWhereConditionCriteria<?, ?> where
                && where.getWhereConditions().stream().anyMatch(WrapperUtil::hasSubquery);
    }

    /**
     * 递归检查条件树中是否包含目标 Criteria 子查询。
     */
    private static boolean hasSubquery(WhereCondition node) {
        return node.getTargetCriteria() != null || node.getChildren() != null
                && node.getChildren().stream().anyMatch(WrapperUtil::hasSubquery);
    }

    /**
     * 拒绝不适用于更新或删除的排序及非默认分页设置。
     */
    private static void validateWriteOptions(ICriteria<?, ?> criteria) {
        if (criteria instanceof IOrderByCriteria<?, ?> order && order.getOrderBy() != null
                && !order.getOrderBy().isBlank()) {
            throw new IllegalArgumentException("更新/删除不支持 orderBy");
        }
        validateNestedPage(criteria);
    }

    /**
     * 将普通 WHERE 条件列表编译到 MP Wrapper。
     */
    private static <
            E extends IEntity<E>,
            C extends IWhereConditionCriteria<E, C>>
    void processWhereConditionCriteria(AbstractWrapper<E, String, ?> wrapper, IWhereConditionCriteria<E, C> whereConditionCriteria) {
        processMpWhereConditions(wrapper, whereConditionCriteria.getWhereConditions(), Condition.Logical.AND);
    }

    /**
     * 递归编译普通 MP WHERE；关联子查询由 MPJ 查询路径负责。
     */
    @SuppressWarnings("unchecked")
    private static void processMpWhereConditions(AbstractWrapper<?, String, ?> wrapper, List<WhereCondition> conditions, Condition connector) {
        ConditionTraversal.consume(conditions, connector, (link, node) -> {
            if (link == Condition.Logical.OR) {
                wrapper.or();
            }
            if (node.getChildren() != null) {
                if (node.getChildren().isEmpty() || node.getSourceProperty() != null
                        || node.getTargetProperty() != null || node.getValue() != null
                        || node.getTargetCriteria() != null) {
                    throw new IllegalArgumentException("条件分组不能为空或同时声明比较字段/目标");
                }
                if (node.getCondition() == Condition.Logical.NOT) {
                    if (node.getChildren().size() != 1) {
                        throw new IllegalArgumentException("NOT 需要一个子表达式");
                    }
                    wrapper.not(w -> processMpWhereConditions((AbstractWrapper<?, String, ?>) w, node.getChildren(), Condition.Logical.AND));
                } else {
                    wrapper.nested(w -> processMpWhereConditions((AbstractWrapper<?, String, ?>) w, node.getChildren(), node.getCondition()));
                }
            } else {
                if (node.getTargetCriteria() != null) {
                    throw new UnsupportedOperationException("关联条件需要 MPJ 查询执行器");
                }
                if (node.getTargetProperty() != null) {
                    if (node.getSourceProperty() == null || node.getValue() != null) {
                        throw new IllegalArgumentException("双字段比较需要 sourceProperty/targetProperty，不能同时声明 value");
                    }
                    Class<?> entityType = wrapper.getEntityClass();
                    String sourceColumn = resolveColumnName(entityType, node.getSourceProperty());
                    String targetColumn = resolveColumnName(entityType, node.getTargetProperty());
                    applyColumnComparison(wrapper, node.getCondition(), sourceColumn, targetColumn);
                    return;
                }
                validateScalar(node);
                applyMpCondition(node, wrapper);
            }
        });
    }

    /**
     * 使用 MP Wrapper 的字段比较方法构造同表双字段条件。
     */
    private static void applyColumnComparison(AbstractWrapper<?, String, ?> wrapper, Condition condition,
                                       String sourceColumn, String targetColumn) {
        if (!(condition instanceof Condition.Compare compare)) {
            throw new IllegalArgumentException("不支持的双字段比较: " + condition);
        }
        switch (compare) {
            case EQUAL -> wrapper.eqSql(true, sourceColumn, targetColumn);
            case NOT_EQUAL -> wrapper.not(true, group -> group.eqSql(true, sourceColumn, targetColumn));
            case GREATER_THAN -> wrapper.gtSql(true, sourceColumn, targetColumn);
            case GREATER_THAN_EQUAL -> wrapper.geSql(true, sourceColumn, targetColumn);
            case LESS_THAN -> wrapper.ltSql(true, sourceColumn, targetColumn);
            case LESS_THAN_EQUAL -> wrapper.leSql(true, sourceColumn, targetColumn);
        }
    }

    /**
     * 校验标量条件的操作符和值；空集合只允许 IN/NOT_IN。
     */
    private static void validateScalar(WhereCondition node) {
        if (node.getCondition() == null) {
            throw new IllegalArgumentException("条件缺少 condition");
        }
        if (node.getValue() == null && node.getCondition() != Condition.NullCheck.IS_NULL
                && node.getCondition() != Condition.NullCheck.IS_NOT_NULL && node.getCondition() != Condition.Text.IS_EMPTY
                && node.getCondition() != Condition.Text.IS_NOT_EMPTY) {
            throw new IllegalArgumentException("已登记的比较条件缺少 value: " + node.getSourceProperty());
        }
        if (node.getValue() instanceof Collection<?> values && values.isEmpty()
                && node.getCondition() != Condition.Membership.IN && node.getCondition() != Condition.Membership.NOT_IN) {
            throw new IllegalArgumentException("已登记的比较条件不能使用空集合");
        }
    }

    /**
     * Criteria 只保存 Java 属性；SQL 列只在本次 Wrapper 转换中使用。
     *
     * @param node    待应用的标量条件
     * @param wrapper 普通 MP 或 MPJ 字符串字段 Wrapper
     */
    static void applyMpCondition(WhereCondition node, AbstractWrapper<?, String, ?> wrapper) {
        String column = resolveColumnName(wrapper.getEntityClass(), resolveSourceProperty(node));
        if (wrapper instanceof MPJQueryWrapper<?> query) {
            column = query.getAlias() + "." + column;
        }
        applyCondition(node, column, wrapper);
    }

    /**
     * 将 Java 属性解析为当前 MPJ 写入表实例的列并应用标量条件。
     */
    static void applyJoinCondition(WhereCondition node, JoinAbstractWrapper<?, ?> wrapper) {
        Class<?> type = wrapper.getIndex() == null ? wrapper.getEntityClass() : wrapper.getJoinClass();
        String alias = wrapper.getIndex() == null ? wrapper.getAlias()
                : wrapper.getTableList().getPrefix(wrapper.getIndex(), type, false);
        String column = alias + "." + resolveColumnName(type, resolveSourceProperty(node));
        applyCondition(node, column, wrapper);
    }

    /**
     * 获取用于列名解析的源属性；JSON 路径条件只取点号前的实体属性。
     */
    private static String resolveSourceProperty(WhereCondition node) {
        if (node.getCondition() == Condition.Json.JSON_OBJECT_PATH_EQUAL
                || node.getCondition() == Condition.Json.JSON_OBJECT_PATH_LIKE) {
            return parseJsonPropertyPath(node.getSourceProperty())[0];
        }
        return node.getSourceProperty();
    }

    /**
     * 将属性和表实例解析为 MPJ APT 字段，并应用标量或子查询条件。
     */
    static void applyAptCondition(MpjWrapper<?> wrapper, BaseCondition<?> node, BaseColumn<?> table,
                                  Function<BaseCondition<?>, String> subquerySql) {
        String property = node.getSourceProperty();
        String directProperty = property == null ? null : resolveSourceProperty((WhereCondition) node);
        Column column = directProperty == null ? null : BaseColumnFactory.column(table, directProperty);
        String sqlColumn = directProperty == null ? null : resolveQualifiedColumnName(wrapper, table, directProperty);
        applyResolvedAptCondition(node, wrapper, column, sqlColumn, subquerySql);
    }

    /**
     * 将非关联操作符应用到普通 MP 或 MPJ 写入 Wrapper，使用原生参数绑定。
     * 列参数以 String 保存数据库列名或带表别名的列名；比较值不限定为 String。
     */
    @SuppressWarnings("unchecked")
    private static void applyCondition(BaseCondition<?> node, String column, Object target) {
        Condition operation = Objects.requireNonNull(node.getCondition(), "条件缺少 condition");
        Object value = node.getValue();
        if (operation instanceof Condition.Compare compare) {
            if (target instanceof AbstractWrapper) {
                AbstractWrapper<?, String, ?> wrapper = (AbstractWrapper<?, String, ?>) target;
                switch (compare) {
                    case EQUAL -> wrapper.eq(true, column, value);
                    case NOT_EQUAL -> wrapper.ne(true, column, value);
                    case GREATER_THAN -> wrapper.gt(true, column, value);
                    case GREATER_THAN_EQUAL -> wrapper.ge(true, column, value);
                    case LESS_THAN -> wrapper.lt(true, column, value);
                    case LESS_THAN_EQUAL -> wrapper.le(true, column, value);
                }
            } else if (target instanceof JoinAbstractWrapper<?, ?> wrapper) {
                switch (compare) {
                    case EQUAL -> wrapper.eq(true, column, value);
                    case NOT_EQUAL -> wrapper.ne(true, column, value);
                    case GREATER_THAN -> wrapper.gt(true, column, value);
                    case GREATER_THAN_EQUAL -> wrapper.ge(true, column, value);
                    case LESS_THAN -> wrapper.lt(true, column, value);
                    case LESS_THAN_EQUAL -> wrapper.le(true, column, value);
                }
            }
        } else if (operation instanceof Condition.Membership membership) {
            Collection<?> values = ConvertUtil.convert(Collection.class, value);
            if (target instanceof AbstractWrapper) {
                AbstractWrapper<?, String, ?> wrapper = (AbstractWrapper<?, String, ?>) target;
                if (values.isEmpty()) {
                    wrapper.apply(true, membership == Condition.Membership.IN ? "1 = 0" : "1 = 1");
                } else if (membership == Condition.Membership.IN) {
                    wrapper.in(true, column, values);
                } else {
                    wrapper.notIn(true, column, values);
                }
            } else if (target instanceof JoinAbstractWrapper<?, ?> wrapper) {
                if (values.isEmpty()) {
                    wrapper.apply(true, membership == Condition.Membership.IN ? "1 = 0" : "1 = 1");
                } else if (membership == Condition.Membership.IN) {
                    wrapper.in(true, column, values);
                } else {
                    wrapper.notIn(true, column, values);
                }
            }
        } else if (operation instanceof Condition.NullCheck check) {
            if (target instanceof AbstractWrapper) {
                AbstractWrapper<?, String, ?> wrapper = (AbstractWrapper<?, String, ?>) target;
                if (check == Condition.NullCheck.IS_NULL) {
                    wrapper.isNull(true, column);
                } else {
                    wrapper.isNotNull(true, column);
                }
            } else if (target instanceof JoinAbstractWrapper<?, ?> wrapper) {
                if (check == Condition.NullCheck.IS_NULL) {
                    wrapper.isNull(true, column);
                } else {
                    wrapper.isNotNull(true, column);
                }
            }
        } else if (operation instanceof Condition.Text text) {
            if (target instanceof AbstractWrapper) {
                AbstractWrapper<?, String, ?> wrapper = (AbstractWrapper<?, String, ?>) target;
                switch (text) {
                    case IS_EMPTY -> wrapper.eq(true, column, "");
                    case IS_NOT_EMPTY -> wrapper.ne(true, column, "");
                    case START_WITH -> wrapper.likeRight(true, column, value);
                    case LIKE -> wrapper.like(true, column, value);
                    case NOT_LIKE -> wrapper.notLike(true, column, value);
                }
            } else if (target instanceof JoinAbstractWrapper<?, ?> wrapper) {
                switch (text) {
                    case IS_EMPTY -> wrapper.eq(true, column, "");
                    case IS_NOT_EMPTY -> wrapper.ne(true, column, "");
                    case START_WITH -> wrapper.likeRight(true, column, value);
                    case LIKE -> wrapper.like(true, column, value);
                    case NOT_LIKE -> wrapper.notLike(true, column, value);
                }
            }
        } else if (operation instanceof Condition.Array || operation instanceof Condition.Json) {
            String dialect = operation instanceof Condition.Json && !isMultiValueJson(operation) ? resolveDatabaseDialect() : "";
            if (target instanceof AbstractWrapper) {
                AbstractWrapper<?, String, ?> wrapper = (AbstractWrapper<?, String, ?>) target;
                if (isMultiValueJson(operation)) {
                    applyJsonArrayConditions(node, column, wrapper);
                } else if (isJsonObjectPath(operation)) {
                    applyJsonObjectPath(node, column, dialect, wrapper);
                } else if ("dm".equals(dialect)) {
                    wrapper.like(true, column, value);
                } else {
                    SqlFragment fragment = buildArrayOrJsonConditionSql(node, column, dialect);
                    wrapper.apply(true, fragment.sql(), fragment.parameters());
                }
            } else if (target instanceof JoinAbstractWrapper<?, ?> wrapper) {
                if (isMultiValueJson(operation)) {
                    applyJsonArrayConditions(node, column, wrapper);
                } else if (isJsonObjectPath(operation)) {
                    applyJsonObjectPath(node, column, dialect, wrapper);
                } else if ("dm".equals(dialect)) {
                    wrapper.like(true, column, value);
                } else {
                    SqlFragment fragment = buildArrayOrJsonConditionSql(node, column, dialect);
                    wrapper.apply(true, fragment.sql(), fragment.parameters());
                }
            }
        } else {
            throw new UnsupportedOperationException("条件不能作为标量使用: " + operation);
        }
    }

    /**
     * 将已解析的列对象及列表达式对应的条件应用到 MPJ APT Wrapper；子查询由传入的 SQL 生成函数提供。
     */
    private static void applyResolvedAptCondition(BaseCondition<?> node, AptAbstractWrapper<?, ?> wrapper,
                                       Column column, String sqlColumn,
                                       Function<BaseCondition<?>, String> subquerySql) {
        Condition operation = Objects.requireNonNull(node.getCondition(), "条件缺少 condition");
        Object value = node.getValue();
        if (node.getTargetCriteria() != null) {
            if (subquerySql == null) {
                throw new IllegalArgumentException("子查询缺少编译上下文");
            }
            String sql = subquerySql.apply(node);
            if (operation == Condition.Existence.EXISTS) {
                wrapper.exists(sql);
            } else if (operation == Condition.Existence.NOT_EXISTS) {
                wrapper.notExists(sql);
            } else if (operation == Condition.Membership.IN) {
                wrapper.inSql(column, sql);
            } else if (operation == Condition.Membership.NOT_IN) {
                wrapper.notInSql(column, sql);
            } else {
                throw new IllegalArgumentException("不支持的子查询条件: " + operation);
            }
            return;
        }
        if (operation instanceof Condition.Compare compare) {
            switch (compare) {
                case EQUAL -> wrapper.eq(true, column, value);
                case NOT_EQUAL -> wrapper.ne(true, column, value);
                case GREATER_THAN -> wrapper.gt(true, column, value);
                case GREATER_THAN_EQUAL -> wrapper.ge(true, column, value);
                case LESS_THAN -> wrapper.lt(true, column, value);
                case LESS_THAN_EQUAL -> wrapper.le(true, column, value);
            }
        } else if (operation instanceof Condition.Membership membership) {
            Collection<?> values = ConvertUtil.convert(Collection.class, value);
            if (values.isEmpty()) {
                wrapper.apply(true, membership == Condition.Membership.IN ? "1 = 0" : "1 = 1");
            } else if (membership == Condition.Membership.IN) {
                wrapper.in(true, column, values);
            } else {
                wrapper.notIn(true, column, values);
            }
        } else if (operation instanceof Condition.NullCheck check) {
            if (check == Condition.NullCheck.IS_NULL) {
                wrapper.isNull(true, column);
            } else {
                wrapper.isNotNull(true, column);
            }
        } else if (operation instanceof Condition.Text text) {
            switch (text) {
                case IS_EMPTY -> wrapper.eq(true, column, "");
                case IS_NOT_EMPTY -> wrapper.ne(true, column, "");
                case START_WITH -> wrapper.likeRight(true, column, value);
                case LIKE -> wrapper.like(true, column, value);
                case NOT_LIKE -> wrapper.notLike(true, column, value);
            }
        } else if (operation instanceof Condition.Array || operation instanceof Condition.Json) {
            String dialect = operation instanceof Condition.Json && !isMultiValueJson(operation) ? resolveDatabaseDialect() : "";
            if (isMultiValueJson(operation)) {
                applyJsonArrayConditions(node, sqlColumn, column, wrapper);
            } else if (isJsonObjectPath(operation)) {
                applyJsonObjectPath(node, sqlColumn, dialect, wrapper);
            } else if ("dm".equals(dialect)) {
                wrapper.like(true, column, value);
            } else {
                SqlFragment fragment = buildArrayOrJsonConditionSql(node, sqlColumn, dialect);
                wrapper.apply(true, fragment.sql(), fragment.parameters());
            }
        } else {
            throw new UnsupportedOperationException("条件不能作为标量使用: " + operation);
        }
    }

    /**
     * 保存无法由原生条件方法表达的受控 SQL 片段及绑定参数。
     */
    private record SqlFragment(String sql, Object[] parameters) {
    }

    /**
     * 保存 JSON 数组的比较值、ANY/ALL 关系与方言回退标记。
     */
    private record JsonArrayConditions(Collection<?> values, boolean any, boolean dameng) {
    }

    /**
     * 判断操作符是否为 JSON 数组多值比较。
     */
    private static boolean isMultiValueJson(Condition operation) {
        return operation == Condition.Json.JSON_ARRAY_CONTAINS_ANY
                || operation == Condition.Json.JSON_ARRAY_CONTAINS_ALL;
    }

    /**
     * 判断操作符是否比较 JSON 对象中的指定路径。
     */
    private static boolean isJsonObjectPath(Condition operation) {
        return operation == Condition.Json.JSON_OBJECT_PATH_EQUAL
                || operation == Condition.Json.JSON_OBJECT_PATH_LIKE;
    }

    /**
     * 将已校验的 JSON 属性路径转换为当前方言使用的列表达式。
     */
    private static String buildJsonPathExpression(BaseCondition<?> node, String column, String dialect) {
        if ("dm".equals(dialect)) {
            return column;
        }
        if (!dialect.isBlank() && !dialect.equals("mysql")) {
            throw new UnsupportedOperationException("不支持的 JSON 条件方言: " + dialect);
        }
        return column + "->'$." + parseJsonPropertyPath(node.getSourceProperty())[1] + "'";
    }

    /**
     * 将 JSON 路径比较应用到普通 MP Wrapper。
     */
    private static void applyJsonObjectPath(BaseCondition<?> node, String column, String dialect, AbstractWrapper<?, String, ?> wrapper) {
        String expression = buildJsonPathExpression(node, column, dialect);
        if ("dm".equals(dialect) || node.getCondition() == Condition.Json.JSON_OBJECT_PATH_LIKE) {
            wrapper.like(true, expression, node.getValue());
        } else {
            wrapper.eq(true, expression, node.getValue());
        }
    }

    /**
     * 将 JSON 路径比较应用到 MPJ 写入 Wrapper。
     */
    private static void applyJsonObjectPath(BaseCondition<?> node, String column, String dialect, JoinAbstractWrapper<?, ?> wrapper) {
        String expression = buildJsonPathExpression(node, column, dialect);
        if ("dm".equals(dialect) || node.getCondition() == Condition.Json.JSON_OBJECT_PATH_LIKE) {
            wrapper.like(true, expression, node.getValue());
        } else {
            wrapper.eq(true, expression, node.getValue());
        }
    }

    /**
     * 将 JSON 路径比较应用到 MPJ APT 查询 Wrapper。
     */
    private static void applyJsonObjectPath(BaseCondition<?> node, String column, String dialect, AptAbstractWrapper<?, ?> wrapper) {
        String expression = buildJsonPathExpression(node, column, dialect);
        if ("dm".equals(dialect) || node.getCondition() == Condition.Json.JSON_OBJECT_PATH_LIKE) {
            wrapper.like(true, expression, node.getValue());
        } else {
            wrapper.eq(true, expression, node.getValue());
        }
    }

    /**
     * 校验 JSON 数组多值比较的输入及数据库方言。
     */
    private static JsonArrayConditions resolveJsonArrayConditions(BaseCondition<?> node) {
        Collection<?> values = ConvertUtil.convert(Collection.class, node.getValue());
        if (values.isEmpty()) {
            throw new IllegalArgumentException("JSON 数组条件不能使用空集合");
        }
        String databaseId = resolveDatabaseDialect();
        if (!databaseId.isBlank() && !databaseId.equals("mysql") && !databaseId.equals("dm")) {
            throw new UnsupportedOperationException("不支持的 JSON 条件方言: " + databaseId);
        }
        return new JsonArrayConditions(values, node.getCondition() == Condition.Json.JSON_ARRAY_CONTAINS_ANY,
                databaseId.equals("dm"));
    }

    /**
     * 在普通 MP Wrapper 中按 ANY/ALL 关系分组应用 JSON 数组条件。
     */
    private static void applyJsonArrayConditions(BaseCondition<?> node, String column, AbstractWrapper<?, String, ?> wrapper) {
        JsonArrayConditions conditions = resolveJsonArrayConditions(node);
        wrapper.nested(group -> {
            int index = 0;
            for (Object item : conditions.values()) {
                if (conditions.any() && index > 0) {
                    group.or();
                }
                if (conditions.dameng()) {
                    group.like(true, column, item);
                } else {
                    group.apply(true, "JSON_CONTAINS(" + column + ",{0})", JsonUtil.writeValue(item));
                }
                index++;
            }
        });
    }

    /**
     * 在 MPJ 写入 Wrapper 中按 ANY/ALL 关系分组应用 JSON 数组条件。
     */
    private static void applyJsonArrayConditions(BaseCondition<?> node, String column, JoinAbstractWrapper<?, ?> wrapper) {
        JsonArrayConditions conditions = resolveJsonArrayConditions(node);
        wrapper.nested(true, group -> {
            int index = 0;
            for (Object item : conditions.values()) {
                if (conditions.any() && index > 0) {
                    group.or(true);
                }
                if (conditions.dameng()) {
                    group.like(true, column, item);
                } else {
                    group.apply(true, "JSON_CONTAINS(" + column + ",{0})", JsonUtil.writeValue(item));
                }
                index++;
            }
        });
    }

    /**
     * 在 MPJ APT 查询 Wrapper 中按 ANY/ALL 关系分组应用 JSON 数组条件。
     */
    private static void applyJsonArrayConditions(BaseCondition<?> node,
                                                 String sqlColumn,
                                                 Column column,
                                                 AptAbstractWrapper<?, ?> wrapper) {
        JsonArrayConditions conditions = resolveJsonArrayConditions(node);
        wrapper.nested(group -> {
            int index = 0;
            for (Object item : conditions.values()) {
                if (conditions.any() && index > 0) {
                    group.or();
                }
                if (conditions.dameng()) {
                    group.like(true, column, item);
                } else {
                    group.apply(true, "JSON_CONTAINS(" + sqlColumn + ",{0})", JsonUtil.writeValue(item));
                }
                index++;
            }
        });
    }

    /**
     * 将“实体属性.JSON 路径”拆分并校验，防止路径内容进入未受控 SQL。
     */
    private static String[] parseJsonPropertyPath(String property) {
        String[] path = property == null ? new String[0] : property.split("\\.", 2);
        if (path.length != 2 || path[1].isBlank()) {
            throw new IllegalArgumentException("JSON 路径需要使用 属性.路径 格式: " + property);
        }
        for (String part : path[1].split("\\.")) {
            if (!part.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
                throw new IllegalArgumentException("非法 JSON 路径: " + property);
            }
        }
        path[1] = Arrays.stream(path[1].split("\\."))
                .map(part -> part.contains("-") ? StrUtil.format("\"{}\"", part) : part)
                .collect(Collectors.joining("."));
        return path;
    }

    /**
     * 为数组运算及 JSON_CONTAINS 生成带绑定参数的方言条件片段。
     */
    private static SqlFragment buildArrayOrJsonConditionSql(BaseCondition<?> node, String column, String databaseId) {
        Condition operation = node.getCondition();
        Object value = node.getValue();
        if (operation instanceof Condition.Array array) {
            Collection<?> values = ConvertUtil.convert(Collection.class, value);
            if (values.isEmpty()) {
                throw new IllegalArgumentException("数组条件不能使用空集合");
            }
            return new SqlFragment(column + (array == Condition.Array.CONTAINS_ALL ? " @> {0}" : " && {0}"),
                    new Object[]{"{" + CollUtil.join(values, ",") + "}"});
        }
        if (operation == Condition.Json.JSON_ARRAY_CONTAINS) {
            if (databaseId.isBlank() || databaseId.equals("mysql")) {
                return new SqlFragment("JSON_CONTAINS(" + column + ",{0})",
                        new Object[]{JsonUtil.writeValue(Collections.singletonList(value))});
            }
        }
        throw new UnsupportedOperationException("不支持的条件或数据库方言: " + operation + " / " + databaseId);
    }

    /**
     * 从当前路由数据源识别 JSON 条件所需的数据库方言标识。
     */
    private static String resolveDatabaseDialect() {
        DynamicRoutingDataSource dataSource = BeanProviderUtil.getBean(DynamicRoutingDataSource.class);
        try {
            String id = BeanProviderUtil.getBean(DatabaseIdProvider.class).getDatabaseId(dataSource);
            return id == null ? "" : id;
        } catch (Exception exception) {
            throw new IllegalStateException("无法识别 JSON 条件数据库方言", exception);
        }
    }

    /**
     * 取得 MPJ 当前表实例的别名与数据库列名，用于需要 SQL 列表达式的条件。
     */
    private static String resolveQualifiedColumnName(MpjWrapper<?> wrapper, BaseColumn<?> table, String property) {
        return wrapper.getAptIndex().get(table) + "." + resolveColumnName(table.getColumnClass(), property);
    }

    /**
     * 根据实体字段元数据将 Java 属性名解析为数据库列名。
     */
    static String resolveColumnName(Class<?> entityType, String property) {
        return EntityPropertyColumnResolver.resolve(entityType, property);
    }

    /**
     * 确认普通 MP 写入路径未携带必须由 MPJ 处理的 JOIN 或 FROM 条件。
     */
    private static void validatePlainWriteCriteria(ICriteria<?, ?> criteria) {
        if (criteria instanceof IRelatedQueryCriteria<?, ?> related
                && !related.getJoinConditions().isEmpty()
                || criteria instanceof FullTypeCriteria<?, ?> full
                && full.getFromCriteria() != null) {
            throw new UnsupportedOperationException("JOIN/FROM 条件需要 MPJ 查询执行器");
        }
    }

    /**
     * 从 Criteria 的泛型元数据获取当前查询的实体类型。
     */
    @SuppressWarnings("unchecked")
    private static <E extends IEntity<E>, C extends ICriteria<E, C>> Class<E> resolveEntityClass(ICriteria<E, C> criteria) {
        return (Class<E>) CriteriaEntityTypes.entityClass(criteria);
    }

}
