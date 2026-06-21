package vip.isass.framework.nocode.v3.operation;

import vip.isass.framework.common.support.api.IsassOrderUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Bridges operation listeners into the interceptor pipeline.
 */
public class NocodeOperationListenerInterceptor implements NocodeOperationInterceptor {

    private final List<NocodeOperationListener> listeners;

    public NocodeOperationListenerInterceptor(Collection<? extends NocodeOperationListener> listeners) {
        this.listeners = new ArrayList<>(listeners == null ? List.of() : listeners);
        this.listeners.sort(Comparator.comparingInt(IsassOrderUtil::getOrder));
    }

    @Override
    public <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next) {
        for (NocodeOperationListener listener : listeners) {
            listener.before(operation);
        }
        try {
            R result = next.invoke(operation);
            for (int i = listeners.size() - 1; i >= 0; i--) {
                listeners.get(i).after(operation, result);
            }
            return result;
        } catch (RuntimeException exception) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                listeners.get(i).onError(operation, exception);
            }
            throw exception;
        }
    }
}
