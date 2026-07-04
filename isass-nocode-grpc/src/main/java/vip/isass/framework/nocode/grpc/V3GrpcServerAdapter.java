package vip.isass.framework.nocode.grpc;

import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;

import java.util.List;

/**
 * Builds all V3 gRPC service definitions without per-entity Spring Beans.
 */
public class V3GrpcServerAdapter {

    private final List<ServerServiceDefinition> serviceDefinitions;

    public V3GrpcServerAdapter(
            List<V3ServiceContract> contracts,
            V3GrpcInvocationHandler invocationHandler
    ) {
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
        return builder.build();
    }
}
