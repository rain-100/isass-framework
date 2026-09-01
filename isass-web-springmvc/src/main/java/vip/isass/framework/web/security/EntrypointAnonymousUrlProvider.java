// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.util.Collection;

/** Provides anonymous URLs declared by locally implemented Entrypoint operations. */
public final class EntrypointAnonymousUrlProvider implements PermitUrlProvider {

    private final ServiceDefinitionRegistry serviceDefinitionRegistry;

    public EntrypointAnonymousUrlProvider(ServiceDefinitionRegistry serviceDefinitionRegistry) {
        this.serviceDefinitionRegistry = serviceDefinitionRegistry;
    }

    @Override
    public Collection<String> getUrls() {
        return serviceDefinitionRegistry.all().stream()
                .filter(service -> service.localImplementation())
                .flatMap(service -> service.operations().stream()
                        .filter(operation -> operation.accessStrategy() == UrlAccessSecurityStrategy.NONE)
                        .map(operation -> service.pathPrefix(operation)
                                + "/" + operation.operationName()))
                .distinct()
                .sorted()
                .toList();
    }
}
