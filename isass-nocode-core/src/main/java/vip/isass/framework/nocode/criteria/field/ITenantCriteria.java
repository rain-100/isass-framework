// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.field;

import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.criteria.type.ISelectColumnCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Collection;
import vip.isass.framework.nocode.property.PropertyGetter;

/**
 * 租户 类型条件接口
 *
 * @author Rain
 */
public interface ITenantCriteria<
    TPK extends Serializable,
    E extends ITenantEntity<TPK, E>,
    C extends ITenantCriteria<TPK, E, C>
    > extends ICriteria<E, C> {

    static <TPK extends Serializable, E extends ITenantEntity<TPK, E>> PropertyGetter<E, TPK> tenantIdGetter() {
        return ITenantEntity::getTenantId;
    }

    @Transient
    @SuppressWarnings({"rawtypes"})
    default Long getTenantId() {
        return this instanceof IWhereConditionCriteria
            ? (Long) ((IWhereConditionCriteria) this).getEquals(tenantIdGetter())
            : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantId(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).equals(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantId(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orEquals(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdNotEqual(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).notEquals(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdNotEqual(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orNotEquals(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdIn(Collection<Long> tenantIds) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).in(tenantIdGetter(), tenantIds)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdIn(Collection<Long> tenantIds) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orIn(tenantIdGetter(), tenantIds)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdNotIn(Collection<Long> tenantIds) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).notIn(tenantIdGetter(), tenantIds)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdNotIn(Collection<Long> tenantIds) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orNotIn(tenantIdGetter(), tenantIds)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdIsNull() {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).isNull(tenantIdGetter())
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setTenantIdIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdIsNull() {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orIsNull(tenantIdGetter())
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).isNotNull(tenantIdGetter())
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setTenantIdIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orIsNotNull(tenantIdGetter())
            : (C) this;
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdLessThan(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).lessThan(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdLessThan(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orLessThan(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdLessThanEqual(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).lessThanEqual(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdLessThanEqual(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orLessThanEqual(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdGreaterThan(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).greaterThan(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdGreaterThan(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orGreaterThan(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setTenantIdGreaterThanEqual(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).greaterThanEqual(tenantIdGetter(), tenantId)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrTenantIdGreaterThanEqual(Long tenantId) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).orGreaterThanEqual(tenantIdGetter(), tenantId)
            : (C) this;
    }

    // endregion

    // region SelectColumnCriteria

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C selectTenantId() {
        return this instanceof ISelectColumnCriteria
            ? (C) ((ISelectColumnCriteria) this).setSelectColumn(tenantIdGetter())
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C addSelectTenantId() {
        return this instanceof ISelectColumnCriteria
            ? (C) ((ISelectColumnCriteria) this).addSelectColumn(tenantIdGetter())
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C unSelectTenantId() {
        return this instanceof ISelectColumnCriteria
            ? (C) ((ISelectColumnCriteria) this).unSelectColumn(tenantIdGetter())
            : (C) this;
    }

    // endregion

}
