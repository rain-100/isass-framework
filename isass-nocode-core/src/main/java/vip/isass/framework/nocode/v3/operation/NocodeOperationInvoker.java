package vip.isass.framework.nocode.v3.operation;

/**
 * Executes a nocode v3 operation.
 *
 * @param <R> return type
 */
@FunctionalInterface
public interface NocodeOperationInvoker<R> {

    R invoke(NocodeOperation operation);

}
