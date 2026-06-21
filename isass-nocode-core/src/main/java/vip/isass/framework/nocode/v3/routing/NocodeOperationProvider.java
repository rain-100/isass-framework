package vip.isass.framework.nocode.v3.routing;

import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationInvoker;

/**
 * Provides the actual implementation for a nocode v3 operation.
 *
 * @param <R> return type
 */
public interface NocodeOperationProvider<R> extends NocodeOperationInvoker<R> {

    NocodeProviderType getProviderType();

    boolean supports(NocodeOperation operation);

}
