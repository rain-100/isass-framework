package vip.isass.framework.nocode.v3.operation;

import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Applies field write rules before standard CRUD save and update operations.
 */
public class NocodeCrudWritePayloadProcessor {

    private final LongSupplier timestampSupplier;

    public NocodeCrudWritePayloadProcessor() {
        this(System::currentTimeMillis);
    }

    public NocodeCrudWritePayloadProcessor(LongSupplier timestampSupplier) {
        this.timestampSupplier = Objects.requireNonNull(timestampSupplier, "timestampSupplier");
    }

    public Map<String, Object> prepareCreate(NocodeEntityDefinition entityDefinition, Map<String, ?> body) {
        return prepare(NocodeCrudOperation.SAVE, entityDefinition, body);
    }

    public Map<String, Object> prepareUpdate(NocodeEntityDefinition entityDefinition, Map<String, ?> body) {
        return prepare(NocodeCrudOperation.UPDATE_BY_ID, entityDefinition, body);
    }

    public Map<String, Object> prepare(
            NocodeCrudOperation operation,
            NocodeEntityDefinition entityDefinition,
            Map<String, ?> body
    ) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(entityDefinition, "entityDefinition");

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        if (body != null) {
            for (Map.Entry<String, ?> entry : body.entrySet()) {
                String fieldName = entry.getKey();
                if (canAcceptClientValue(entityDefinition, fieldName)) {
                    result.put(fieldName, entry.getValue());
                }
            }
        }

        Long timestamp = null;
        for (NocodeFieldDefinition field : entityDefinition.fields()) {
            if (shouldAutoFill(operation, field)) {
                if (timestamp == null) {
                    timestamp = timestampSupplier.getAsLong();
                }
                result.put(field.fieldName(), timestamp);
            }
        }
        return result;
    }

    private boolean canAcceptClientValue(NocodeEntityDefinition entityDefinition, String fieldName) {
        return entityDefinition.field(fieldName)
                .map(NocodeFieldDefinition::clientWritable)
                .orElse(true);
    }

    private boolean shouldAutoFill(NocodeCrudOperation operation, NocodeFieldDefinition field) {
        return switch (operation) {
            case SAVE -> field.autoFill().fillOnCreate();
            case UPDATE_BY_ID -> field.autoFill().fillOnUpdate();
            default -> false;
        };
    }
}
