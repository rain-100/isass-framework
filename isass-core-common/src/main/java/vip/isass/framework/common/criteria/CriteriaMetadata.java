// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按 Class 生命周期缓存静态反射信息，不保存 Criteria 实例、条件值或 ORM 执行状态。
 * 关联定义来自实体生成的 associations()，必须与实体实例状态无关。
 */
public final class CriteriaMetadata {
    private CriteriaMetadata() { }

    private static final ClassValue<Constructor<?>> CONSTRUCTORS = new ClassValue<>() {
        @Override
        protected Constructor<?> computeValue(Class<?> type) {
            try {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException exception) {
                throw new IllegalArgumentException("类型需要无参构造器: " + type.getName(), exception);
            }
        }
    };

    private static final ClassValue<List<Field>> FIELDS = new ClassValue<>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> current = type; current != Object.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            return List.copyOf(fields);
        }
    };

    private static final ClassValue<Map<String, Field>> FIELDS_BY_NAME = new ClassValue<>() {
        @Override
        protected Map<String, Field> computeValue(Class<?> type) {
            Map<String, Field> fields = new LinkedHashMap<>();
            for (Field field : FIELDS.get(type)) fields.putIfAbsent(field.getName(), field);
            return Map.copyOf(fields);
        }
    };

    /** JSON 绑定由当前反序列化上下文解析 JavaType，不缓存 mapper 配置。 */
    record Binding(Field field, Method setter, Type type) {
        void set(Object target, Object value) throws ReflectiveOperationException {
            if (field != null) field.set(target, value);
            else setter.invoke(target, value);
        }
    }

    private static final ClassValue<Map<String, Binding>> BINDINGS = new ClassValue<>() {
        @Override
        protected Map<String, Binding> computeValue(Class<?> type) {
            Map<String, Binding> bindings = new LinkedHashMap<>();
            for (Field field : FIELDS.get(type)) {
                if (!Modifier.isFinal(field.getModifiers())) {
                    bindings.putIfAbsent(field.getName(), new Binding(field, null,
                            GenericTypeResolver.resolveType(field.getGenericType(), type)));
                }
            }
            for (Method method : type.getMethods()) {
                String name = method.getName();
                if (!name.startsWith("set") || name.length() <= 3 || method.getParameterCount() != 1
                        || Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()) continue;
                String property = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                if (!bindings.containsKey(property)) {
                    method.setAccessible(true);
                    bindings.put(property, new Binding(null, method,
                            GenericTypeResolver.resolveType(method.getGenericParameterTypes()[0], type)));
                }
            }
            return Map.copyOf(bindings);
        }
    };

    private static final ClassValue<Map<String, Class<?>>> VALUE_TYPES = new ClassValue<>() {
        @Override
        protected Map<String, Class<?>> computeValue(Class<?> type) {
            Map<String, Class<?>> values = new LinkedHashMap<>();
            // getX 优先于 isX，与反序列化既有行为一致。
            for (String prefix : List.of("get", "is")) {
                for (Method method : type.getMethods()) {
                    String name = method.getName();
                    if (!name.startsWith(prefix) || name.length() <= prefix.length()
                            || method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())
                            || method.isBridge() || method.isSynthetic()) continue;
                    Class<?> value = ResolvableType.forMethodReturnType(method, type).resolve();
                    if (value != null) {
                        String property = Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1);
                        values.putIfAbsent(property, value);
                    }
                }
            }
            return Map.copyOf(values);
        }
    };

    private static final ClassValue<Map<String, Class<?>>> ELEMENT_TYPES = new ClassValue<>() {
        @Override
        protected Map<String, Class<?>> computeValue(Class<?> type) {
            Map<String, Class<?>> elements = new LinkedHashMap<>();
            FIELDS_BY_NAME.get(type).forEach((name, field) -> {
                if (Collection.class.isAssignableFrom(field.getType())) {
                    Class<?> element = ResolvableType.forField(field, type).as(Collection.class).getGeneric(0).resolve();
                    if (element != null) elements.put(name, element);
                }
            });
            return Map.copyOf(elements);
        }
    };

    private static final ClassValue<List<EntityAssociation>> ASSOCIATIONS = new ClassValue<>() {
        @Override
        protected List<EntityAssociation> computeValue(Class<?> type) {
            try {
                return List.copyOf(((IEntity<?>) constructor(type).newInstance()).associations());
            } catch (ReflectiveOperationException exception) {
                throw new IllegalArgumentException("无法读取实体关联元数据: " + type.getName(), exception);
            }
        }
    };

    public static Constructor<?> constructor(Class<?> type) { return CONSTRUCTORS.get(type); }
    static List<Field> fields(Class<?> type) { return FIELDS.get(type); }
    public static Field field(Class<?> type, String property) { return FIELDS_BY_NAME.get(type).get(property); }
    static Binding binding(Class<?> type, String property) { return BINDINGS.get(type).get(property); }
    static Class<?> valueType(Class<?> type, String property) { return VALUE_TYPES.get(type).get(property); }
    public static Class<?> elementType(Class<?> type, String property) { return ELEMENT_TYPES.get(type).get(property); }
    public static List<EntityAssociation> associations(Class<?> type) { return ASSOCIATIONS.get(type); }
}
