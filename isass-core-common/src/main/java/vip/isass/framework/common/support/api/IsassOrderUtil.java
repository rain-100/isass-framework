package vip.isass.framework.common.support.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Resolves ordering while keeping core modules free of Spring dependencies.
 *
 * @author isass
 */
public final class IsassOrderUtil {

    private static final String SPRING_ORDER_ANNOTATION = "org.springframework.core.annotation.Order";

    private IsassOrderUtil() {
    }

    public static int getOrder(Object source) {
        if (source == null) {
            return IsassOrdered.LOWEST_PRECEDENCE;
        }

        if (source instanceof IsassOrdered ordered) {
            return ordered.getOrder();
        }

        Class<?> sourceClass = source instanceof Class<?> clazz ? clazz : source.getClass();
        IsassOrder isassOrder = sourceClass.getAnnotation(IsassOrder.class);
        if (isassOrder != null) {
            return isassOrder.value();
        }

        Integer springOrder = findSpringOrder(sourceClass);
        return springOrder == null ? IsassOrdered.LOWEST_PRECEDENCE : springOrder;
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
