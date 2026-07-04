package vip.isass.framework.nocode.v3.transport;

/**
 * Transport failure with enough state to make cross-protocol fallback safe.
 */
public class V3TransportInvocationException extends RuntimeException {

    private final boolean requestSent;

    public V3TransportInvocationException(String message, boolean requestSent) {
        super(message);
        this.requestSent = requestSent;
    }

    public V3TransportInvocationException(String message, boolean requestSent, Throwable cause) {
        super(message, cause);
        this.requestSent = requestSent;
    }

    public boolean requestSent() {
        return requestSent;
    }
}
