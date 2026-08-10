// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import vip.isass.framework.common.map.MultiKeyMultiValueBiMap;

import java.util.Collection;
import java.util.Set;

/**
 * 多键多值的集合序列化器
 */
public class MultiKeyMultiValueBiMapSerializer extends ValueSerializer<MultiKeyMultiValueBiMap> {

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(MultiKeyMultiValueBiMap value, JsonGenerator gen, SerializationContext serializers) {
        Set<?> keySet = value.keySet();
        gen.writeStartObject();
        for (Object key : keySet) {
            Collection<?> collection = value.get(key);
            gen.writeName(key.toString());
            gen.writePOJO(collection);
        }
        gen.writeEndObject();
    }

}
