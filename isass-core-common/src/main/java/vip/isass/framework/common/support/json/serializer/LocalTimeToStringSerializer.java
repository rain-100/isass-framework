// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import vip.isass.framework.common.converter.datatime.LocalTimeToStringConverter;

import java.time.LocalTime;

public class LocalTimeToStringSerializer extends ValueSerializer<LocalTime> {

    @Override
    public void serialize(LocalTime value, JsonGenerator gen, SerializationContext serializers) {
        gen.writeString(LocalTimeToStringConverter.convert0(value));
    }

}