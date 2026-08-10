// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import vip.isass.framework.common.sequence.impl.LongSequence;

import java.beans.Transient;
import java.io.Serializable;

/**
 * 审计追踪类型实体
 * UPK: user 的主键类型
 *
 * @author Rain
 */
public interface ITraceEntity<UPK extends Serializable, E extends ITraceEntity<UPK, E>>
        extends IEntity<E> {

    // 允许数据库表只包含其中1个审计字段，所以 get set 方法均添加默认实现，避免实现类报错

    /**
     * 获取创建用户的 id
     *
     * @return create user id
     */
    default UPK getCreateUserId() {
        return null;
    }

    /**
     * 设置创建用户的 id
     *
     * @param createUserId create user id
     */
    default void setCreateUserId(UPK createUserId) {

    }

    /**
     * @return 创建用户的用户名
     */
    default String getCreateUserName() {
        return null;
    }

    /**
     * 设置创建用户的用户名
     *
     * @param createUserName create user name
     */
    default void setCreateUserName(String createUserName) {

    }

    /**
     * 获取创建记录的时间
     *
     * @return create time
     */
    default Long getCreateTime() {
        return null;
    }

    /**
     * 设置创建记录的时间
     *
     * @param createTime create time
     */
    default void setCreateTime(Long createTime) {

    }

    /**
     * 获取修改用户的 id
     *
     * @return modify user id
     */
    default UPK getModifyUserId() {
        return null;
    }

    /**
     * 设置修改用户的 id
     *
     * @param modifyUserId modify user id
     */
    default void setModifyUserId(UPK modifyUserId) {

    }

    /**
     * 获取修改用户的用户名
     *
     * @return modify user name
     */
    default String getModifyUserName() {
        return null;
    }

    /**
     * 设置修改用户的用户名
     *
     * @param modifyUserName modify user name
     */
    default void setModifyUserName(String modifyUserName) {

    }

    /**
     * 获取修改记录的时间
     *
     * @return modify time
     */
    default Long getModifyTime() {
        return null;
    }

    /**
     * 设置修改记录的时间
     *
     * @param modifyTime modify time
     */
    default void setModifyTime(Long modifyTime) {

    }

    @Override
    @SuppressWarnings("unchecked")
    default E randomEntity() {
        setCreateUserId((UPK) LongSequence.get());
        setCreateUserName(randomString());
        setCreateTime(randomLong());
        setModifyUserId((UPK) LongSequence.get());
        setModifyUserName(randomString());
        setModifyTime(randomLong());
        return (E) this;
    }

}
