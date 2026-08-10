// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.impl.type;

import lombok.Getter;
import lombok.ToString;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.entity.IEntity;

/**
 * 排序条件
 */
@ToString
public class OrderByCriteria<E extends IEntity<E>, C extends OrderByCriteria<E, C>>
    implements IOrderByCriteria<E, C> {

    @Getter
    private String orderBy;

    @Override
    @SuppressWarnings("unchecked")
    public C setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return (C) this;
    }

}
