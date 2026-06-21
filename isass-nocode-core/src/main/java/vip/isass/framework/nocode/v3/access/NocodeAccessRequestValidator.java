package vip.isass.framework.nocode.v3.access;

import java.util.Map;
import java.util.Objects;

/**
 * Validates framework-neutral access requests against standard CRUD argument definitions.
 */
public class NocodeAccessRequestValidator {

    public void validate(NocodeAccessRequest request) {
        Objects.requireNonNull(request, "request");
        NocodeCrudAccessDefinition.find(request.operationName())
                .ifPresent(definition -> validate(request, definition));
    }

    private void validate(NocodeAccessRequest request, NocodeCrudAccessDefinition definition) {
        Map<String, Object> arguments = request.arguments();
        for (String argumentName : definition.requiredArguments()) {
            if (!arguments.containsKey(argumentName)) {
                throw new NocodeAccessValidationException(
                        "Missing required argument '" + argumentName + "' for " + request.operationName()
                );
            }
        }
        for (String argumentName : arguments.keySet()) {
            if (!definition.supportsArgument(argumentName)) {
                throw new NocodeAccessValidationException(
                        "Unsupported argument '" + argumentName + "' for " + request.operationName()
                );
            }
        }
    }
}
