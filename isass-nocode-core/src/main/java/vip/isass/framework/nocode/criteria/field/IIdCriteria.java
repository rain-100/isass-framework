// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.field;

import vip.isass.framework.nocode.criteria.type.ISelectColumnCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;

import java.beans.Transient;
import java.io.Serializable;
import vip.isass.framework.nocode.property.PropertyGetter;
import java.util.Collection;

/**
 * 主键类型条件接口
 *
 * @author Rain
 */
public interface IIdCriteria<PK extends Serializable, E extends IIdEntity<PK, E>, C extends IIdCriteria<PK, E, C>>
        extends IPkCriteria<PK, E, C> {

    static <PK extends Serializable, E extends IIdEntity<PK, E>> PropertyGetter<E, PK> idGetter() {
        return IIdEntity::getId;
    }

    @Transient
    @SuppressWarnings({"rawtypes", "unchecked"})
    default PK getId() {
        return this instanceof IWhereConditionCriteria
                ? (PK) ((IWhereConditionCriteria) this).getEquals(idGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setId(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(idGetter(), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrId(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(((IWhereConditionCriteria) this).propertyName(idGetter()), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdNotEqual(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(((IWhereConditionCriteria) this).propertyName(idGetter()), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdNotEqual(PK id) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(((IWhereConditionCriteria) this).propertyName(idGetter()), id)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(idGetter(), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(((IWhereConditionCriteria) this).propertyName(idGetter()), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdNotIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(((IWhereConditionCriteria) this).propertyName(idGetter()), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdNotIn(Collection<PK> ids) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(((IWhereConditionCriteria) this).propertyName(idGetter()), ids)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(idGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setIdIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(((IWhereConditionCriteria) this).propertyName(idGetter()))
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(idGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setIdIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(((IWhereConditionCriteria) this).propertyName(idGetter()))
                : (C) this;
    }

    // endregion

    // region 字符串类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdLike(PK idLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).like(((IWhereConditionCriteria) this).propertyName(idGetter()), idLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdLike(PK orIdLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLike(((IWhereConditionCriteria) this).propertyName(idGetter()), orIdLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdNotLike(PK idNotLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notLike(((IWhereConditionCriteria) this).propertyName(idGetter()), idNotLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdNotLike(PK orIdNotLike) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotLike(((IWhereConditionCriteria) this).propertyName(idGetter()), orIdNotLike)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdStartWith(PK idStartWith) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).startWith(((IWhereConditionCriteria) this).propertyName(idGetter()), idStartWith)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdStartWith(PK orIdStartWith) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orStartWith(((IWhereConditionCriteria) this).propertyName(idGetter()), orIdStartWith)
                : (C) this;
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdLessThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThan(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdLessThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThan(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdLessThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThanEqual(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdLessThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThanEqual(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdGreaterThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThan(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdGreaterThan(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThan(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setIdGreaterThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThanEqual(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrIdGreaterThanEqual(PK value) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThanEqual(((IWhereConditionCriteria) this).propertyName(idGetter()), value)
                : (C) this;
    }

    // endregion

    // region SelectColumnCriteria

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C selectId() {
        return this instanceof ISelectColumnCriteria
                ? (C) ((ISelectColumnCriteria) this).setSelectColumn((PropertyGetter) idGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C addSelectId() {
        return this instanceof ISelectColumnCriteria
                ? (C) ((ISelectColumnCriteria) this).addSelectColumn((PropertyGetter) idGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C unSelectId() {
        return this instanceof ISelectColumnCriteria
                ? (C) ((ISelectColumnCriteria) this).unSelectColumn((PropertyGetter) idGetter())
                : (C) this;
    }

    // endregion

}
