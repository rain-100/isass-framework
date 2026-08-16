// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

/** Protocol-neutral invocation gateway used by transport server adapters. */
public interface EntrypointInvocationGateway {

    Object invoke(String serviceName, String contextName, String resourceName,
                  String operationName, Object[] arguments);
}
