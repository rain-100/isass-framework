package vip.isass.framework.nocode.v3.stream;

/**
 * V3 文件资源不存在。
 *
 * <p>HTTP 传输层将此异常转换为不含 {@code Resp} 响应体的 HTTP 404。</p>
 */
public class V3FileNotFoundException extends RuntimeException {

    public V3FileNotFoundException(String message) {
        super(message);
    }

    public V3FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
