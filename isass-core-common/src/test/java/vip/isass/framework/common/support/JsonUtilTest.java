// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.support.json.LocalDateTimeToLongConvert;
import vip.isass.framework.common.support.json.LocalDateToLongConvert;
import vip.isass.framework.common.support.json.LocalTimeToLongConvert;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonUtilTest {

    @Test
    void rejectsLossyFloatingPointToIntegerConversionForBothJacksonVersions() throws Exception {
        assertEquals(2087700000000000020L, JsonUtil.readValue("\"2087700000000000020\"", Long.class));
        assertThrows(JacksonException.class, () -> JsonUtil.readValue("100.5", Long.class));
        assertThrows(JacksonException.class, () -> JsonUtil.NOT_NULL_INSTANCE.readValue("100.5", Long.class));
        assertThrows(JsonProcessingException.class,
                () -> JsonUtil.LEGACY_MAPPER.readValue("100.5", Long.class));
        assertEquals(100.5D, JsonUtil.readValue("100.5", Double.class));
    }

    @Test
    void readsGenericJsonFromBytesAndStreams() {
        String json = "{\"items\":[1,2]}";
        Map<String, List<Integer>> fromBytes = JsonUtil.readValue(
                json.getBytes(StandardCharsets.UTF_8), new TypeReference<Map<String, List<Integer>>>() { }.getType());
        Map<String, List<Integer>> fromStream = JsonUtil.readValue(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new TypeReference<Map<String, List<Integer>>>() { });
        assertEquals(List.of(1, 2), fromBytes.get("items"));
        assertEquals(fromBytes, fromStream);
    }

    @Test
    void recognizesJsonObjectsAndArrays() {
        assertTrue(JsonUtil.isJson("{\"ok\":true}"));
        assertTrue(JsonUtil.isJson("[1,2]"));
        assertFalse(JsonUtil.isJson("plain text"));
        assertFalse(JsonUtil.isJson("\"text\""));
    }

    @Test
    void serializesTemporalValuesThroughTheirLongConverters() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 28, 8, 30);
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        assertEquals(JsonUtil.writeValue(new LocalDateTimeToLongConvert().convert(dateTime)), JsonUtil.writeValue(dateTime));
        assertEquals(JsonUtil.writeValue(new LocalDateToLongConvert().convert(date)), JsonUtil.writeValue(date));
        assertEquals(JsonUtil.writeValue(new LocalTimeToLongConvert().convert(time)), JsonUtil.writeValue(time));
    }

    @Test
    void legacyMapperIgnoresTransientFieldEvenWhenItHasAGetter() throws Exception {
        TransientBean bean = new TransientBean();
        assertEquals("{\"name\":\"visible\"}", JsonUtil.writeValue(bean));
        assertEquals("{\"name\":\"visible\"}", JsonUtil.LEGACY_MAPPER.writeValueAsString(bean));
    }

    @Test
    void conditionOnlySerializesJavaProperties() throws Exception {
        WhereCondition condition = new WhereCondition()
                .setSourceProperty("id").setTargetProperty("attFileId");
        String json = JsonUtil.writeValue(condition);
        String legacyJson = JsonUtil.LEGACY_MAPPER.writeValueAsString(condition);
        assertFalse(json.contains("sourceColumn"));
        assertFalse(json.contains("targetColumn"));
        assertFalse(legacyJson.contains("sourceColumn"));
        assertFalse(legacyJson.contains("targetColumn"));
    }

    private static final class TransientBean {
        private final String name = "visible";
        private transient String internal = "secret";

        public String getName() {
            return name;
        }

        public String getInternal() {
            return internal;
        }
    }
}
