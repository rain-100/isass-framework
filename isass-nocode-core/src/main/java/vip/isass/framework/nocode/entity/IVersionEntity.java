// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.beans.Transient;

/**
 * 乐观锁版本号
 *
 * @author Rain
 */
public interface IVersionEntity<E extends IVersionEntity<E>> extends IEntity<E> {

    Integer DEFAULT_VERSION = 1;

    /**
     * 获取版本号
     *
     * @return version
     */
    Integer getVersion();

    /**
     * 设置版本号
     *
     * @param version version
     */
    void setVersion(Integer version);

    /**
     * @return 如果版本号为 null, 则设置版本号为1，并返回版本号
     */
    @SuppressWarnings("unchecked")
    default E computeDefaultVersionIfAbsent() {
        if (getVersion() == null) {
            setVersion(DEFAULT_VERSION);
        }
        return (E) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    default E randomEntity() {
        setVersion(randomInteger());
        return (E) this;
    }

}
