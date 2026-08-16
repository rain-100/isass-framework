// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.transport;

/** A transport failure that records whether another protocol may still be selected safely. */
public class EntrypointTransportException extends RuntimeException {

    private final boolean unavailableBeforeSend;

    public EntrypointTransportException(String message, boolean unavailableBeforeSend) {
        super(message);
        this.unavailableBeforeSend = unavailableBeforeSend;
    }

    public EntrypointTransportException(String message, boolean unavailableBeforeSend, Throwable cause) {
        super(message, cause);
        this.unavailableBeforeSend = unavailableBeforeSend;
    }

    public boolean unavailableBeforeSend() {
        return unavailableBeforeSend;
    }
}
