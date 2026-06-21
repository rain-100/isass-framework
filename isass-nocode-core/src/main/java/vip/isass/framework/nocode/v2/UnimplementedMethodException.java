package vip.isass.framework.nocode.v2;

/**
 * Thrown when a legacy v2 service implementation intentionally leaves an operation unimplemented.
 *
 * @author Rain
 */
public class UnimplementedMethodException extends RuntimeException {

    public UnimplementedMethodException() {
        super();
    }

    public UnimplementedMethodException(String message) {
        super(message);
    }

    public UnimplementedMethodException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnimplementedMethodException(Throwable cause) {
        super(cause);
    }

}
