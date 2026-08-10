// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import java.util.HashMap;
import java.util.Map;

public class MapFlattener {

    public static Map<String, Object> flattenMap(Map<String, Object> map) {
        return flattenMap(map, "", new HashMap<>());
    }

    public static Map<String, Object> flattenMapIfNecessary(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                return flattenMap(map);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> flattenMap(Map<String, Object> currentMap,
                                                 String prefix,
                                                 Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map) {
                flattenMap((Map<String, Object>) value, fullKey, result);
            } else {
                // 如果值不是 Map，直接添加到结果中
                result.put(fullKey, value);
            }
        }
        return result;
    }

    }
