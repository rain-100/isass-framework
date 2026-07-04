package vip.isass.framework.nocode.v3.transport;

import java.util.List;

public record V3Invocation(
        String serviceName,
        String entityName,
        String operationName,
        List<Object> arguments,
        boolean idempotent
) {
    public V3Invocation {
        arguments = List.copyOf(arguments == null ? List.of() : arguments);
    }
}
