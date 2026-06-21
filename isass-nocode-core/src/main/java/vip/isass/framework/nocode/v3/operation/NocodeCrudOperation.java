package vip.isass.framework.nocode.v3.operation;

import java.util.Arrays;
import java.util.Optional;

/**
 * Standard nocode v3 CRUD operation names shared by access adapters and providers.
 */
public enum NocodeCrudOperation {

    FIND_BY_ID("findById"),
    PAGE("page"),
    LIST("list"),
    SAVE("save"),
    UPDATE_BY_ID("updateById"),
    DELETE_BY_ID("deleteById");

    private final String operationName;

    NocodeCrudOperation(String operationName) {
        this.operationName = operationName;
    }

    public String getOperationName() {
        return operationName;
    }

    public static Optional<NocodeCrudOperation> fromOperationName(String operationName) {
        if (operationName == null || operationName.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(operation -> operation.operationName.equals(operationName))
                .findFirst();
    }
}
