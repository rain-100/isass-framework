// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.support.JsonUtil;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves developer-declared association constraints using Java property types.
 */
final class AssociationSupport {

    private static final ClassValue<Map<String, PropertyDescriptor>> PROPERTIES = new ClassValue<>() {
        @Override
        protected Map<String, PropertyDescriptor> computeValue(Class<?> type) {
            try {
                Map<String, PropertyDescriptor> result = new LinkedHashMap<>();
                for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                    result.put(descriptor.getName(), descriptor);
                }
                return Map.copyOf(result);
            } catch (IntrospectionException exception) {
                throw new IllegalStateException("无法分析关联属性: " + type.getName(), exception);
            }
        }
    };

    private AssociationSupport() {
    }

    static Map<String, Object> fixedValues(EntityAssociation association) {
        Map<String, Object> result = new LinkedHashMap<>();
        association.fixedValues().forEach((field, value) -> result.put(field,
                JsonUtil.convertValue(value, descriptor(association.targetType(), field).getPropertyType())));
        return result;
    }

    static void constrain(IEntity<?> target, EntityAssociation association, Object sourceKey) {
        Map<String, Object> required = new LinkedHashMap<>(fixedValues(association));
        required.put(association.targetField(), convert(target.getClass(), association.targetField(), sourceKey));
        required.forEach((field, value) -> {
            Object submitted = read(target, field);
            if (submitted != null && !Objects.equals(submitted, value)) {
                throw new IllegalArgumentException("关联对象不能改变归属或固定条件: " + field);
            }
            write(target, field, value);
        });
    }

    static boolean matches(Object target, EntityAssociation association, Object sourceKey) {
        if (!Objects.equals(read(target, association.targetField()),
                convert(target.getClass(), association.targetField(), sourceKey))) {
            return false;
        }
        return fixedValues(association).entrySet().stream()
                .allMatch(entry -> Objects.equals(read(target, entry.getKey()), entry.getValue()));
    }

    static Object read(Object bean, String property) {
        try {
            var method = descriptor(bean.getClass(), property).getReadMethod();
            if (method == null) {
                throw new IllegalArgumentException("属性不可读: " + property);
            }
            if (!method.canAccess(bean)) {
                method.setAccessible(true);
            }
            return method.invoke(bean);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取关联属性失败: " + property, exception);
        }
    }

    static void write(Object bean, String property, Object value) {
        try {
            var method = descriptor(bean.getClass(), property).getWriteMethod();
            if (method == null) {
                throw new IllegalArgumentException("属性不可写: " + property);
            }
            if (!method.canAccess(bean)) {
                method.setAccessible(true);
            }
            method.invoke(bean, convert(bean.getClass(), property, value));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("写入关联属性失败: " + property, exception);
        }
    }

    private static Object convert(Class<?> type, String property, Object value) {
        return value == null ? null : JsonUtil.convertValue(value, descriptor(type, property).getPropertyType());
    }

    private static PropertyDescriptor descriptor(Class<?> type, String property) {
        PropertyDescriptor descriptor = PROPERTIES.get(type).get(property);
        if (descriptor == null) {
            throw new IllegalArgumentException("未知关联属性: " + type.getSimpleName() + "." + property);
        }
        return descriptor;
    }
}
