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
public interface IParentIdEntity<PK extends Serializable, E extends IParentIdEntity<PK, E>>
    extends IPkEntity<PK, E> {

    String TOP_ID_STRING_VALUE = "0";

    Integer TOP_ID_INTEGER_VALUE = 0;

    Long TOP_ID_LONG_VALUE = 0L;

    /**
     * @return 父 id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    PK getParentId();

    /**
     * 设置父 id
     *
     * @param parentId parent id
     */
    void setParentId(PK parentId);

    /**
     * 标记为顶级实体
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    default E markAsTopEntity() {
        Class<PK> pkClass = findPkClass();
        if (pkClass == String.class) {
            setParentId((PK) TOP_ID_STRING_VALUE);
        } else if (pkClass == Long.class) {
            setParentId((PK) TOP_ID_LONG_VALUE);
        } else if (pkClass == Integer.class) {
            setParentId((PK) TOP_ID_INTEGER_VALUE);
        } else {
            throw new UnsupportedOperationException(StrUtil.format(
                "未支持自动生成类型为[{}]的 parent_id", pkClass
            ));
        }
        return (E) this;
    }

    @SuppressWarnings("unchecked")
    default E randomParentId() {
        setParentId(randomPk());
        return (E) this;
    }

    @Override
    default E randomEntity() {
        return randomParentId();
    }

}
