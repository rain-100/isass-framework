package vip.isass.framework.nocode.v3.operation;

import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeEntityRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Applies CRUD write payload rules before invoking save and update providers.
 */
public class NocodeCrudWriteInterceptor implements NocodeOperationInterceptor {

    private static final String ARG_BODY = "body";

    private final NocodeEntityRegistry entityRegistry;
    private final NocodeCrudWritePayloadProcessor payloadProcessor;

    public NocodeCrudWriteInterceptor(NocodeEntityRegistry entityRegistry) {
        this(entityRegistry, new NocodeCrudWritePayloadProcessor());
    }

    public NocodeCrudWriteInterceptor(
            NocodeEntityRegistry entityRegistry,
            NocodeCrudWritePayloadProcessor payloadProcessor
    ) {
        this.entityRegistry = Objects.requireNonNull(entityRegistry, "entityRegistry");
        this.payloadProcessor = Objects.requireNonNull(payloadProcessor, "payloadProcessor");
    }

    @Override
    public <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next) {
        return next.invoke(prepare(operation));
    }

    private NocodeOperation prepare(NocodeOperation operation) {
        Optional<NocodeCrudOperation> crudOperation = NocodeCrudOperation.fromOperationName(operation.operationName());
        if (crudOperation.isEmpty() || !isWriteOperation(crudOperation.get())) {
            return operation;
        }

        Object body = operation.arguments().get(ARG_BODY);
        if (!(body instanceof Map<?, ?> bodyMap)) {
            return operation;
        }

        Optional<NocodeEntityDefinition> entityDefinition = entityRegistry.find(operation.entityName());
        if (entityDefinition.isEmpty()) {
            return operation;
        }

        LinkedHashMap<String, Object> arguments = new LinkedHashMap<>(operation.arguments());
        arguments.put(ARG_BODY, payloadProcessor.prepare(
                crudOperation.get(),
                entityDefinition.get(),
                toStringKeyMap(bodyMap)
        ));
        return new NocodeOperation(
                operation.entityName(),
                operation.operationName(),
                arguments,
                operation.returnType()
        );
    }

    private boolean isWriteOperation(NocodeCrudOperation operation) {
        return operation == NocodeCrudOperation.SAVE || operation == NocodeCrudOperation.UPDATE_BY_ID;
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> bodyMap) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : bodyMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}
