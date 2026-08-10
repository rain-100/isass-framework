// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.map.MultiKeyMultiValueBiMap;

import java.util.Map;

/**
 * 一键多值集合的序列化器
 */
public class MapToMultiKeyMultiValueBiMapConvert extends StdConverter<Map<Object, Object>, MultiKeyMultiValueBiMap<Object, Object>> {

    @Override
    public MultiKeyMultiValueBiMap<Object, Object> convert(Map<Object, Object> value) {
        if (value == null) {
            return null;
        }

        MultiKeyMultiValueBiMap<Object, Object> multiValueBiMap = new MultiKeyMultiValueBiMap<>();
        for (Map.Entry<Object, Object> entry : value.entrySet()) {
            multiValueBiMap.putAll(entry.getKey(), (Iterable<?>) entry.getValue());
        }
        return multiValueBiMap;
    }
}
