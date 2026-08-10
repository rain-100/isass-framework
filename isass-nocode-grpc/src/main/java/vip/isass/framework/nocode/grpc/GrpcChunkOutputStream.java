// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.grpc;

import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * 将文件写入请求按固定大小输出为 gRPC server-streaming 消息。
 */
final class GrpcChunkOutputStream extends OutputStream {

    static final int CHUNK_SIZE = 64 * 1024;

    private final StreamObserver<byte[]> observer;
    private final byte[] buffer = new byte[CHUNK_SIZE];
    private int count;

    GrpcChunkOutputStream(StreamObserver<byte[]> observer) {
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    @Override
    public void write(int value) {
        buffer[count++] = (byte) value;
        if (count == buffer.length) {
            flushBuffer();
        }
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        while (length > 0) {
            int copied = Math.min(length, buffer.length - count);
            System.arraycopy(bytes, offset, buffer, count, copied);
            offset += copied;
            length -= copied;
            count += copied;
            if (count == buffer.length) {
                flushBuffer();
            }
        }
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    @Override
    public void close() {
        flushBuffer();
    }

    private void flushBuffer() {
        if (count == 0) {
            return;
        }
        observer.onNext(GrpcFileFrames.content(buffer, 0, count));
        count = 0;
    }
}
