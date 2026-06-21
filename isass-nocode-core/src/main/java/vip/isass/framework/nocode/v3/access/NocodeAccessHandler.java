package vip.isass.framework.nocode.v3.access;

import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationExecutor;

import java.util.Objects;

/**
 * Converts framework-neutral access requests into nocode v3 operations.
 */
public class NocodeAccessHandler {

    private final NocodeOperationExecutor executor;

    public NocodeAccessHandler(NocodeOperationExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public <R> R handle(NocodeAccessRequest request) {
        Objects.requireNonNull(request, "request");
        NocodeOperation operation = new NocodeOperation(
                request.entityName(),
                request.operationName(),
                request.arguments(),
                request.returnType()
        );
        return executor.execute(operation, request.routeMode());
    }
}
