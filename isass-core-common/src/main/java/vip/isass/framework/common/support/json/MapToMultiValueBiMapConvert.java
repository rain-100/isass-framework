// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.map.MultiValueBiMap;

import java.util.Map;

/**
 * 一键多值集合的序列化器
 */
public class MapToMultiValueBiMapConvert extends StdConverter<Map<Object, Object>, MultiValueBiMap<Object, Object>> {

    @Override
    public MultiValueBiMap<Object, Object> convert(Map<Object, Object> value) {
        if (value == null) {
            return null;
        }

        MultiValueBiMap<Object, Object> multiValueBiMap = new MultiValueBiMap<>();
        for (Map.Entry<Object, Object> entry : value.entrySet()) {
            multiValueBiMap.putAll(entry.getKey(), (Iterable<?>) entry.getValue());
        }
        return multiValueBiMap;
    }
}
