package vip.isass.framework.nocode.v3.operation;

import vip.isass.framework.nocode.v3.routing.NocodeOperationProvider;
import vip.isass.framework.nocode.v3.routing.NocodeOperationRouter;
import vip.isass.framework.nocode.v3.routing.NocodeRouteMode;

import java.util.Collection;
import java.util.Objects;

/**
 * Executes an operation by routing to a provider and applying the interceptor pipeline.
 */
public class NocodeOperationExecutor {

    private final NocodeOperationRouter router;
    private final NocodeOperationPipeline pipeline;

    public NocodeOperationExecutor(Collection<? extends NocodeOperationProvider<?>> providers,
                                   Collection<? extends NocodeOperationInterceptor> interceptors) {
        this(new NocodeOperationRouter(providers), new NocodeOperationPipeline(interceptors));
    }

    public NocodeOperationExecutor(NocodeOperationRouter router, NocodeOperationPipeline pipeline) {
        this.router = Objects.requireNonNull(router, "router");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    public <R> R execute(NocodeOperation operation, NocodeRouteMode routeMode) {
        Objects.requireNonNull(operation, "operation");
        NocodeOperationProvider<R> provider = router.route(operation, routeMode);
        return pipeline.invoke(operation, provider);
    }
}
