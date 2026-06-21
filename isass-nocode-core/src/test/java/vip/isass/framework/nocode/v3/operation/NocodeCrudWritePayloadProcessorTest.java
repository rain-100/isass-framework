package vip.isass.framework.nocode.v3.operation;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeFieldAutoFill;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeCrudWritePayloadProcessorTest {

    private final NocodeEntityDefinition entityDefinition = NocodeEntityDefinition.builder("attachment", Object.class)
            .field(NocodeFieldDefinition.builder("id", Long.class)
                    .idField(true)
                    .clientWritable(false)
                    .build())
            .field(NocodeFieldDefinition.builder("name", String.class).build())
            .field(NocodeFieldDefinition.builder("createTime", Long.class)
                    .clientWritable(false)
                    .autoFill(NocodeFieldAutoFill.CREATE_TIME)
                    .build())
            .field(NocodeFieldDefinition.builder("updateTime", Long.class)
                    .clientWritable(false)
                    .autoFill(NocodeFieldAutoFill.UPDATE_TIME)
                    .build())
            .build();

    @Test
    void createRemovesClientReadonlyFieldsAndFillsCreateTime() {
        NocodeCrudWritePayloadProcessor processor = new NocodeCrudWritePayloadProcessor(() -> 1718950000000L);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", 9L);
        body.put("name", "demo.txt");
        body.put("createTime", 1L);
        body.put("updateTime", 2L);
        body.put("externalCode", "keep");

        Map<String, Object> result = processor.prepareCreate(entityDefinition, body);

        assertThat(result).containsExactly(
                Map.entry("name", "demo.txt"),
                Map.entry("externalCode", "keep"),
                Map.entry("createTime", 1718950000000L)
        );
        assertThat(body).containsEntry("id", 9L);
    }

    @Test
    void updateRemovesClientReadonlyFieldsAndFillsUpdateTime() {
        NocodeCrudWritePayloadProcessor processor = new NocodeCrudWritePayloadProcessor(() -> 1718950000000L);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "demo.txt");
        body.put("createTime", 1L);
        body.put("updateTime", 2L);

        Map<String, Object> result = processor.prepareUpdate(entityDefinition, body);

        assertThat(result).containsExactly(
                Map.entry("name", "demo.txt"),
                Map.entry("updateTime", 1718950000000L)
        );
    }
}
