// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.metadata;

import vip.isass.framework.entrypoint.IEntrypoint;

import java.util.List;

public record ServiceDefinition(
        String serviceName,
        String contextName,
        String resourceName,
        String tag,
        int displayOrder,
        Class<? extends IEntrypoint> serviceInterface,
        List<OperationDefinition> operations,
        boolean localImplementation
) {

    public ServiceDefinition {
        operations = List.copyOf(operations);
    }

    public ServiceDefinition(String serviceName, String contextName, String resourceName,
                             Class<? extends IEntrypoint> serviceInterface,
                             List<OperationDefinition> operations, boolean localImplementation) {
        this(serviceName, contextName, resourceName, contextName + "/" + resourceName, 1000,
                serviceInterface, operations, localImplementation);
    }

    public ServiceDefinition(String serviceName, String contextName, String resourceName, String tag,
                             Class<? extends IEntrypoint> serviceInterface,
                             List<OperationDefinition> operations, boolean localImplementation) {
        this(serviceName, contextName, resourceName, tag, 1000,
                serviceInterface, operations, localImplementation);
    }

    public String key() {
        return serviceName + "/" + contextName + "/" + resourceName;
    }

    public boolean hasNocodeOperations() {
        return operations.stream().anyMatch(OperationDefinition::nocode);
    }

    public String pathPrefix(OperationDefinition operation) {
        return "/" + serviceName + (operation.nocode() ? "/nocode" : "")
                + "/" + contextName + "/" + resourceName;
    }
}
