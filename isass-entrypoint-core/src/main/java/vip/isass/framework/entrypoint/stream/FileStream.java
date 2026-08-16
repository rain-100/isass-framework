// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 传输无关的文件流响应。
 *
 * <p>文件内容以写入目标 {@link OutputStream} 的方式提供，使 HTTP、gRPC 等传输层能够直接
 * 消费源流。每个实例只能消费一次：调用方必须在 {@link #writeTo(OutputStream)} 与
 * {@link #openInputStream()} 中二选一。</p>
 */
public final class FileStream {

    private static final int INPUT_STREAM_PIPE_BUFFER_SIZE = 64 * 1024;

    private final String fileName;
    private final String contentType;
    private final Long contentLength;
    private final boolean download;
    private final StreamBody body;
    private final AtomicReference<Consumption> consumption = new AtomicReference<>(Consumption.NEW);

    public FileStream(
            String fileName,
            String contentType,
            Long contentLength,
            boolean download,
            StreamBody body
    ) {
        this.fileName = fileName == null ? "file" : fileName;
        this.contentType = contentType == null ? "application/octet-stream" : contentType;
        this.contentLength = contentLength;
        this.download = download;
        this.body = Objects.requireNonNull(body, "body");
    }

    public String fileName() {
        return fileName;
    }

    public String contentType() {
        return contentType;
    }

    public Long contentLength() {
        return contentLength;
    }

    public boolean download() {
        return download;
    }

    /**
     * 直接把文件内容写入调用方提供的目标流。调用方负责目标流的关闭。
     */
    public void writeTo(OutputStream output) throws IOException {
        Objects.requireNonNull(output, "output");
        claim(Consumption.WRITING);
        try {
            body.writeTo(output);
        } finally {
            consumption.set(Consumption.CLOSED);
        }
    }

    /**
     * 为读取式业务代码提供兼容入口。
     *
     * <p>该方法仅在调用方确实需要 {@link InputStream} 时使用；它通过有界 Pipe 将写入式
     * 源流转换为读取式接口，并使用虚拟线程驱动源流。HTTP 与 gRPC 传输层应直接调用
     * {@link #writeTo(OutputStream)}。</p>
     */
    public InputStream openInputStream() {
        claim(Consumption.READING);
        try {
            PipedOutputStream output = new PipedOutputStream();
            PipedInputStream input = new PipedInputStream(output, INPUT_STREAM_PIPE_BUFFER_SIZE);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            AtomicBoolean cancelled = new AtomicBoolean();
            Thread.startVirtualThread(() -> {
                try (output) {
                    body.writeTo(output);
                } catch (Throwable exception) {
                    if (!cancelled.get()) {
                        failure.compareAndSet(null, exception);
                    }
                }
            });
            return new StreamInput(input, failure, cancelled, () -> consumption.set(Consumption.CLOSED));
        } catch (IOException exception) {
            consumption.set(Consumption.CLOSED);
            throw new IllegalStateException("无法创建  文件读取流", exception);
        }
    }

    private void claim(Consumption next) {
        if (!consumption.compareAndSet(Consumption.NEW, next)) {
            throw new IllegalStateException("FileStream 已被消费；同一文件流只能选择 writeTo(OutputStream) 或 openInputStream() 之一");
        }
    }

    private enum Consumption {
        NEW,
        WRITING,
        READING,
        CLOSED
    }

    private static final class StreamInput extends FilterInputStream {

        private final AtomicReference<Throwable> failure;
        private final AtomicBoolean cancelled;
        private final Runnable closeAction;
        private boolean closed;

        private StreamInput(
                InputStream input,
                AtomicReference<Throwable> failure,
                AtomicBoolean cancelled,
                Runnable closeAction
        ) {
            super(input);
            this.failure = failure;
            this.cancelled = cancelled;
            this.closeAction = closeAction;
        }

        @Override
        public int read() throws IOException {
            return checkRead(super.read());
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            return checkRead(super.read(bytes));
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            return checkRead(super.read(bytes, offset, length));
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            cancelled.set(true);
            try {
                super.close();
            } finally {
                closeAction.run();
            }
        }

        private int checkRead(int count) throws IOException {
            if (count >= 0) {
                return count;
            }
            closeAction.run();
            Throwable exception = failure.get();
            if (exception == null) {
                return -1;
            }
            throw new IOException(" 文件流读取失败", exception);
        }
    }
}
