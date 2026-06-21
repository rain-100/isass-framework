package vip.isass.framework.nocode.v3.operation;

/**
 * Listens to nocode v3 operation execution without replacing the provider.
 */
public interface NocodeOperationListener {

    default void before(NocodeOperation operation) {
    }

    default void after(NocodeOperation operation, Object result) {
    }

    default void onError(NocodeOperation operation, RuntimeException exception) {
    }
}
