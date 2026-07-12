package vip.isass.framework.nocode.stream;

/**
 *  文件资源不存在。
 *
 * <p>HTTP 传输层将此异常转换为不含 {@code Resp} 响应体的 HTTP 404。</p>
 */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
