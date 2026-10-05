// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import tools.jackson.databind.JsonNode;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 复制执行用条件树，不经过业务 setter，避免重复追加条件或共享可变请求状态。 */
public final class CriteriaCopies {
    private CriteriaCopies() { }

    @SuppressWarnings("unchecked")
    public static <T> T copy(T source) {
        return (T) copyValue(source, new IdentityHashMap<>(), new IdentityHashMap<>());
    }

    private static Object copyValue(Object source, Map<Object, Object> copies, Map<Object, Boolean> visiting) {
        if (source == null || source instanceof EmptyCriteria<?>) return source;
        if (visiting.containsKey(source)) {
            throw new IllegalArgumentException("Criteria 条件树不能包含循环引用");
        }
        if (copies.containsKey(source)) return copies.get(source);
        visiting.put(source, Boolean.TRUE);
        try {
            Object result;
            if (source instanceof ICriteria<?, ?> || source instanceof BaseCondition<?> || source instanceof RelatedCondition) {
                result = CriteriaMetadata.constructor(source.getClass()).newInstance();
                for (Field field : CriteriaMetadata.fields(source.getClass())) {
                    field.set(result, copyValue(field.get(source), copies, visiting));
                }
            } else if (source instanceof Map<?, ?> map) {
                Map<Object, Object> target = new LinkedHashMap<>();
                for (var entry : map.entrySet()) {
                    target.put(copyValue(entry.getKey(), copies, visiting), copyValue(entry.getValue(), copies, visiting));
                }
                result = target;
            } else if (source instanceof Collection<?> collection) {
                Collection<Object> target = source instanceof Set<?> ? new LinkedHashSet<>() : new ArrayList<>();
                for (Object value : collection) target.add(copyValue(value, copies, visiting));
                result = target;
            } else if (source.getClass().isArray()) {
                result = Array.newInstance(source.getClass().getComponentType(), Array.getLength(source));
                for (int i = 0; i < Array.getLength(source); i++) {
                    Array.set(result, i, copyValue(Array.get(source, i), copies, visiting));
                }
            } else if (source instanceof Date date) {
                result = date.clone();
            } else if (source instanceof JsonNode node) {
                result = node.deepCopy();
            } else {
                // 字符串、数字、枚举、java.time 等标量按值使用。
                result = source;
            }
            copies.put(source, result);
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法复制条件对象（需无参构造器）: " + source.getClass().getName(), exception);
        } finally {
            visiting.remove(source);
        }
    }
}
