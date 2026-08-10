// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import vip.isass.framework.common.sequence.impl.LongSequence;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 含有主键类型的接口
 * 主键类型泛型的定义始终放在第一位
 *
 * @author Rain
 */
public interface IPkEntity<PK extends Serializable, E extends IPkEntity<PK, E>> extends IEntity<E> {

    Map<Class<?>, Class<?>> PK_CLASS_CACHE = new ConcurrentHashMap<>(64);

    @SuppressWarnings("unchecked")
    default Class<PK> findPkClass() {
        return (Class<PK>) PK_CLASS_CACHE.computeIfAbsent(this.getClass(), c -> {
            // 如果 thisClass 是继承了 IEntity的类，即不是代码自动生成的实体类，则他本身没有定义泛型，需要往其父类上找到泛型
            Type[] types = getTypes(c);
            ParameterizedType parameterizedType = (ParameterizedType) types[0];
            Type type = parameterizedType.getActualTypeArguments()[0];
            return (Class<?>) type;
        });
    }

    static Type[] getTypes(Class<?> thisClass) {
        if (thisClass == null) {
            return new Type[0];
        }
        Type[] types = thisClass.getGenericInterfaces();
        if (types.length > 0) {
            return types;
        }
        return getTypes(thisClass.getSuperclass());
    }

    //
    //    @SuppressWarnings("unchecked")
    //    default Class<PK> findPkClass() {
    //        Type[] types = this.getClass().getGenericInterfaces();
    //        String typeName = types[0].getTypeName();
    //        System.out.println(typeName);
    //        ParameterizedType parameterizedType = (ParameterizedType) types[0];
    //        Type type = parameterizedType.getActualTypeArguments()[0];
    //        return (Class<PK>) type;
    //    }

    @SuppressWarnings("unchecked")
    default PK randomPk() {
        Class<PK> pkClass = findPkClass();
        if (pkClass == String.class) {
            return (PK) LongSequence.get().toString();
        } else if (pkClass == Long.class) {
            return (PK) LongSequence.get();
        } else if (pkClass == Integer.class) {
            return (PK) Integer.valueOf(RandomUtil.randomInt());
        } else {
            throw new UnsupportedOperationException(StrUtil.format(
                    "未支持自动生成类型为[{}]的主键", pkClass
            ));
        }
    }

}
