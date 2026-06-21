package vip.isass.framework.nocode.v3.operation;

/**
 * Enhances a nocode v3 operation without implementing a whole service.
 */
@FunctionalInterface
public interface NocodeOperationInterceptor {

    <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next);

}
