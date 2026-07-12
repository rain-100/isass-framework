package vip.isass.framework.nocode.grpc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;
import vip.isass.framework.nocode.v3.stream.V3FileStream;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class V3GrpcLocalInvocationHandler implements V3GrpcInvocationHandler {

    private final V3ServiceRegistry services;
    private final ObjectMapper objectMapper;

    public V3GrpcLocalInvocationHandler(V3ServiceRegistry services, ObjectMapper objectMapper) {
        this.services = services;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] invoke(
            V3ServiceContract serviceContract,
            V3OperationContract operation,
            byte[] request
    ) {
        try {
            return objectMapper.writeValueAsBytes(invokeResult(serviceContract, operation, request));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke V3 gRPC operation " + operation.name(), exception);
        }
    }

    @Override
    public V3FileStream invokeFile(
            V3ServiceContract serviceContract,
            V3OperationContract operation,
            byte[] request
    ) {
        try {
            Object result = invokeResult(serviceContract, operation, request);
            if (result instanceof V3FileStream fileStream) {
                return fileStream;
            }
            throw new IllegalStateException("V3 gRPC file operation did not return V3FileStream: " + operation.name());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke V3 gRPC file operation " + operation.name(), exception);
        }
    }

    private Object invokeResult(
            V3ServiceContract serviceContract,
            V3OperationContract operation,
            byte[] request
    ) throws ReflectiveOperationException {
        Object service = services.require(serviceContract.serviceName(), serviceContract.entityName());
        var method = java.util.Arrays.stream(service.getClass().getMethods())
                .filter(candidate -> candidate.getName().equals(operation.name()))
                .filter(candidate -> candidate.getParameterCount() == operation.parameters().size())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Service method is missing: " + operation.name()));
        try {
            JsonNode argumentsNode = objectMapper.readTree(request);
            List<Object> arguments = new ArrayList<>();
            for (int index = 0; index < operation.parameters().size(); index++) {
                var javaType = objectMapper.getTypeFactory().constructFromCanonical(
                        operation.parameters().get(index).javaType());
                arguments.add(objectMapper.convertValue(argumentsNode.get(index), javaType));
            }
            return method.invoke(service, arguments.toArray());
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("V3 gRPC operation failed: " + operation.name(), cause);
        }
    }
}
