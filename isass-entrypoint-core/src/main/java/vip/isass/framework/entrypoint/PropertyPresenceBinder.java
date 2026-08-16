// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint;

import java.beans.Introspector;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies protocol-decoder field presence to converted request objects. */
public final class PropertyPresenceBinder {

    private PropertyPresenceBinder() {
    }

    public static void bind(Object value, Object decodedTree) {
        if (value == null || decodedTree == null) return;
        if (value instanceof Collection<?> values && decodedTree instanceof List<?> nodes) {
            int index = 0;
            for (Object item : values) {
                if (index >= nodes.size()) break;
                bind(item, nodes.get(index++));
            }
            return;
        }
        if (value.getClass().isArray() && decodedTree instanceof List<?> nodes) {
            for (int index = 0; index < Math.min(Array.getLength(value), nodes.size()); index++) {
                bind(Array.get(value, index), nodes.get(index));
            }
            return;
        }
        if (value instanceof Map<?, ?> values && decodedTree instanceof Map<?, ?> nodes) {
            nodes.forEach((key, node) -> bind(values.get(key), node));
            return;
        }
        if (!(decodedTree instanceof Map<?, ?> nodes)) return;
        if (value instanceof PropertyPresenceAware aware) {
            nodes.keySet().stream().map(String::valueOf).forEach(aware::markPresentProperty);
        }
        for (var entry : nodes.entrySet()) {
            bind(readProperty(value, String.valueOf(entry.getKey())), entry.getValue());
        }
    }

    /** Removes fields known to be absent before a request crosses a transport boundary. */
    public static Object project(Object value, Object encodedTree) {
        if (value == null || encodedTree == null) return encodedTree;
        if (value instanceof Collection<?> values && encodedTree instanceof List<?> nodes) {
            List<Object> projected = new ArrayList<>(nodes.size());
            int index = 0;
            for (Object item : values) {
                if (index >= nodes.size()) break;
                projected.add(project(item, nodes.get(index++)));
            }
            return projected;
        }
        if (value.getClass().isArray() && encodedTree instanceof List<?> nodes) {
            List<Object> projected = new ArrayList<>(nodes.size());
            for (int index = 0; index < Math.min(Array.getLength(value), nodes.size()); index++) {
                projected.add(project(Array.get(value, index), nodes.get(index)));
            }
            return projected;
        }
        if (value instanceof Map<?, ?> values && encodedTree instanceof Map<?, ?> nodes) {
            Map<Object, Object> projected = new LinkedHashMap<>();
            nodes.forEach((key, node) -> projected.put(key, project(values.get(key), node)));
            return projected;
        }
        if (!(encodedTree instanceof Map<?, ?> nodes)) return encodedTree;
        java.util.Set<String> presence = value instanceof PropertyPresenceAware aware
                && !aware.presentProperties().isEmpty() ? aware.presentProperties() : null;
        Map<Object, Object> projected = new LinkedHashMap<>();
        nodes.forEach((key, node) -> {
            String property = String.valueOf(key);
            if (presence != null && !presence.contains(property)) return;
            Object propertyValue = readProperty(value, property);
            projected.put(key, project(propertyValue, node));
        });
        return projected;
    }

    private static Object readProperty(Object value, String property) {
        var descriptor = descriptors(value).get(property);
        if (descriptor != null) return read(value, descriptor);
        if (!value.getClass().isRecord()) return null;
        for (var component : value.getClass().getRecordComponents()) {
            if (!component.getName().equals(property)) continue;
            try {
                var accessor = component.getAccessor();
                if (!accessor.canAccess(value)) accessor.setAccessible(true);
                return accessor.invoke(value);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("无法读取 record 属性: " + property, exception);
            }
        }
        return null;
    }

    private static Map<String, java.beans.PropertyDescriptor> descriptors(Object value) {
        try {
            return java.util.Arrays.stream(Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors())
                    .collect(java.util.stream.Collectors.toMap(
                            java.beans.PropertyDescriptor::getName, descriptor -> descriptor));
        } catch (java.beans.IntrospectionException exception) {
            throw new IllegalStateException("无法分析属性: " + value.getClass().getName(), exception);
        }
    }

    private static Object read(Object value, java.beans.PropertyDescriptor descriptor) {
        if (descriptor.getReadMethod() == null) return null;
        try {
            var reader = descriptor.getReadMethod();
            if (!reader.canAccess(value)) reader.setAccessible(true);
            return reader.invoke(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法读取属性: " + descriptor.getName(), exception);
        }
    }
}
