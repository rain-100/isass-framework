// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;


import cn.hutool.core.util.ReflectUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Rain
 */
public class ReflectUtils {

    /**
     * 获取实现类中实现了 含有注解 @ApiService 类的方法
     *
     * @param implementClass  implement class
     * @param annotationClass annotation class
     * @return implement api service methods
     */
    public static List<Method> getImplementApiServiceMethods(Class<?> implementClass, Class<? extends Annotation> annotationClass) {
        // 指定类的 public 方法
        Method[] implementClassMethods = ReflectUtil.getPublicMethods(implementClass);

        // 去掉 default 方法
        List<Method> implementMethods = Stream.of(implementClassMethods)
            .filter(m -> !m.isDefault())
            .collect(Collectors.toList());

        // 找到含有指定注解的接口
        List<Class<?>> apiServiceClass = Stream
            .of(implementClass.getInterfaces())
            .filter(c -> c.isAnnotationPresent(annotationClass))
            .collect(Collectors.toList());

        // ApiService 类的所有方法
        return implementMethods.stream()
            .filter(m -> {
                for (Class<?> serviceClass : apiServiceClass) {
                    Method interfaceMethod = findInterfaceMethod(m, serviceClass);
                    return interfaceMethod != null && m != interfaceMethod;
                }
                return false;
            })
            .collect(Collectors.toList());
    }

    private static Method findInterfaceMethod(Method method, Class<?> serviceClass) {
        try {
            return serviceClass.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

}
