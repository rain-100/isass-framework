// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.grpc;

import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.stream.FileStream;

import java.util.Arrays;

/**
 * 动态 gRPC 文件流帧：首帧为元数据，后续帧为原始文件字节，避免 Base64 编码。
 */
final class GrpcFileFrames {

    private static final byte METADATA = 1;
    private static final byte CONTENT = 2;

    private GrpcFileFrames() {
    }

    static byte[] metadata(FileStream stream, ObjectMapper objectMapper) {
        byte[] json = objectMapper.writeValueAsBytes(new Metadata(
                stream.fileName(), stream.contentType(), stream.contentLength(), stream.download()));
        return frame(METADATA, json, 0, json.length);
    }

    static Metadata parseMetadata(byte[] frame, ObjectMapper objectMapper) {
        requireType(frame, METADATA);
        return objectMapper.readValue(frame, 1, frame.length - 1, Metadata.class);
    }

    static byte[] content(byte[] bytes, int offset, int length) {
        return frame(CONTENT, bytes, offset, length);
    }

    static byte[] parseContent(byte[] frame) {
        requireType(frame, CONTENT);
        return Arrays.copyOfRange(frame, 1, frame.length);
    }

    private static byte[] frame(byte type, byte[] payload, int offset, int length) {
        byte[] frame = new byte[length + 1];
        frame[0] = type;
        System.arraycopy(payload, offset, frame, 1, length);
        return frame;
    }

    private static void requireType(byte[] frame, byte expectedType) {
        if (frame == null || frame.length == 0 || frame[0] != expectedType) {
            throw new IllegalStateException("Invalid  gRPC file frame");
        }
    }

    record Metadata(String fileName, String contentType, Long contentLength, boolean download) {
        Metadata {
            fileName = fileName == null ? "file" : fileName;
            contentType = contentType == null ? "application/octet-stream" : contentType;
        }
    }
}
