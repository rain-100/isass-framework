package vip.isass.framework.nocode.v3;

import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Criteria 工具：将 HTTP query 参数 Map 转为实体专用的 Criteria 对象。
 * 使用反射调用 Criteria 的链式 setter 方法填充字段值。
 */
public class V3CriteriaMapper {

    /**
     * 将 Map 参数转换为指定类型的 Criteria 实例。
     * Map key 对应 Criteria 的 setter 方法名（如 "name" → setName("foo")）。
     */
    public <C> C toCriteria(Map<String, String> params, Class<C> criteriaClass) {
        C criteria = ReflectUtil.newInstance(criteriaClass);
        if (params == null || params.isEmpty()) {
            return criteria;
        }
        Map<String, Method> setters = setters(criteriaClass);
        params.forEach((name, value) -> {
            if (value == null) {
                return;
            }
            Method setter = setters.get(name);
            if (setter != null) {
                invokeSetter(criteria, setter, value);
            }
        });
        return criteria;
    }

    private static Map<String, Method> setters(Class<?> criteriaClass) {
        Map<String, Method> map = new LinkedHashMap<>();
        for (Method method : criteriaClass.getMethods()) {
            String methodName = method.getName();
            if (method.getParameterCount() == 1
                    && methodName.startsWith("set")
                    && methodName.length() > 3
                    && method.getReturnType().equals(criteriaClass)) {
                String propertyName = Character.toLowerCase(methodName.charAt(3))
                        + methodName.substring(4);
                map.putIfAbsent(propertyName, method);
            }
        }
        return map;
    }

    private static void invokeSetter(Object criteria, Method setter, String value) {
        Class<?> paramType = setter.getParameterTypes()[0];
        try {
            if (paramType == String.class) {
                setter.invoke(criteria, value);
            } else if (paramType == Long.class || paramType == long.class) {
                setter.invoke(criteria, Long.valueOf(value));
            } else if (paramType == Integer.class || paramType == int.class) {
                setter.invoke(criteria, Integer.valueOf(value));
            } else if (paramType == Boolean.class || paramType == boolean.class) {
                setter.invoke(criteria, Boolean.valueOf(value));
            } else {
                // fallback: attempt String, let the setter handle conversion
                setter.invoke(criteria, value);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set " + setter.getName() + " with value '" + value + "'", e);
        }
    }
}
