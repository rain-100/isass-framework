// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.field;

import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IVersionEntity;
import vip.isass.framework.nocode.criteria.type.ISelectColumnCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;

import java.beans.Transient;
import vip.isass.framework.nocode.property.PropertyGetter;

/**
 * 乐观锁版本号 类型条件接口
 *
 * @author Rain
 */
public interface IVersionCriteria<
    E extends IVersionEntity<E>,
    C extends IVersionCriteria<E, C>
    > extends ICriteria<E, C> {

    static <E extends IVersionEntity<E>> PropertyGetter<E, Integer> versionGetter() {
        return IVersionEntity::getVersion;
    }

    @Transient
    @SuppressWarnings({"rawtypes"})
    default Integer getVersion() {
        return this instanceof IWhereConditionCriteria
            ? (Integer) ((IWhereConditionCriteria) this).getEquals(versionGetter())
            : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setVersion(Integer id) {
        return this instanceof IWhereConditionCriteria
            ? (C) ((IWhereConditionCriteria) this).equals(versionGetter(), id)
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setVersionIsNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNull(versionGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setVersionIsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setVersionIsNull() : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setVersionIsNotNull() {
        return this instanceof IWhereConditionCriteria
                ? (C) ((IWhereConditionCriteria) this).isNotNull(versionGetter())
                : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C setVersionIsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? setVersionIsNotNull() : (C) this;
    }

    // region SelectColumnCriteria

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C selectVersion() {
        return this instanceof ISelectColumnCriteria
            ? (C) ((ISelectColumnCriteria) this).setSelectColumn(versionGetter())
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C addSelectVersion() {
        return this instanceof ISelectColumnCriteria
            ? (C) ((ISelectColumnCriteria) this).addSelectColumn(versionGetter())
            : (C) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default C unSelectVersion() {
        return this instanceof ISelectColumnCriteria
            ? (C) ((ISelectColumnCriteria) this).unSelectColumn(versionGetter())
            : (C) this;
    }

    // endregion

}
