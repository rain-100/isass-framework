// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.security.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.util.List;

/**
 * 第三方 Jackson 2 调用链也必须遵守记录的字段可见性。
 */
public final class LegacyDataFieldSerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription bean,
                                                    List<BeanPropertyWriter> properties) {
        return properties.stream().map(ProtectedWriter::new).map(BeanPropertyWriter.class::cast).toList();
    }

    private static final class ProtectedWriter extends BeanPropertyWriter {
        private ProtectedWriter(BeanPropertyWriter source) {
            super(source);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator generator, SerializerProvider provider)
                throws Exception {
            if (!HiddenDataFields.contains(bean, getName())) {
                super.serializeAsField(bean, generator, provider);
            }
        }
    }
}
