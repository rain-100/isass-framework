package vip.isass.framework.nocode.v3.stream;

import java.io.InputStream;
import java.util.Objects;

/**
 * 传输无关的文件流响应。
 *
 * <p>调用方必须在读取完成后关闭 {@link #content()}。</p>
 */
public record V3FileStream(
        String fileName,
        String contentType,
        Long contentLength,
        boolean download,
        InputStream content
) {
    public V3FileStream {
        content = Objects.requireNonNull(content, "content");
        fileName = fileName == null ? "file" : fileName;
        contentType = contentType == null ? "application/octet-stream" : contentType;
    }
}
