package vip.isass.framework.nocode.v3.operation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes one nocode v3 service operation.
 *
 * @param entityName    logical entity name
 * @param operationName logical operation name
 * @param arguments     operation arguments
 * @param returnType    expected return type
 */
public record NocodeOperation(
        String entityName,
        String operationName,
        Map<String, Object> arguments,
        Class<?> returnType
) {

    public NocodeOperation {
        entityName = requireText(entityName, "entityName");
        operationName = requireText(operationName, "operationName");
        arguments = arguments == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        returnType = returnType == null ? Object.class : returnType;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
