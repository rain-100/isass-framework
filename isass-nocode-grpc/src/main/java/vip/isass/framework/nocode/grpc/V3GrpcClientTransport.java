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
        return true;
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
            byte[] response = ClientCalls.blockingUnaryCall(
                    channel, V3GrpcDescriptors.method(service, operation), CallOptions.DEFAULT, request);
            return objectMapper.readValue(
                    response, objectMapper.getTypeFactory().constructFromCanonical(operation.returnJavaType()));
        } catch (RuntimeException exception) {
            throw new V3TransportInvocationException(
                    "V3 gRPC invocation failed: " + invocation.operationName(), true, exception);
        }
    }
}
