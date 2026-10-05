// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.nocode.query;

import org.springframework.core.ResolvableType;
import tools.jackson.databind.JsonNode;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.criteria.CriteriaEntityTypes;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.entrypoint.QueryParamConverter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 整个 Criteria 的 QUERY 表示，普通 JSON 不做字符串化。 */
public final class CriteriaQueryParamConverter implements QueryParamConverter {
    private static final Set<String> COMPLEX = Set.of("whereConditions", "joinConditions", "loadRelated", "fromCriteria");
    @Override
    public boolean supports(Type type) {
        Class<?> raw = ResolvableType.forType(type).resolve();
        return raw != null && ICriteria.class.isAssignableFrom(raw);
    }

    @Override
    public Map<String, String> toQueryParams(Object value, Type type) {
        Map<String, String> result = new LinkedHashMap<>();
        for (var property : JsonUtil.valueToTree(value).properties()) {
            String name = property.getKey();
            JsonNode node = property.getValue();
            if (node.isNull() || (node.isObject() || node.isArray()) && node.isEmpty()) continue;
            rejectRemoved(name);
            if (COMPLEX.contains(name)) {
                result.put(name, JsonUtil.writeValue(node));
            } else if (node.isArray()) {
                ArrayList<String> values = new ArrayList<>();
                boolean complex = false;
                for (JsonNode item : node) {
                    if (!item.isValueNode()) { complex = true; break; }
                    values.add(item.asString());
                }
                result.put(name, complex ? JsonUtil.writeValue(node) : String.join(",", values));
            } else if (node.isValueNode()) {
                result.put(name, node.asString());
            } else {
                // 自定义 Criteria 中的其他复杂对象也采用一个 JSON Query 值。
                result.put(name, JsonUtil.writeValue(node));
            }
        }
        return result;
    }

    @Override
    public Object fromQueryParams(Map<String, String> params, Type type) {
        Class<?> declared = ResolvableType.forType(type).resolve();
        Class<?> raw = declared != null && declared.isInterface() && params.containsKey("entityType")
                ? CriteriaEntityTypes.newCriteria(CriteriaEntityTypes.requireEntity(params.get("entityType"))).getClass()
                : declared;
        Map<String, Object> values = new LinkedHashMap<>();
        params.forEach((name, text) -> {
            rejectRemoved(name);
            if (COMPLEX.contains(name)) {
                values.put(name, JsonUtil.readTree(text));
            } else if (name.equals("entityType")) {
                values.put(name, text);
            } else {
                Method setter = setter(raw, name);
                if (setter == null) return; // 其他声明 QUERY 参数，例如 cursorId，由 HTTP 自己绑定。
                ResolvableType resolved = ResolvableType.forMethodParameter(setter, 0, raw);
                Class<?> propertyType = resolved.resolve(Object.class);
                if (propertyType.isArray() || Iterable.class.isAssignableFrom(propertyType)) {
                    Class<?> itemType = propertyType.isArray() ? propertyType.getComponentType() : resolved.getGeneric(0).resolve(Object.class);
                    values.put(name, isScalar(itemType) ? Arrays.asList(text.split(",", -1)) : JsonUtil.readTree(text));
                } else if (Map.class.isAssignableFrom(propertyType) || !isScalar(propertyType)) {
                    values.put(name, JsonUtil.readTree(text));
                } else {
                    values.put(name, text);
                }
            }
        });
        return JsonUtil.convertValue(values, type);
    }

    private static void rejectRemoved(String name) {
        if (name.startsWith("association.") || name.equals("associationQueries") || name.equals("associationCriteria")) {
            throw new IllegalArgumentException("旧关联查询参数已删除，请使用 loadRelated: " + name);
        }
        if (Set.of("sourceCriteria", "targetType", "relatedQueryVersion").contains(name)) {
            throw new IllegalArgumentException("已删除的 Criteria 字段: " + name);
        }
    }

    private static boolean isScalar(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || type == String.class || Number.class.isAssignableFrom(type)
                || type == Boolean.class || type == Character.class || type.getPackageName().equals("java.time");
    }

    private static Method setter(Class<?> type, String property) {
        if (property.isEmpty()) return null;
        String name = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        return Arrays.stream(type.getMethods()).filter(method -> method.getName().equals(name)
                && method.getParameterCount() == 1).findFirst().orElse(null);
    }
}
