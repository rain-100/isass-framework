// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

/**
 * Transport failure with enough state to make cross-protocol fallback safe.
 */
public class TransportInvocationException extends RuntimeException {

    private final boolean requestSent;

    public TransportInvocationException(String message, boolean requestSent) {
        super(message);
        this.requestSent = requestSent;
    }

    public TransportInvocationException(String message, boolean requestSent, Throwable cause) {
        super(message, cause);
        this.requestSent = requestSent;
    }

    public boolean requestSent() {
        return requestSent;
    }
}
