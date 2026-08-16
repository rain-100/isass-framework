// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.transport;

import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

public interface EntrypointTransport {

    String name();

    boolean supports(ServiceDefinition service, OperationDefinition operation);

    Object invoke(ServiceDefinition service, OperationDefinition operation, Object[] arguments);
}
