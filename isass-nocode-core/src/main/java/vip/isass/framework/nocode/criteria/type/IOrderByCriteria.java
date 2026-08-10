// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.type;

import cn.hutool.core.util.StrUtil;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.property.PropertyGetter;
import vip.isass.framework.nocode.property.PropertyNameResolver;

/**
 * order by 排序条件接口
 *
 * @author Rain
 */
public interface IOrderByCriteria<E extends IEntity<E>, C extends IOrderByCriteria<E, C>>
    extends ICriteria<E, C> {

    String ASC = " asc";

    String DESC = " desc";

    String getOrderBy();

    C setOrderBy(String orderBy);

    @SuppressWarnings("unchecked")
    default C orderByIfBlank(String column, String direction) {
        if (StrUtil.isBlank(getOrderBy())) {
            return orderBy(column, direction);
        }
        return (C) this;
    }

    default C orderBy(String column, String direction) {
        return setOrderBy(column + " " + direction);
    }

    default C orderBy(PropertyGetter<E, ?> propertyGetter, String direction) {
        return orderBy(PropertyNameResolver.resolve(propertyGetter), direction);
    }

    default C orderByIfBlank(PropertyGetter<E, ?> propertyGetter, String direction) {
        return orderByIfBlank(PropertyNameResolver.resolve(propertyGetter), direction);
    }

}
