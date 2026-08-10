// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.field;

import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主键类型条件接口
 *
 * @author Rain
 */
public interface IPkCriteria<PK extends Serializable, E extends IEntity<E>, C extends IPkCriteria<PK, E, C>>
    extends ICriteria<E, C> {

    Map<Class<?>, Class<?>> PK_CLASS_CACHE = new ConcurrentHashMap<>(64);

    @SuppressWarnings("unchecked")
    default Class<PK> findPkClass() {
        Class<?> thisClass = this.getClass();
        return (Class<PK>) PK_CLASS_CACHE.computeIfAbsent(thisClass, c -> {
            Type[] types = thisClass.getGenericInterfaces();
            String typeName = types[0].getTypeName();
            System.out.println(typeName);
            ParameterizedType parameterizedType = (ParameterizedType) types[0];
            Type type = parameterizedType.getActualTypeArguments()[0];
            return (Class<?>) type;
        });
    }

}
