// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

import java.util.Collection;
import java.util.Optional;

public interface ServiceDefinitionRegistry {

    Collection<ServiceDefinition> all();

    Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName);

    default ServiceDefinition require(String serviceName, String contextName, String resourceName) {
        return find(serviceName, contextName, resourceName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entrypoint: "
                        + serviceName + "/" + contextName + "/" + resourceName));
    }
}
