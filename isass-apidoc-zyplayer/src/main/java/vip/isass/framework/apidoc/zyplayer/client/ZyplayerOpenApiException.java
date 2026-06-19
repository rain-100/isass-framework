package vip.isass.framework.apidoc.zyplayer.client;

/**
 * @author Rain
 */
public class ZyplayerOpenApiException extends RuntimeException {

    public ZyplayerOpenApiException(String message) {
        super(message);
    }

    public ZyplayerOpenApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
