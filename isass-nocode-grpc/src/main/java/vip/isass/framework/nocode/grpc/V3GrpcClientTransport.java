package vip.isass.framework.nocode.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.ClientCalls;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;
import vip.isass.framework.nocode.v3.transport.V3Invocation;
import vip.isass.framework.nocode.v3.transport.V3InvocationTransport;
import vip.isass.framework.nocode.v3.transport.V3TransportInvocationException;
import vip.isass.framework.nocode.v3.transport.V3TransportKind;
import vip.isass.framework.nocode.v3.stream.V3FileStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class V3GrpcClientTransport implements V3InvocationTransport {

    private final Channel channel;
    private final V3ContractRegistry contracts;
    private final ObjectMapper objectMapper;

    public V3GrpcClientTransport(
            Channel channel,
            V3ContractRegistry contracts,
            ObjectMapper objectMapper
    ) {
        this.channel = channel;
        this.contracts = contracts;
        this.objectMapper = objectMapper;
    }

    public V3TransportKind kind() {
        return V3TransportKind.GRPC;
    }

    public boolean available(V3Invocation invocation) {
        V3ServiceContract service = contracts.requireService(
                invocation.serviceName(), invocation.entityName());
        V3OperationContract operation = service.operations().stream()
                .filter(candidate -> candidate.name().equals(invocation.operationName()))
                .findFirst().orElseThrow();
        // 上传流仍由 HTTP transport 承载；文件下载使用 gRPC server-streaming。
        return operation.parameters().stream()
                .noneMatch(parameter -> InputStream.class.getName().equals(parameter.javaType()));
    }

    public Object invoke(V3Invocation invocation) {
        V3ServiceContract service = contracts.requireService(
                invocation.serviceName(), invocation.entityName());
        V3OperationContract operation = service.operations().stream()
                .filter(candidate -> candidate.name().equals(invocation.operationName()))
                .findFirst()
                .orElseThrow();
        try {
            byte[] request = objectMapper.writeValueAsBytes(invocation.arguments());
            if (V3FileStream.class.getName().equals(operation.returnJavaType())) {
                return invokeFile(service, operation, request);
            }
            byte[] response = ClientCalls.blockingUnaryCall(
                    channel, V3GrpcDescriptors.method(service, operation), CallOptions.DEFAULT, request);
            return objectMapper.readValue(
                    response, objectMapper.getTypeFactory().constructFromCanonical(operation.returnJavaType()));
        } catch (RuntimeException exception) {
            throw new V3TransportInvocationException(
                    "V3 gRPC invocation failed: " + invocation.operationName(), true, exception);
        }
    }

    private V3FileStream invokeFile(
            V3ServiceContract service,
            V3OperationContract operation,
            byte[] request
    ) {
        Iterator<byte[]> frames = ClientCalls.blockingServerStreamingCall(
                channel, V3GrpcDescriptors.fileStreamMethod(service, operation), CallOptions.DEFAULT, request);
        if (!frames.hasNext()) {
            throw new V3TransportInvocationException(
                    "V3 gRPC file response has no metadata: " + operation.name(), true, null);
        }
        V3GrpcFileFrames.Metadata metadata = V3GrpcFileFrames.parseMetadata(frames.next(), objectMapper);
        return new V3FileStream(metadata.fileName(), metadata.contentType(), metadata.contentLength(),
                metadata.download(), output -> copyFrames(frames, output));
    }

    private void copyFrames(Iterator<byte[]> frames, OutputStream output) throws IOException {
        while (frames.hasNext()) {
            output.write(V3GrpcFileFrames.parseContent(frames.next()));
        }
    }
}
