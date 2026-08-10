// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.math.BigDecimal;

public class DoubleSerializer extends ValueSerializer<Double> {

    @Override
    public void serialize(Double value, JsonGenerator gen, SerializationContext serializers) {
        gen.writeNumber(new BigDecimal(value.toString()).toString());
    }

}
