package vip.isass.framework.nocode.v3.validation;

import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeEntityRegistry;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationInterceptor;
import vip.isass.framework.nocode.v3.operation.NocodeOperationInvoker;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validates standard CRUD write bodies before provider invocation.
 */
public class NocodeCrudValidationInterceptor implements NocodeOperationInterceptor {

    private static final String ARG_BODY = "body";

    private final NocodeEntityRegistry entityRegistry;
    private final NocodeEntityValidator validator;

    public NocodeCrudValidationInterceptor(NocodeEntityRegistry entityRegistry) {
        this(entityRegistry, new NocodeEntityValidator());
    }

    public NocodeCrudValidationInterceptor(NocodeEntityRegistry entityRegistry, NocodeEntityValidator validator) {
        this.entityRegistry = Objects.requireNonNull(entityRegistry, "entityRegistry");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next) {
        validationGroup(operation).ifPresent(group -> validate(operation, group));
        return next.invoke(operation);
    }

    private Optional<NocodeValidationGroup> validationGroup(NocodeOperation operation) {
        return NocodeCrudOperation.fromOperationName(operation.operationName())
                .flatMap(crudOperation -> switch (crudOperation) {
                    case SAVE -> Optional.of(NocodeValidationGroup.CREATE);
                    case UPDATE_BY_ID -> Optional.of(NocodeValidationGroup.UPDATE);
                    default -> Optional.empty();
                });
    }

    private void validate(NocodeOperation operation, NocodeValidationGroup group) {
        Object body = operation.arguments().get(ARG_BODY);
        if (!(body instanceof Map<?, ?> bodyMap)) {
            return;
        }
        Optional<NocodeEntityDefinition> entityDefinition = entityRegistry.find(operation.entityName());
        entityDefinition.ifPresent(definition -> validator.validateAndThrow(definition, toStringKeyMap(bodyMap), group));
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> bodyMap) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : bodyMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}
