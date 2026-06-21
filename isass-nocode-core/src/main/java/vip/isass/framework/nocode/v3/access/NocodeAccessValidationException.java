package vip.isass.framework.nocode.v3.access;

/**
 * Thrown when a standard nocode access request does not match its argument contract.
 */
public class NocodeAccessValidationException extends IllegalArgumentException {

    public NocodeAccessValidationException(String message) {
        super(message);
    }
}
