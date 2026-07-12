package vip.isass.framework.nocode.grpc;

import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;
import vip.isass.framework.nocode.v3.stream.V3FileStream;

import java.util.List;

/**
 * Builds all V3 gRPC service definitions without per-entity Spring Beans.
 */
public class V3GrpcServerAdapter {

    private final List<ServerServiceDefinition> serviceDefinitions;
    private final ObjectMapper objectMapper;

    public V3GrpcServerAdapter(
            List<V3ServiceContract> contracts,
            V3GrpcInvocationHandler invocationHandler,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.serviceDefinitions = contracts.stream()
                .map(contract -> buildService(contract, invocationHandler))
                .toList();
    }

    public List<ServerServiceDefinition> serviceDefinitions() {
        return serviceDefinitions;
    }

    private ServerServiceDefinition buildService(
            V3ServiceContract contract,
            V3GrpcInvocationHandler invocationHandler
    ) {
        String serviceName = V3GrpcDescriptors.serviceName(contract);
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(serviceName);
        for (V3OperationContract operation : contract.operations()) {
            if (isFileOperation(operation)) {
                builder.addMethod(V3GrpcDescriptors.fileStreamMethod(contract, operation),
                        ServerCalls.asyncServerStreamingCall(
                                (byte[] request, StreamObserver<byte[]> response) -> streamFile(
                                        contract, operation, request, response, invocationHandler)));
            } else {
                var descriptor = V3GrpcDescriptors.method(contract, operation);
                builder.addMethod(descriptor, ServerCalls.asyncUnaryCall(
                        (byte[] request, StreamObserver<byte[]> response) -> {
                            try {
                                response.onNext(invocationHandler.invoke(contract, operation, request));
                                response.onCompleted();
                            } catch (Throwable throwable) {
                                response.onError(throwable);
                            }
                        }));
            }
        }
        return builder.build();
    }

    private void streamFile(
            V3ServiceContract contract,
            V3OperationContract operation,
            byte[] request,
            StreamObserver<byte[]> response,
            V3GrpcInvocationHandler invocationHandler
    ) {
        try {
            V3FileStream fileStream = invocationHandler.invokeFile(contract, operation, request);
            response.onNext(V3GrpcFileFrames.metadata(fileStream, objectMapper));
            V3GrpcChunkOutputStream output = new V3GrpcChunkOutputStream(response);
            fileStream.writeTo(output);
            output.flush();
            response.onCompleted();
        } catch (Throwable throwable) {
            response.onError(throwable);
        }
    }

    private boolean isFileOperation(V3OperationContract operation) {
        return V3FileStream.class.getName().equals(operation.returnJavaType());
    }
}
