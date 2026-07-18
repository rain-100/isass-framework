package vip.isass.framework.nocode.transport;

import java.util.List;

public record Invocation(
        String service,
        String entity,
        String operationName,
        List<Object> arguments,
        boolean idempotent
) {
    public Invocation {
        arguments = List.copyOf(arguments == null ? List.of() : arguments);
    }
}
