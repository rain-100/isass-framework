package vip.isass.framework.nocode.grpc;

import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.stream.FileStream;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds all  gRPC service definitions without per-entity Spring Beans.
 */
public class GrpcServerAdapter {

    private final List<ServerServiceDefinition> serviceDefinitions;
    private final ObjectMapper objectMapper;

    public GrpcServerAdapter(
            List<ServiceContract> contracts,
            GrpcInvocationHandler invocationHandler,
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
            ServiceContract contract,
        GrpcInvocationHandler invocationHandler
    ) {
        String service = GrpcDescriptors.service(contract);
        Map<OperationContract, MethodDescriptor<byte[], byte[]>> descriptors = new LinkedHashMap<>();
        for (OperationContract operation : contract.operations()) {
            descriptors.put(operation, isFileOperation(operation)
                    ? GrpcDescriptors.fileStreamMethod(contract, operation)
                    : GrpcDescriptors.method(contract, operation));
        }
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(
                new ServiceDescriptor(service, List.copyOf(descriptors.values())));
        for (Map.Entry<OperationContract, MethodDescriptor<byte[], byte[]>> entry : descriptors.entrySet()) {
            OperationContract operation = entry.getKey();
            if (isFileOperation(operation)) {
                builder.addMethod(entry.getValue(),
                        ServerCalls.asyncServerStreamingCall(
                                (byte[] request, StreamObserver<byte[]> response) -> streamFile(
                                        contract, operation, request, response, invocationHandler)));
            } else {
                builder.addMethod(entry.getValue(), ServerCalls.asyncUnaryCall(
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
            ServiceContract contract,
            OperationContract operation,
            byte[] request,
            StreamObserver<byte[]> response,
            GrpcInvocationHandler invocationHandler
    ) {
        try {
            FileStream fileStream = invocationHandler.invokeFile(contract, operation, request);
            response.onNext(GrpcFileFrames.metadata(fileStream, objectMapper));
            GrpcChunkOutputStream output = new GrpcChunkOutputStream(response);
            fileStream.writeTo(output);
            output.flush();
            response.onCompleted();
        } catch (Throwable throwable) {
            response.onError(throwable);
        }
    }

    private boolean isFileOperation(OperationContract operation) {
        return FileStream.class.getName().equals(operation.returnJavaType());
    }
}
