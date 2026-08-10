// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.field;

import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.ITraceEntity;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Collection;
import vip.isass.framework.nocode.property.PropertyGetter;

/**
 * 审计追踪类型查询条件接口
 *
 * @author Rain
 */
public interface ITraceCriteria<
        UPK extends Serializable,
        E extends ITraceEntity<UPK, E>,
        C extends ITraceCriteria<UPK, E, C>
        > extends ICriteria<E, C> {

    static <UPK extends Serializable, E extends ITraceEntity<UPK, E>> PropertyGetter<E, UPK> createUserIdGetter() {
        return ITraceEntity::getCreateUserId;
    }

    static <UPK extends Serializable, E extends ITraceEntity<UPK, E>> PropertyGetter<E, String> createUserNameGetter() {
        return ITraceEntity::getCreateUserName;
    }

    static <UPK extends Serializable, E extends ITraceEntity<UPK, E>> PropertyGetter<E, Long> createTimeGetter() {
        return ITraceEntity::getCreateTime;
    }

    static <UPK extends Serializable, E extends ITraceEntity<UPK, E>> PropertyGetter<E, UPK> modifyUserIdGetter() {
        return ITraceEntity::getModifyUserId;
    }

    static <UPK extends Serializable, E extends ITraceEntity<UPK, E>> PropertyGetter<E, String> modifyUserNameGetter() {
        return ITraceEntity::getModifyUserName;
    }

    static <UPK extends Serializable, E extends ITraceEntity<UPK, E>> PropertyGetter<E, Long> modifyTimeGetter() {
        return ITraceEntity::getModifyTime;
    }

    // endregion

    // region createUserId

    @Transient
    @SuppressWarnings({"rawtypes", "unchecked"})
    default UPK getCreateUserId() {
        return this instanceof IWhereConditionCriteria
                ? (UPK) ((IWhereConditionCriteria) this).getEquals(createUserIdGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserId(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserId(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdNotEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdNotEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C createUserIdIn(Collection<UPK> userIds) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(createUserIdGetter(), userIds)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orCreateUserIdIn(Collection<UPK> userIds) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(createUserIdGetter(), userIds)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C createUserIdNotIn(Collection<UPK> userIds) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(createUserIdGetter(), userIds)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orCreateUserIdNotIn(Collection<UPK> userIds) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(createUserIdGetter(), userIds)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(createUserIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setCreateUserIdIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(createUserIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(createUserIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setCreateUserIdIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(createUserIdGetter())
                : (C) this;
    }

    // endregion

    // region 字符串类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).like(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLike(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdNotLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notLike(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdNotLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotLike(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdStartWith(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).startWith(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdStartWith(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orStartWith(createUserIdGetter(), userId)
                : (C) this;
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdLessThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThan(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdLessThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThan(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdLessThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThanEqual(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdLessThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThanEqual(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdGreaterThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThan(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdGreaterThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThan(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserIdGreaterThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThanEqual(createUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserIdGreaterThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThanEqual(createUserIdGetter(), userId)
                : (C) this;
    }

    // endregion

    // endregion

    // region createUserName

    @Transient
    @SuppressWarnings("rawtypes")
    default String getCreateUserName() {
        return this instanceof IWhereConditionCriteria
                ? (String) ((IWhereConditionCriteria) this).getEquals(createUserNameGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserName(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserName(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameNotEqual(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserNameNotEqual(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C createUserNameIn(Collection<String> userNames) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(createUserNameGetter(), userNames)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orCreateUserNameIn(Collection<String> userNames) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(createUserNameGetter(), userNames)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C createUserNameNotIn(Collection<String> userNames) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(createUserNameGetter(), userNames)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orCreateUserNameNotIn(Collection<String> userNames) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(createUserNameGetter(), userNames)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(createUserNameGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setCreateUserNameIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserNameIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(createUserNameGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(createUserNameGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setCreateUserNameIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserNameIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(createUserNameGetter())
                : (C) this;
    }

    // endregion

    // region 字符串类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameLike(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).like(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserNameLike(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLike(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameNotLike(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notLike(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserNameNotLike(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotLike(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateUserNameStartWith(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).startWith(createUserNameGetter(), userName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateUserNameStartWith(String userName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orStartWith(createUserNameGetter(), userName)
                : (C) this;
    }

    // endregion

    // endregion

    // region createTime

    @Transient
    @SuppressWarnings("rawtypes")
    default Long getCreateTime() {
        return this instanceof IWhereConditionCriteria
                ? (Long) ((IWhereConditionCriteria) this).getEquals(createTimeGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTime(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTime(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeNotEqual(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTimeNotEqual(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C createTimeIn(Collection<Long> createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orCreateTimeIn(Collection<Long> createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C createTimeNotIn(Collection<Long> createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orCreateTimeNotIn(Collection<Long> createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(createTimeGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setCreateTimeIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTimeIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(createTimeGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(createTimeGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setCreateTimeIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTimeIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(createTimeGetter())
                : (C) this;
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeLessThan(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThan(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTimeLessThan(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThan(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeLessThanEqual(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThanEqual(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTimeLessThanEqual(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThanEqual(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeGreaterThan(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThan(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTimeGreaterThan(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThan(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setCreateTimeGreaterThanEqual(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThanEqual(createTimeGetter(), createTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrCreateTimeGreaterThanEqual(Long createTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThanEqual(createTimeGetter(), createTime)
                : (C) this;
    }

    // endregion

    // endregion

    // region modifyUserId

    @Transient
    @SuppressWarnings({"unchecked", "rawtypes"})
    default UPK getModifyUserId() {
        return this instanceof IWhereConditionCriteria
                ? (UPK) ((IWhereConditionCriteria) this).getEquals(modifyUserIdGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserId(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserId(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdNotEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdNotEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C modifyUserIdIn(Collection<UPK> userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orModifyUserIdIn(Collection<UPK> userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C modifyUserIdNotIn(Collection<UPK> userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orModifyUserIdNotIn(Collection<UPK> userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(modifyUserIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setModifyUserIdIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(modifyUserIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(modifyUserIdGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setModifyUserIdIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(modifyUserIdGetter())
                : (C) this;
    }

    // endregion

    // region 字符串类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).like(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLike(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdNotLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notLike(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdNotLike(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotLike(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdStartWith(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).startWith(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdStartWith(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orStartWith(modifyUserIdGetter(), userId)
                : (C) this;
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdLessThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThan(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdLessThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThan(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdLessThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThanEqual(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdLessThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThanEqual(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdGreaterThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThan(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdGreaterThan(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThan(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserIdGreaterThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThanEqual(modifyUserIdGetter(), userId)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserIdGreaterThanEqual(UPK userId) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThanEqual(modifyUserIdGetter(), userId)
                : (C) this;
    }

    // endregion

    // endregion

    // region modifyUserName

    @Transient
    @SuppressWarnings("rawtypes")
    default String getModifyUserName() {
        return this instanceof IWhereConditionCriteria
                ? (String) ((IWhereConditionCriteria) this).getEquals(modifyUserNameGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserName(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserName(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameNotEqual(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserNameNotEqual(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C modifyUserNameIn(Collection<String> modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orModifyUserNameIn(Collection<String> modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C modifyUserNameNotIn(Collection<String> modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orModifyUserNameNotIn(Collection<String> modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(modifyUserNameGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setModifyUserNameIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserNameIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(modifyUserNameGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(modifyUserNameGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setModifyUserNameIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserNameIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(modifyUserNameGetter())
                : (C) this;
    }

    // endregion

    // region 字符串类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameLike(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).like(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserNameLike(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLike(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameNotLike(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notLike(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserNameNotLike(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotLike(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyUserNameStartWith(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).startWith(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyUserNameStartWith(String modifyUserName) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orStartWith(modifyUserNameGetter(), modifyUserName)
                : (C) this;
    }

    // endregion

    // endregion

    // region modifyTime

    @Transient
    @SuppressWarnings("rawtypes")
    default Long getModifyTime() {
        return this instanceof IWhereConditionCriteria
                ? (Long) ((IWhereConditionCriteria) this).getEquals(modifyTimeGetter())
                : null;
    }

    // region 所有类型都有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTime(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).equals(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTime(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orEquals(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeNotEqual(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notEquals(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTimeNotEqual(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotEquals(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C modifyTimeIn(Collection<Long> modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).in(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orModifyTimeIn(Collection<Long> modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIn(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C modifyTimeNotIn(Collection<Long> modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).notIn(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orModifyTimeNotIn(Collection<Long> modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orNotIn(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(modifyTimeGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setModifyTimeIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTimeIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNull(modifyTimeGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(modifyTimeGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setModifyTimeIsNotNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTimeIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orIsNotNull(modifyTimeGetter())
                : (C) this;
    }

    // endregion

    // region 数字类型字段拥有的条件

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeLessThan(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThan(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTimeLessThan(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThan(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeLessThanEqual(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).lessThanEqual(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTimeLessThanEqual(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orLessThanEqual(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeGreaterThan(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThan(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTimeGreaterThan(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThan(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setModifyTimeGreaterThanEqual(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).greaterThanEqual(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setOrModifyTimeGreaterThanEqual(Long modifyTime) {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).orGreaterThanEqual(modifyTimeGetter(), modifyTime)
                : (C) this;
    }

    // endregion

    // endregion

    // region 时间字段排序方法

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orderByCreateTimeDescIfBlank() {
        return this instanceof IOrderByCriteria
                ? (C) ((IOrderByCriteria) this).orderByIfBlank(createTimeGetter(), IOrderByCriteria.DESC)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orderByModifyTimeDescIfBlank() {
        return this instanceof IOrderByCriteria
                ? (C) ((IOrderByCriteria) this).orderByIfBlank(modifyTimeGetter(), IOrderByCriteria.DESC)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orderByCreateTimeDesc() {
        return this instanceof IOrderByCriteria
                ? (C) ((IOrderByCriteria) this).orderBy(createTimeGetter(), IOrderByCriteria.DESC)
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C orderByModifyTimeDesc() {
        return this instanceof IOrderByCriteria
                ? (C) ((IOrderByCriteria) this).orderBy(modifyTimeGetter(), IOrderByCriteria.DESC)
                : (C) this;
    }

    // endregion

}
