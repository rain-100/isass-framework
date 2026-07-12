package vip.isass.framework.nocode.transport;

import java.util.List;

public record Invocation(
        String serviceName,
        String entityName,
        String operationName,
        List<Object> arguments,
        boolean idempotent
) {
    public Invocation {
        arguments = List.copyOf(arguments == null ? List.of() : arguments);
    }
}
