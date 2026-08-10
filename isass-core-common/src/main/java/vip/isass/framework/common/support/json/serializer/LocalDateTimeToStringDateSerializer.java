// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import vip.isass.framework.common.converter.datatime.LocalDateTimeToStringDateConverter;

import java.time.LocalDateTime;

public class LocalDateTimeToStringDateSerializer extends ValueSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext serializers) {
        gen.writeString(LocalDateTimeToStringDateConverter.convert0(value));
    }

}