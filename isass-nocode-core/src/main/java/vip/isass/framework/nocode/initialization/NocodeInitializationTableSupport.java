// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.BeanUtils;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.entrypoint.annotation.ApiDoc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Shared field metadata and lossless cell values for Excel and CSV initialization. */
final class NocodeInitializationTableSupport {

    private static final Set<String> AUDIT_FIELDS = Set.of(
            "createUserId", "createUserName", "createTime", "modifyUserId", "modifyUserName", "modifyTime");

    private NocodeInitializationTableSupport() {
    }

    static Map<String, Field> fields(Class<?> entityType) {
        if (entityType == null) {
            return Map.of();
        }
        var entity = (IEntity<?>) JsonUtil.convertValue(Map.of(), entityType);
        Set<String> associations = entity.associations().stream()
                .map(EntityAssociation::property).collect(Collectors.toSet());
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Class<?> type = entityType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                JsonIgnore ignored = field.getAnnotation(JsonIgnore.class);
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())
                        && !field.isSynthetic() && !associations.contains(field.getName())
                        && (ignored == null || !ignored.value())) {
                    fields.putIfAbsent(field.getName(), field);
                }
            }
        }
        return fields;
    }

    static List<Field> templateFields(Class<?> entityType) {
        List<Field> fields = fields(entityType).values().stream()
                .filter(field -> !AUDIT_FIELDS.contains(field.getName())).toList();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("初始化实体没有可填写字段: " + entityType.getSimpleName());
        }
        return fields;
    }

    static boolean isJsonType(Class<?> type) {
        return type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)
                || !BeanUtils.isSimpleValueType(type);
    }

    static Object readText(String text, Field field) {
        Class<?> type = field == null ? null : field.getType();
        boolean prefixed = text.startsWith("json:");
        String json = prefixed ? text.substring(5) : text;
        if (text.equals("json:null")) {
            if (type != null && type.isPrimitive()) {
                throw new IllegalArgumentException("基本类型字段不能填写 null");
            }
            return null;
        }
        // Explicit JSON string literals also allow reserved markers and formula-like text to round-trip.
        if (prefixed && json.startsWith("\"")) {
            return JsonUtil.readValue(json, String.class);
        }
        if ((type != null && isJsonType(type)) || (type == null && prefixed)) {
            try {
                return JsonUtil.readValue(json, Object.class);
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("字段必须填写有效 JSON: " + exception.getMessage(), exception);
            }
        }
        if (prefixed && type != null && type != String.class) {
            return JsonUtil.readValue(json, Object.class);
        }
        return text;
    }

    static String description(Field field) {
        ApiDoc doc = field.getAnnotation(ApiDoc.class);
        String description = doc == null || doc.description().isBlank() ? field.getName() : doc.description();
        Class<?> type = field.getType();
        if (type.isEnum()) {
            List<String> options = new ArrayList<>();
            for (Object value : type.getEnumConstants()) {
                options.add(JsonUtil.writeValue(value) + "=" + ((Enum<?>) value).name());
            }
            return description + "（枚举编码：" + String.join("；", options) + "）";
        }
        if (type == Boolean.class || type == boolean.class) {
            return description + "（TRUE/FALSE）";
        }
        if (isJsonType(type)) {
            return description + "（JSON：直接填写数组、对象等 JSON 值，无需 json: 前缀）";
        }
        return description;
    }

    static String writeValue(Object value, Field field) {
        JsonProperty property = field.getAnnotation(JsonProperty.class);
        if (property != null && property.access() == JsonProperty.Access.WRITE_ONLY) {
            return "";
        }
        if (value == null) {
            return "json:null";
        }
        if (isJsonType(field.getType())) {
            return JsonUtil.writeValue(value);
        }
        if (value instanceof String text) {
            if (text.isEmpty() || text.startsWith("json:") || "=+-@\t\r\n".indexOf(text.charAt(0)) >= 0) {
                return "json:" + JsonUtil.writeValue(text);
            }
            return text;
        }
        Object scalar = JsonUtil.readValue(JsonUtil.writeValue(value), Object.class);
        return scalar instanceof String ? writeValue(scalar, field) : JsonUtil.writeValue(scalar);
    }
}
