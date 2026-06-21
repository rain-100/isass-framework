package vip.isass.framework.nocode.v3.routing;

import vip.isass.framework.nocode.v3.operation.NocodeOperation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Selects a local or remote v3 operation provider.
 */
public class NocodeOperationRouter {

    private final List<NocodeOperationProvider<?>> providers;

    public NocodeOperationRouter(Collection<? extends NocodeOperationProvider<?>> providers) {
        this.providers = List.copyOf(providers == null ? List.of() : providers);
    }

    @SuppressWarnings("unchecked")
    public <R> NocodeOperationProvider<R> route(NocodeOperation operation, NocodeRouteMode routeMode) {
        Objects.requireNonNull(operation, "operation");
        NocodeRouteMode mode = routeMode == null ? NocodeRouteMode.AUTO : routeMode;

        NocodeOperationProvider<?> provider = switch (mode) {
            case LOCAL -> find(operation, NocodeProviderType.LOCAL);
            case REMOTE -> find(operation, NocodeProviderType.REMOTE);
            case AUTO -> {
                NocodeOperationProvider<?> local = find(operation, NocodeProviderType.LOCAL);
                yield local == null ? find(operation, NocodeProviderType.REMOTE) : local;
            }
        };

        if (provider == null) {
            throw new IllegalStateException("No nocode provider found for " + operation.entityName() + "." + operation.operationName());
        }
        return (NocodeOperationProvider<R>) provider;
    }

    private NocodeOperationProvider<?> find(NocodeOperation operation, NocodeProviderType providerType) {
        return providers.stream()
                .filter(provider -> provider.getProviderType() == providerType)
                .filter(provider -> provider.supports(operation))
                .findFirst()
                .orElse(null);
    }
}
