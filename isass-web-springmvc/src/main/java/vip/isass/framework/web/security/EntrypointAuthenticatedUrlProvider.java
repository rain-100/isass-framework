// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.util.Collection;

/** Provides authenticated-only URLs declared by locally implemented Entrypoint operations. */
public final class EntrypointAuthenticatedUrlProvider {

    private final ServiceDefinitionRegistry serviceDefinitionRegistry;

    public EntrypointAuthenticatedUrlProvider(ServiceDefinitionRegistry serviceDefinitionRegistry) {
        this.serviceDefinitionRegistry = serviceDefinitionRegistry;
    }

    public Collection<String> getUrls() {
        return serviceDefinitionRegistry.all().stream()
                .filter(service -> service.localImplementation())
                .flatMap(service -> service.operations().stream()
                        .filter(operation -> operation.accessStrategy() == UrlAccessSecurityStrategy.AUTHENTICATED)
                        .map(operation -> service.pathPrefix(operation)
                                + "/" + operation.operationName()))
                .distinct()
                .sorted()
                .toList();
    }
}
