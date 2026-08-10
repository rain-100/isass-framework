// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.ClientCalls;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.transport.Invocation;
import vip.isass.framework.nocode.transport.InvocationTransport;
import vip.isass.framework.nocode.transport.TransportInvocationException;
import vip.isass.framework.nocode.transport.TransportKind;
import vip.isass.framework.nocode.stream.FileStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class GrpcClientTransport implements InvocationTransport {

    private final Channel channel;
    private final ContractRegistry contracts;
    private final ObjectMapper objectMapper;

    public GrpcClientTransport(
            Channel channel,
            ContractRegistry contracts,
            ObjectMapper objectMapper
    ) {
        this.channel = channel;
        this.contracts = contracts;
        this.objectMapper = objectMapper;
    }

    public TransportKind kind() {
        return TransportKind.GRPC;
    }

    public boolean available(Invocation invocation) {
        ServiceContract service = contracts.requireService(
                invocation.service(), invocation.entity());
        OperationContract operation = service.operations().stream()
                .filter(candidate -> candidate.name().equals(invocation.operationName()))
                .findFirst().orElseThrow();
        // 上传流仍由 HTTP transport 承载；文件下载使用 gRPC server-streaming。
        return operation.parameters().stream()
                .noneMatch(parameter -> InputStream.class.getName().equals(parameter.javaType()));
    }

    public Object invoke(Invocation invocation) {
        ServiceContract service = contracts.requireService(
                invocation.service(), invocation.entity());
        OperationContract operation = service.operations().stream()
                .filter(candidate -> candidate.name().equals(invocation.operationName()))
                .findFirst()
                .orElseThrow();
        try {
            byte[] request = objectMapper.writeValueAsBytes(invocation.arguments());
            if (FileStream.class.getName().equals(operation.returnJavaType())) {
                return invokeFile(service, operation, request);
            }
            byte[] response = ClientCalls.blockingUnaryCall(
                    channel, GrpcDescriptors.method(service, operation), CallOptions.DEFAULT, request);
            return objectMapper.readValue(
                    response, objectMapper.getTypeFactory().constructFromCanonical(operation.returnJavaType()));
        } catch (RuntimeException exception) {
            throw new TransportInvocationException(
                    " gRPC invocation failed: " + invocation.operationName(), true, exception);
        }
    }

    private FileStream invokeFile(
            ServiceContract service,
            OperationContract operation,
            byte[] request
    ) {
        Iterator<byte[]> frames = ClientCalls.blockingServerStreamingCall(
                channel, GrpcDescriptors.fileStreamMethod(service, operation), CallOptions.DEFAULT, request);
        if (!frames.hasNext()) {
            throw new TransportInvocationException(
                    " gRPC file response has no metadata: " + operation.name(), true, null);
        }
        GrpcFileFrames.Metadata metadata = GrpcFileFrames.parseMetadata(frames.next(), objectMapper);
        return new FileStream(metadata.fileName(), metadata.contentType(), metadata.contentLength(),
                metadata.download(), output -> copyFrames(frames, output));
    }

    private void copyFrames(Iterator<byte[]> frames, OutputStream output) throws IOException {
        while (frames.hasNext()) {
            output.write(GrpcFileFrames.parseContent(frames.next()));
        }
    }
}
