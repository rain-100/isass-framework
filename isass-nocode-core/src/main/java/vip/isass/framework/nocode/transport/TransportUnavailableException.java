// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

public class TransportUnavailableException extends IllegalStateException {

    public TransportUnavailableException(Invocation invocation) {
        super(invocation == null
                ? "No  invocation transport is available"
                : "No  invocation transport is available for "
                + invocation.service() + "/" + invocation.entity()
                + "#" + invocation.operationName());
    }
}
