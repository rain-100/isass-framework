package vip.isass.framework.nocode.v3.operation;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeEntityRegistry;
import vip.isass.framework.nocode.v3.model.NocodeFieldAutoFill;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeCrudWriteInterceptorTest {

    @Test
    void preparesWriteBodyBeforeProviderInvocation() {
        NocodeCrudWriteInterceptor interceptor = new NocodeCrudWriteInterceptor(
                new NocodeEntityRegistry(java.util.List.of(entityDefinition())),
                new NocodeCrudWritePayloadProcessor(() -> 1718950000000L)
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", 1L);
        body.put("name", "demo.txt");
        body.put("createTime", 1L);
        NocodeOperation operation = new NocodeOperation(
                "attachment",
                NocodeCrudOperation.SAVE.getOperationName(),
                Map.of("body", body),
                Map.class
        );

        Map<String, Object> result = interceptor.intercept(operation, next -> next.arguments());

        assertThat(result).containsKey("body");
        assertThat(result.get("body")).isEqualTo(Map.of(
                "name", "demo.txt",
                "createTime", 1718950000000L
        ));
    }

    @Test
    void ignoresNonWriteOperations() {
        NocodeCrudWriteInterceptor interceptor = new NocodeCrudWriteInterceptor(
                new NocodeEntityRegistry(java.util.List.of(entityDefinition())),
                new NocodeCrudWritePayloadProcessor(() -> 1718950000000L)
        );
        NocodeOperation operation = new NocodeOperation(
                "attachment",
                NocodeCrudOperation.FIND_BY_ID.getOperationName(),
                Map.of("id", 1L),
                Object.class
        );

        Map<String, Object> result = interceptor.intercept(operation, next -> next.arguments());

        assertThat(result).containsExactly(Map.entry("id", 1L));
    }

    private NocodeEntityDefinition entityDefinition() {
        return NocodeEntityDefinition.builder("attachment", Object.class)
                .field(NocodeFieldDefinition.builder("id", Long.class)
                        .idField(true)
                        .clientWritable(false)
                        .build())
                .field(NocodeFieldDefinition.builder("name", String.class).build())
                .field(NocodeFieldDefinition.builder("createTime", Long.class)
                        .clientWritable(false)
                        .autoFill(NocodeFieldAutoFill.CREATE_TIME)
                        .build())
                .build();
    }
}
