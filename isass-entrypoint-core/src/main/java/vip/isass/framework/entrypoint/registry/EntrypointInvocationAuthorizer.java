// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

/** Transport-independent authorization hook executed immediately before a local entrypoint invocation. */
@FunctionalInterface
public interface EntrypointInvocationAuthorizer {
    void check(ServiceDefinition service, OperationDefinition operation, Object[] arguments);
}
