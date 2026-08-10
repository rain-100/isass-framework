// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.beans.Transient;
import java.io.Serializable;

/**
 * 租户实体
 *
 * @author Rain
 */
public interface ITenantEntity<TPK extends Serializable, E extends ITenantEntity<TPK, E>> extends IEntity<E> {

    /**
     * 获取 租户 id
     *
     * @return 租户 id
     */
    TPK getTenantId();

    /**
     * 设置租户 id
     *
     * @param tenantId 租户 id
     */
    void setTenantId(TPK tenantId);

    @Override
    @SuppressWarnings("unchecked")
    default E randomEntity() {
        // todo 如何获取到TPK类型
        // setTenantId(randomPk());
        return (E) this;
    }

}
