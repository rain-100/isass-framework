// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import cn.hutool.core.util.StrUtil;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.beans.Transient;
import java.io.Serializable;

/**
 * @author Rain
 */
public interface IIdEntity<PK extends Serializable, E extends IIdEntity<PK, E>>
    extends IPkEntity<PK, E> {

    /**
     * @return id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    PK getId();

    /**
     * 设置 id
     *
     * @param id id
     */
    void setId(PK id);

    /**
     * 生成一个随机 id
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    default E randomId() {
        setId(randomPk());
        return (E) this;
    }

    /**
     * 如果 id 为 null, 则生成一个随机 id，并返回 id
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    default E randomIdIfAbsent() {
        if (StrUtil.isEmptyIfStr(getId())) {
            randomId();
        }
        return (E) this;
    }

    @Override
    default E randomEntity() {
        return randomId();
    }

}
