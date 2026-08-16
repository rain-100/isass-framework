// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import io.grpc.MethodDescriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {
    static final ByteArrayMarshaller INSTANCE = new ByteArrayMarshaller();

    @Override
    public InputStream stream(byte[] value) {
        return new ByteArrayInputStream(value);
    }

    @Override
    public byte[] parse(InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("读取 gRPC 数据失败", exception);
        }
    }
}
