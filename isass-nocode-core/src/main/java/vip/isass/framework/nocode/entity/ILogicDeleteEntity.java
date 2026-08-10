// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.beans.Transient;

/**
 * 逻辑删除
 *
 * @author Rain
 */
public interface ILogicDeleteEntity<E extends ILogicDeleteEntity<E>> extends IEntity<E> {

    Boolean DEFAULT_DELETE_FLAG_VALUE = Boolean.FALSE;

    /** MyBatis-Plus 中未删除记录的数据库值。 */
    String NOT_DELETED_VALUE = "0";

    /** MyBatis-Plus 中已删除记录的数据库值。 */
    String DELETED_VALUE = "1";

    /**
     * 获取删除标识
     *
     * @return deleteFlag
     */
    Boolean getDeleteFlag();

    /**
     * 设置删除标识
     *
     * @param deleteFlag deleteFlag
     */
    void setDeleteFlag(Boolean deleteFlag);

    /**
     * 如果删除标识为 null, 则设置删除标识为 false，并返回删除标识
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    default E computeDefaultDeleteFlagIfAbsent() {
        if (getDeleteFlag() == null) {
            setDeleteFlag(Boolean.FALSE);
        }
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    default E randomEntity() {
        setDeleteFlag(randomBoolean());
        return (E) this;
    }

}
