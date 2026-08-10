// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.field;

import vip.isass.framework.nocode.criteria.type.ISelectColumnCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.IParentIdEntity;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Collection;
import vip.isass.framework.nocode.property.PropertyGetter;

/**
 * 父id类型条件接口
 *
 * @author Rain
 */
public interface IParentIdCriteria<
        PK extends Serializable,
        E extends IParentIdEntity<PK, E>,
        C extends IParentIdCriteria<PK, E, C>
        > extends IPkCriteria<PK, E, C> {

    static <PK extends Serializable, E extends IParentIdEntity<PK, E>> PropertyGetter<E, PK> parentIdGetter() {
        return IParentIdEntity::getParentId;
    }

    @Transient
    @SuppressWarnings({"unchecked", "rawtypes"})
    default PK getParentId() {
        return this instanceof IWhereConditionCriteria
                ? (PK) ((IWhereConditionCriteria) this).getEquals(parentIdGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentId(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(parentIdGetter(), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentId(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(parentIdGetter(), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdNotEqual(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(parentIdGetter(), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdNotEqual(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(parentIdGetter(), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(parentIdGetter(), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(parentIdGetter(), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdNotIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(parentIdGetter(), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdNotIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(parentIdGetter(), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(parentIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setParentIdIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(parentIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(parentIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setParentIdIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(parentIdGetter())
                : (C) this;
    }

    // endregion

    // region 字符串类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdLike(PK idLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).like(parentIdGetter(), idLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdLike(PK orParentIdLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLike(parentIdGetter(), orParentIdLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdNotLike(PK idNotLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notLike(parentIdGetter(), idNotLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdNotLike(PK orParentIdNotLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotLike(parentIdGetter(), orParentIdNotLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdStartWith(PK idStartWith) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).startWith(parentIdGetter(), idStartWith)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdStartWith(PK orParentIdStartWith) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orStartWith(parentIdGetter(), orParentIdStartWith)
                : (C) this;
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdLessThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThan(parentIdGetter(), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdLessThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThan(parentIdGetter(), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdLessThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThanEqual(parentIdGetter(), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdLessThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThanEqual(parentIdGetter(), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdGreaterThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThan(parentIdGetter(), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdGreaterThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThan(parentIdGetter(), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setParentIdGreaterThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThanEqual(parentIdGetter(), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrParentIdGreaterThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThanEqual(parentIdGetter(), value)
                : (C) this;
    }

    // endregion

    // region SelectColumnCriteria

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C selectParentId() {
        return this instanceof ISelectColumnCriteria
                ? (C) ((ISelectColumnCriteria) this).setSelectColumn(parentIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C addSelectParentId() {
        return this instanceof ISelectColumnCriteria
                ? (C) ((ISelectColumnCriteria) this).addSelectColumn(parentIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C unSelectParentId() {
        return this instanceof ISelectColumnCriteria
                ? (C) ((ISelectColumnCriteria) this).unSelectColumn(parentIdGetter())
                : (C) this;
    }

    // endregion

}
