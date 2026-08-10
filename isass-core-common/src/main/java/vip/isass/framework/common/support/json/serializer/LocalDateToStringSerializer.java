// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import vip.isass.framework.common.converter.datatime.LocalDateToStringConverter;

import java.time.LocalDate;

public class LocalDateToStringSerializer extends ValueSerializer<LocalDate> {

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializationContext serializers) {
        gen.writeString(LocalDateToStringConverter.convert0(value));
    }

}