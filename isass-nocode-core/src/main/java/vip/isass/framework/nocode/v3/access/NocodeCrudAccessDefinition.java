package vip.isass.framework.nocode.v3.access;

import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Standard CRUD access argument contract shared by access adapters and code generators.
 */
public record NocodeCrudAccessDefinition(
        NocodeCrudOperation operation,
        List<String> requiredArguments,
        List<String> optionalArguments
) {

    private static final Map<NocodeCrudOperation, NocodeCrudAccessDefinition> DEFINITIONS = definitions();

    public NocodeCrudAccessDefinition {
        operation = Objects.requireNonNull(operation, "operation");
        requiredArguments = immutable(requiredArguments);
        optionalArguments = immutable(optionalArguments);
    }

    public boolean requiresArgument(String argumentName) {
        return requiredArguments.contains(argumentName);
    }

    public boolean supportsArgument(String argumentName) {
        return requiredArguments.contains(argumentName) || optionalArguments.contains(argumentName);
    }

    public static List<NocodeCrudAccessDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static Optional<NocodeCrudAccessDefinition> find(NocodeCrudOperation operation) {
        return Optional.ofNullable(DEFINITIONS.get(operation));
    }

    public static Optional<NocodeCrudAccessDefinition> find(String operationName) {
        return NocodeCrudOperation.fromOperationName(operationName).flatMap(NocodeCrudAccessDefinition::find);
    }

    private static Map<NocodeCrudOperation, NocodeCrudAccessDefinition> definitions() {
        Map<NocodeCrudOperation, NocodeCrudAccessDefinition> definitions = new LinkedHashMap<>();
        register(definitions, NocodeCrudOperation.FIND_BY_ID,
                List.of(NocodeCrudAccessRequests.ARG_ID),
                List.of(NocodeCrudAccessRequests.ARG_FETCH_OPTIONS));
        register(definitions, NocodeCrudOperation.PAGE,
                List.of(NocodeCrudAccessRequests.ARG_CRITERIA),
                List.of(NocodeCrudAccessRequests.ARG_FETCH_OPTIONS));
        register(definitions, NocodeCrudOperation.LIST,
                List.of(NocodeCrudAccessRequests.ARG_CRITERIA),
                List.of(NocodeCrudAccessRequests.ARG_FETCH_OPTIONS));
        register(definitions, NocodeCrudOperation.SAVE,
                List.of(NocodeCrudAccessRequests.ARG_BODY),
                List.of());
        register(definitions, NocodeCrudOperation.UPDATE_BY_ID,
                List.of(NocodeCrudAccessRequests.ARG_ID, NocodeCrudAccessRequests.ARG_BODY),
                List.of());
        register(definitions, NocodeCrudOperation.DELETE_BY_ID,
                List.of(NocodeCrudAccessRequests.ARG_ID),
                List.of(NocodeCrudAccessRequests.ARG_DELETE_OPTIONS));
        return Collections.unmodifiableMap(definitions);
    }

    private static void register(Map<NocodeCrudOperation, NocodeCrudAccessDefinition> definitions,
                                 NocodeCrudOperation operation,
                                 List<String> requiredArguments,
                                 List<String> optionalArguments) {
        definitions.put(operation, new NocodeCrudAccessDefinition(operation, requiredArguments, optionalArguments));
    }

    private static List<String> immutable(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> copy = List.copyOf(values);
        copy.forEach(value -> {
            Objects.requireNonNull(value, "argumentName");
            if (value.isBlank()) {
                throw new IllegalArgumentException("argumentName must not be blank");
            }
        });
        return copy;
    }

    static {
        Arrays.stream(NocodeCrudOperation.values()).forEach(operation -> {
            if (!DEFINITIONS.containsKey(operation)) {
                throw new IllegalStateException("Missing CRUD access definition for " + operation);
            }
        });
    }
}
