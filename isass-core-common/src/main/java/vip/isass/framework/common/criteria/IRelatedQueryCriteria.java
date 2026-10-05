// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.criteria.type.IReturnFieldCriteria;
import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.property.PropertyGetter;
import vip.isass.framework.common.property.PropertyNameResolver;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 关系查询的公共条件 API；只保存属性及 Criteria，不保存表名或 ORM 对象。
 * 构造时保留输入引用；JOIN 设置传入条件的 joinType，相关 EXISTS 追加目标条件，IN 选列重载设置目标投影。
 * 需要独立条件树时由调用方显式 copy；仅共享的 EmptyCriteria 在写入前物化。
 */
public interface IRelatedQueryCriteria<E extends IEntity<E>, C extends IRelatedQueryCriteria<E, C>>
        extends IWhereConditionCriteria<E, C> {
    List<RelatedCondition> getLoadRelated();
    C setLoadRelated(List<RelatedCondition> conditions);
    List<JoinCondition> getJoinConditions();
    C setJoinConditions(List<JoinCondition> conditions);

    default C loadRelated(String property) { return loadRelated(property, null); }

    default C loadRelated(String property, ICriteria<?, ?> criteria) {
        return loadRelated(new RelatedCondition().setProperty(property).setCriteria(criteria));
    }

    @SuppressWarnings("unchecked")
    default C loadRelated(RelatedCondition condition) {
        Objects.requireNonNull(condition, "condition");
        getLoadRelated().removeIf(existing -> Objects.equals(existing.getProperty(), condition.getProperty()));
        getLoadRelated().add(condition);
        return (C) this;
    }

    default <T extends IEntity<T>, V> C leftJoin(Class<T> type,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return leftJoin(EmptyCriteria.of(type), source, target);
    }

    default <T extends IEntity<T>, V> C leftJoin(ICriteria<T, ?> criteria,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return leftJoin(new JoinCondition().setTargetCriteria(criteria)
                .setSourceProperty(source).setCondition(Condition.Compare.EQUAL).setTargetProperty(target));
    }

    default C leftJoin(JoinCondition condition) { return addJoin(JoinType.LEFT, condition); }

    default <T extends IEntity<T>, V> C innerJoin(Class<T> type,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return innerJoin(EmptyCriteria.of(type), source, target);
    }

    default <T extends IEntity<T>, V> C innerJoin(ICriteria<T, ?> criteria,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return innerJoin(new JoinCondition().setTargetCriteria(criteria)
                .setSourceProperty(source).setCondition(Condition.Compare.EQUAL).setTargetProperty(target));
    }

    default C innerJoin(JoinCondition condition) { return addJoin(JoinType.INNER, condition); }

    default <T extends IEntity<T>, V> C rightJoin(Class<T> type,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return rightJoin(EmptyCriteria.of(type), source, target);
    }

    default <T extends IEntity<T>, V> C rightJoin(ICriteria<T, ?> criteria,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return rightJoin(new JoinCondition().setTargetCriteria(criteria)
                .setSourceProperty(source).setCondition(Condition.Compare.EQUAL).setTargetProperty(target));
    }

    default C rightJoin(JoinCondition condition) { return addJoin(JoinType.RIGHT, condition); }

    default <T extends IEntity<T>, V> C fullJoin(Class<T> type,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return fullJoin(EmptyCriteria.of(type), source, target);
    }

    default <T extends IEntity<T>, V> C fullJoin(ICriteria<T, ?> criteria,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return fullJoin(new JoinCondition().setTargetCriteria(criteria)
                .setSourceProperty(source).setCondition(Condition.Compare.EQUAL).setTargetProperty(target));
    }

    default C fullJoin(JoinCondition condition) { return addJoin(JoinType.FULL, condition); }

    default <T extends IEntity<T>> C crossJoin(Class<T> type) {
        return crossJoin(EmptyCriteria.of(type));
    }

    default C crossJoin(ICriteria<?, ?> criteria) {
        return crossJoin(new JoinCondition().setTargetCriteria(criteria));
    }

    default C crossJoin(JoinCondition condition) { return addJoin(JoinType.CROSS, condition); }

    @SuppressWarnings("unchecked")
    private C addJoin(JoinType type, JoinCondition condition) {
        Objects.requireNonNull(condition, "condition").setJoinType(type);
        getJoinConditions().add(condition);
        return (C) this;
    }

    default C exists(ICriteria<?, ?> criteria) {
        return addSubquery(null, Condition.Existence.EXISTS, criteria);
    }

    default C notExists(ICriteria<?, ?> criteria) {
        return addSubquery(null, Condition.Existence.NOT_EXISTS, criteria);
    }

    default <T extends IEntity<T>, V> C exists(ICriteria<T, ?> criteria,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return addSubquery(null, Condition.Existence.EXISTS, correlate(criteria, source, target));
    }

    default <T extends IEntity<T>, V> C notExists(ICriteria<T, ?> criteria,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        return addSubquery(null, Condition.Existence.NOT_EXISTS, correlate(criteria, source, target));
    }

    private <T extends IEntity<T>, V> ICriteria<T, ?> correlate(ICriteria<T, ?> criteria,
            PropertyGetter<E, V> source, PropertyGetter<T, V> target) {
        ICriteria<T, ?> targetCriteria = CriteriaEntityTypes.materializeForWrite(Objects.requireNonNull(criteria, "targetCriteria"));
        if (!(targetCriteria instanceof IWhereConditionCriteria<?, ?> where)) {
            throw new IllegalArgumentException("关联子查询需要支持 WHERE 的具体 Criteria");
        }
        if (!where.getWhereConditions().isEmpty()) {
            where.setWhereConditions(new ArrayList<>(List.of(new WhereCondition().setChildren(where.getWhereConditions()))));
        }
        where.getWhereConditions().add(new WhereCondition()
                .setSourceProperty(source).setCondition(Condition.Compare.EQUAL).setTargetProperty(target));
        return targetCriteria;
    }

    default C in(String source, ICriteria<?, ?> criteria) {
        return addSubquery(source, Condition.Membership.IN, criteria);
    }

    default C notIn(String source, ICriteria<?, ?> criteria) {
        return addSubquery(source, Condition.Membership.NOT_IN, criteria);
    }

    default C in(PropertyGetter<E, ?> source, ICriteria<?, ?> criteria) {
        return in(PropertyNameResolver.resolve(source), criteria);
    }

    default C notIn(PropertyGetter<E, ?> source, ICriteria<?, ?> criteria) {
        return notIn(PropertyNameResolver.resolve(source), criteria);
    }

    default <T extends IEntity<T>, V> C in(PropertyGetter<E, V> source, ICriteria<T, ?> criteria,
            PropertyGetter<T, V> select) {
        return addSubquery(PropertyNameResolver.resolve(source), Condition.Membership.IN, projected(criteria, select));
    }

    default <T extends IEntity<T>, V> C notIn(PropertyGetter<E, V> source, ICriteria<T, ?> criteria,
            PropertyGetter<T, V> select) {
        return addSubquery(PropertyNameResolver.resolve(source), Condition.Membership.NOT_IN, projected(criteria, select));
    }

    private <T extends IEntity<T>, V> ICriteria<T, ?> projected(ICriteria<T, ?> criteria,
            PropertyGetter<T, V> select) {
        ICriteria<T, ?> targetCriteria = CriteriaEntityTypes.materializeForWrite(Objects.requireNonNull(criteria, "targetCriteria"));
        if (!(targetCriteria instanceof IReturnFieldCriteria<?, ?> columns)) {
            throw new IllegalArgumentException("IN 子查询需要支持 returnFields 的具体 Criteria");
        }
        columns.setReturnField(PropertyNameResolver.resolve(select));
        return targetCriteria;
    }

    @SuppressWarnings("unchecked")
    private C addSubquery(String source, Condition condition, ICriteria<?, ?> criteria) {
        getWhereConditions().add(new WhereCondition(source, condition, null)
                .setTargetCriteria(Objects.requireNonNull(criteria, "targetCriteria")));
        return (C) this;
    }
}
