// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria;

import cn.hutool.core.bean.BeanUtil;
import vip.isass.framework.nocode.entity.IEntity;

/**
 * 基于mysql的条件
 *
 * @author Rain
 */
public interface ICriteria<E extends IEntity<E>, C extends ICriteria<E, C>> {

    @SuppressWarnings("unchecked")
    default C copy() {
        try {
            var constructor = getClass().getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            C target = (C) constructor.newInstance();
            BeanUtil.copyProperties(this, target);
            return target;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Criteria 必须提供无参构造器: " + getClass().getName(), exception);
        }
    }

}
