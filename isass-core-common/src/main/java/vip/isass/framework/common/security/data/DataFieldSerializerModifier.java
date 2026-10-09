// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.security.data;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import java.util.List;

/**
 * 按返回记录的可见性元数据省略 JSON 属性，而非只将字段值替换为 null。
 */
public final class DataFieldSerializerModifier extends ValueSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
            BeanDescription.Supplier bean, List<BeanPropertyWriter> properties) {
        return properties.stream().map(ProtectedWriter::new).map(BeanPropertyWriter.class::cast).toList();
    }

    private static final class ProtectedWriter extends BeanPropertyWriter {
        private ProtectedWriter(BeanPropertyWriter source) {
            super(source);
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context)
                throws Exception {
            if (!HiddenDataFields.contains(bean, getName())) {
                super.serializeAsProperty(bean, generator, context);
            }
        }
    }
}
