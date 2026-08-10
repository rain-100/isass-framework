// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

import vip.isass.framework.nocode.contract.ServiceContract;

import java.util.List;

/**
 * Supplies the remote transports available for one generated nocode service contract.
 * Local services are intentionally not exposed here: a local Spring service bean wins
 * before a remote proxy is registered.
 */
public interface RemoteTransportProvider {

    List<InvocationTransport> transports(ServiceContract contract);
}
