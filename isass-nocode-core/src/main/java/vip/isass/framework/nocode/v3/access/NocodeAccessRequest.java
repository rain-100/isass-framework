package vip.isass.framework.nocode.v3.access;

import vip.isass.framework.nocode.v3.routing.NocodeRouteMode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-neutral access request for controller, message, task, or socket entrypoints.
 */
public record NocodeAccessRequest(
        String entityName,
        String operationName,
        Map<String, Object> arguments,
        Class<?> returnType,
        NocodeRouteMode routeMode
) {

    public NocodeAccessRequest {
        entityName = requireText(entityName, "entityName");
        operationName = requireText(operationName, "operationName");
        arguments = arguments == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        returnType = returnType == null ? Object.class : returnType;
        routeMode = routeMode == null ? NocodeRouteMode.AUTO : routeMode;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
