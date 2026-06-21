package vip.isass.framework.nocode.v3.operation;

import vip.isass.framework.common.support.api.IsassOrderUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Runs nocode v3 operation interceptors around the final invoker.
 */
public class NocodeOperationPipeline {

    private final List<NocodeOperationInterceptor> interceptors;

    public NocodeOperationPipeline(Collection<? extends NocodeOperationInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors == null ? List.of() : interceptors);
        this.interceptors.sort(Comparator.comparingInt(IsassOrderUtil::getOrder));
    }

    public <R> R invoke(NocodeOperation operation, NocodeOperationInvoker<R> invoker) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(invoker, "invoker");

        NocodeOperationInvoker<R> chain = invoker;
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            NocodeOperationInterceptor interceptor = interceptors.get(i);
            NocodeOperationInvoker<R> next = chain;
            chain = currentOperation -> interceptor.intercept(currentOperation, next);
        }
        return chain.invoke(operation);
    }
}
