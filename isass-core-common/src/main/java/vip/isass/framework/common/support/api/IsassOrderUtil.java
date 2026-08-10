// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Resolves ordering while keeping core modules free of Spring dependencies.
 *
 * @author isass
 */
public final class IsassOrderUtil {

    public static final int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    public static final int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    private static final String SPRING_ORDER_ANNOTATION = "org.springframework.core.annotation.Order";

    private IsassOrderUtil() {
    }

    public static int getOrder(Object source) {
        if (source == null) {
            return LOWEST_PRECEDENCE;
        }

        Class<?> sourceClass = source instanceof Class<?> clazz ? clazz : source.getClass();
        Integer methodOrder = findGetOrder(source, sourceClass);
        if (methodOrder != null) {
            return methodOrder;
        }

        IsassOrder isassOrder = sourceClass.getAnnotation(IsassOrder.class);
        if (isassOrder != null) {
            return isassOrder.value();
        }

        Integer springOrder = findSpringOrder(sourceClass);
        return springOrder == null ? LOWEST_PRECEDENCE : springOrder;
    }

    private static Integer findGetOrder(Object source, Class<?> sourceClass) {
        if (source instanceof Class<?>) {
            return null;
        }
        try {
            Method getOrderMethod = sourceClass.getMethod("getOrder");
            Object value = getOrderMethod.invoke(source);
            return value instanceof Integer integer ? integer : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Integer findSpringOrder(Class<?> sourceClass) {
        for (Annotation annotation : sourceClass.getAnnotations()) {
            if (!SPRING_ORDER_ANNOTATION.equals(annotation.annotationType().getName())) {
                continue;
            }
            try {
                Method valueMethod = annotation.annotationType().getMethod("value");
                Object value = valueMethod.invoke(annotation);
                return value instanceof Integer integer ? integer : null;
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return null;
    }
}
