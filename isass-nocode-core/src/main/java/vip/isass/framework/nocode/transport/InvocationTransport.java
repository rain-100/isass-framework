// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

public interface InvocationTransport {

    TransportKind kind();

    boolean available(Invocation invocation);

    Object invoke(Invocation invocation);
}
