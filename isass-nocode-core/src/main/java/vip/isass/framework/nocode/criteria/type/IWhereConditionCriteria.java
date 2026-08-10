// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.type;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.WhereCondition;
import vip.isass.framework.nocode.criteria.impl.type.Condition;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.property.PropertyGetter;
import vip.isass.framework.nocode.property.PropertyNameResolver;

import java.beans.Transient;
import java.util.Collection;
import java.util.List;

/**
 * sql 的 where 条件接口
 * 设置条件时，value 必需做校验不能为空，否则在 update 或 delete 时，因为业务传了 null 过来，而被忽略的话，会导致全表数据被修改。
 * todo 以后可做优化，在给 wrapper 赋值时，可判断 wrapper 类型，如果是 queryWrapper，可忽略 null 值，如果是 updateWrapper，则不能为 null
 *
 * @author Rain
 */
public interface IWhereConditionCriteria<E extends IEntity<E>, C extends IWhereConditionCriteria<E, C>>
    extends ICriteria<E, C> {

    /**
     * whereConditions 相关方法
     *
     * @return where condition list
     */
    List<WhereCondition> getWhereConditions();

    @Transient
    default String propertyName(PropertyGetter<E, ?> propertyGetter) {
        return PropertyNameResolver.resolve(propertyGetter);
    }

    /**
     * 前端筛选控件常会提交未填写的参数，这类值不应转化为数据库条件。
     */
    default boolean isConditionValuePresent(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence) {
            return StrUtil.isNotBlank((CharSequence) value);
        }
        if (value instanceof Collection) {
            return CollUtil.isNotEmpty((Collection<?>) value);
        }
        if (value instanceof java.util.Map) {
            return !((java.util.Map<?, ?>) value).isEmpty();
        }
        return !value.getClass().isArray() || java.lang.reflect.Array.getLength(value) > 0;
    }

    @SuppressWarnings("unchecked")
    default C setWhereConditions(List<WhereCondition> whereConditions) {
        getWhereConditions().clear();
        if (CollUtil.isNotEmpty(whereConditions)) {
            getWhereConditions().addAll(whereConditions);
        }
        return (C) this;
    }

    // region 所有类型都有的条件

    /**
     * 添加查询条件
     *
     * @param propertyName propertyName
     * @param columnName   columnName
     * @param value        value
     */
    @SuppressWarnings("unchecked")
    default C equals(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(propertyName, Condition.EQUAL, value));
        return (C) this;
    }

    default C equals(PropertyGetter<E, ?> propertyGetter, Object value) {
        return equals(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getEquals(String propertyName) {
        return getValue(propertyName, Condition.EQUAL);
    }

    default <T> T getEquals(PropertyGetter<E, ?> propertyGetter) {
        return getEquals(propertyName(propertyGetter));
    }

    @Transient
    default <T> T getEquals(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.EQUAL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orEquals(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.EQUAL, value));
        return (C) this;
    }

    default C orEquals(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orEquals(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrEquals(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.EQUAL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C notEquals(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(propertyName, Condition.NOT_EQUAL, value));
        return (C) this;
    }

    default C notEquals(PropertyGetter<E, ?> propertyGetter, Object value) {
        return notEquals(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getNotEquals(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.NOT_EQUAL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orNotEquals(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.NOT_EQUAL, value));
        return (C) this;
    }

    default C orNotEquals(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orNotEquals(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrNotEquals(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.NOT_EQUAL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C in(String propertyName, Collection<?> values) {
        Assert.notEmpty(values, "values不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IN, values));
        return (C) this;
    }

    default C in(PropertyGetter<E, ?> propertyGetter, Collection<?> values) {
        return in(propertyName(propertyGetter), values);
    }

    @Transient
    default <T> T getIn(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.IN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orIn(String propertyName, Collection<?> values) {
        Assert.notEmpty(values, "values不能为空");
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IN, values));
        return (C) this;
    }

    default C orIn(PropertyGetter<E, ?> propertyGetter, Collection<?> values) {
        return orIn(propertyName(propertyGetter), values);
    }

    @Transient
    default <T> T getOrIn(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.IN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C notIn(String propertyName, Collection<?> values) {
        Assert.notEmpty(values, "values不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.NOT_IN, values));
        return (C) this;
    }

    default C notIn(PropertyGetter<E, ?> propertyGetter, Collection<?> values) {
        return notIn(propertyName(propertyGetter), values);
    }

    @Transient
    default <T> T getNotIn(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.NOT_IN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orNotIn(String propertyName, Collection<?> values) {
        Assert.notEmpty(values, "values不能为空");
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.NOT_IN, values));
        return (C) this;
    }

    default C orNotIn(PropertyGetter<E, ?> propertyGetter, Collection<?> values) {
        return orNotIn(propertyName(propertyGetter), values);
    }

    @Transient
    default <T> T getOrNotIn(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.NOT_IN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C isNull(String propertyName) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IS_NULL, null));
        return (C) this;
    }

    default C isNull(PropertyGetter<E, ?> propertyGetter) {
        return isNull(propertyName(propertyGetter));
    }

    @Transient
    default <T> T getIsNull(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.IS_NULL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orIsNull(String propertyName) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IS_NULL, null));
        return (C) this;
    }

    default C orIsNull(PropertyGetter<E, ?> propertyGetter) {
        return orIsNull(propertyName(propertyGetter));
    }

    @Transient
    default <T> T getOrIsNull(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.IS_NULL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C isNotNull(String propertyName) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IS_NOT_NULL, null));
        return (C) this;
    }

    default C isNotNull(PropertyGetter<E, ?> propertyGetter) {
        return isNotNull(propertyName(propertyGetter));
    }

    @Transient
    default <T> T getIsNotNull(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.IS_NOT_NULL, clazz);
    }


    @SuppressWarnings("unchecked")
    default C orIsNotNull(String propertyName) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IS_NOT_NULL, null));
        return (C) this;
    }

    default C orIsNotNull(PropertyGetter<E, ?> propertyGetter) {
        return orIsNotNull(propertyName(propertyGetter));
    }

    @Transient
    default <T> T getOrIsNotNull(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.IS_NOT_NULL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C isEmpty(String propertyName) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IS_EMPTY, null));
        return (C) this;
    }

    default C isEmpty(PropertyGetter<E, ?> propertyGetter) {
        return isEmpty(propertyName(propertyGetter));
    }

    @SuppressWarnings("unchecked")
    default C isNotEmpty(String propertyName) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.IS_NOT_EMPTY, null));
        return (C) this;
    }

    default C isNotEmpty(PropertyGetter<E, ?> propertyGetter) {
        return isNotEmpty(propertyName(propertyGetter));
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings("unchecked")
    default C lessThan(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(propertyName, Condition.LESS_THAN, value));
        return (C) this;
    }

    default C lessThan(PropertyGetter<E, ?> propertyGetter, Object value) {
        return lessThan(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getLessThan(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.LESS_THAN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orLessThan(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.LESS_THAN, value));
        return (C) this;
    }

    default C orLessThan(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orLessThan(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrLessThan(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.LESS_THAN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C lessThanEqual(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(propertyName, Condition.LESS_THAN_EQUAL, value));
        return (C) this;
    }

    default C lessThanEqual(PropertyGetter<E, ?> propertyGetter, Object value) {
        return lessThanEqual(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getLessThanEqual(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.LESS_THAN_EQUAL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orLessThanEqual(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.LESS_THAN_EQUAL, value));
        return (C) this;
    }

    default C orLessThanEqual(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orLessThanEqual(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrLessThanEqual(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.LESS_THAN_EQUAL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C greaterThan(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(propertyName, Condition.GREATER_THAN, value));
        return (C) this;
    }

    default C greaterThan(PropertyGetter<E, ?> propertyGetter, Object value) {
        return greaterThan(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getGreaterThan(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.GREATER_THAN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orGreaterThan(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.GREATER_THAN, value));
        return (C) this;
    }

    default C orGreaterThan(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orGreaterThan(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrGreaterThan(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.GREATER_THAN, clazz);
    }

    @SuppressWarnings("unchecked")
    default C greaterThanEqual(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(propertyName, Condition.GREATER_THAN_EQUAL, value));
        return (C) this;
    }

    default C greaterThanEqual(PropertyGetter<E, ?> propertyGetter, Object value) {
        return greaterThanEqual(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getGreaterThanEqual(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.GREATER_THAN_EQUAL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orGreaterThanEqual(String propertyName, Object value) {
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.GREATER_THAN_EQUAL, value));
        return (C) this;
    }

    default C orGreaterThanEqual(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orGreaterThanEqual(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrGreaterThanEqual(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.GREATER_THAN_EQUAL, clazz);
    }

    // endregion

    // region 字符串类型字段拥有的条件

    @SuppressWarnings("unchecked")
    default C like(String propertyName, Object value) {
        Assert.notNull(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.LIKE, value));
        return (C) this;
    }

    default C like(PropertyGetter<E, ?> propertyGetter, Object value) {
        return like(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getLike(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.LIKE, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orLike(String propertyName, Object value) {
        Assert.notNull(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.LIKE, value));
        return (C) this;
    }

    default C orLike(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orLike(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrLike(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.LIKE, clazz);
    }

    @SuppressWarnings("unchecked")
    default C notLike(String propertyName, Object value) {
        Assert.notNull(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.NOT_LIKE, value));
        return (C) this;
    }

    default C notLike(PropertyGetter<E, ?> propertyGetter, Object value) {
        return notLike(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getNotLike(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.NOT_LIKE, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orNotLike(String propertyName, Object value) {
        Assert.notNull(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.NOT_LIKE, value));
        return (C) this;
    }

    default C orNotLike(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orNotLike(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrNotLike(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.NOT_LIKE, clazz);
    }

    @SuppressWarnings("unchecked")
    default C startWith(String propertyName, Object value) {
        Assert.notNull(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.START_WITH, value));
        return (C) this;
    }

    default C startWith(PropertyGetter<E, ?> propertyGetter, Object value) {
        return startWith(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getStartWith(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.START_WITH, clazz);
    }

    @SuppressWarnings("unchecked")
    default C orStartWith(String propertyName, Object value) {
        Assert.notNull(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(null, Condition.OR, null));
        getWhereConditions().add(new WhereCondition(propertyName, Condition.START_WITH, value));
        return (C) this;
    }

    default C orStartWith(PropertyGetter<E, ?> propertyGetter, Object value) {
        return orStartWith(propertyName(propertyGetter), value);
    }

    @Transient
    default <T> T getOrStartWith(String propertyName, Class<T> clazz) {
        return getOrValue(propertyName, Condition.START_WITH, clazz);
    }

    // endregion

    // region json 类型字段拥有的条件

    @SuppressWarnings("unchecked")
    default C collectionContainsAll(String propertyName, Collection<String> value) {
        Assert.notBlank(propertyName, "propertyName不能为空");
        Assert.notEmpty(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.CONTAINS_ALL, value));
        return (C) this;
    }

    @Transient
    default <T> T getCollectionContainsAll(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.CONTAINS_ALL, clazz);
    }

    @SuppressWarnings("unchecked")
    default C collectionContainsAny(String propertyName, Collection<String> value) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        Assert.notEmpty(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.CONTAINS_ANY, value));
        return (C) this;
    }

    @Transient
    default <T> T getCollectionContainsAny(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.CONTAINS_ANY, clazz);
    }

    @SuppressWarnings("unchecked")
    default C jsonArrayContains(String propertyName, Object value) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        Assert.notNull(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.JSON_ARRAY_CONTAINS, value));
        return (C) this;
    }

    @Transient
    default <T> T getMysqlJsonArrayContains(String propertyName, Class<T> clazz) {
        return getValue(propertyName, Condition.JSON_ARRAY_CONTAINS, clazz);
    }

    @SuppressWarnings("unchecked")
    default C jsonArrayContainsAny(String propertyName, Collection<?> value) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        Assert.notEmpty(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.JSON_ARRAY_CONTAINS_ANY, value));
        return (C) this;
    }

    @Transient
    default <T> Collection<T> getJsonArrayContainsAny(String propertyName) {
        return getValue(propertyName, Condition.JSON_ARRAY_CONTAINS_ANY);
    }

    @SuppressWarnings("unchecked")
    default C jsonArrayContainsAll(String propertyName, Collection<?> value) {
        Assert.notBlank(propertyName, "propertyName 不能为空");
        Assert.notEmpty(value, "value不能为空");
        getWhereConditions().add(new WhereCondition(propertyName, Condition.JSON_ARRAY_CONTAINS_ALL, value));
        return (C) this;
    }

    @Transient
    default <T> Collection<T> getJsonArrayContainsAll(String propertyName) {
        return getValue(propertyName, Condition.JSON_ARRAY_CONTAINS_ALL);
    }

    // endregion

    @SuppressWarnings("unchecked")
    default <T> T getValue(String propertyName, Condition condition) {
        return getWhereConditions().stream()
            .filter(c -> condition == c.getCondition())
            .filter(c -> propertyName.equalsIgnoreCase(c.getPropertyName()))
            .map(c -> (T) c.getValue())
            .findFirst()
            .orElse(null);
    }

    @Transient
    @SuppressWarnings("unchecked")
    default <T> T getValue(String propertyName, Condition condition, Class<T> clazz) {
        return getWhereConditions().stream()
            .filter(c -> condition == c.getCondition())
            .filter(c -> propertyName.equalsIgnoreCase(c.getPropertyName()))
            .map(c -> (T) c.getValue())
            .findFirst()
            .orElse(null);
    }

    @Transient
    @SuppressWarnings("unchecked")
    default <T> T getOrValue(String propertyName, Condition condition, Class<T> clazz) {
        for (int i = 0; i < getWhereConditions().size(); i++) {
            List<WhereCondition> whereConditions = getWhereConditions();
            WhereCondition whereCondition = whereConditions.get(i);
            if (i == 0
                || !propertyName.equals(whereCondition.getPropertyName())
                || whereCondition.getCondition() != condition) {
                continue;
            }
            if (whereConditions.get(i - 1).getCondition() != Condition.OR) {
                continue;
            }
            return (T) whereCondition.getValue();
        }
        return null;
    }

    default boolean hasCondition(String propertyName, Condition condition) {
        return getWhereConditions().stream()
            .filter(c -> condition == c.getCondition())
            .anyMatch(c -> propertyName.equalsIgnoreCase(c.getPropertyName()));
    }

    default boolean hasConditions() {
        return !getWhereConditions().isEmpty();
    }

}
