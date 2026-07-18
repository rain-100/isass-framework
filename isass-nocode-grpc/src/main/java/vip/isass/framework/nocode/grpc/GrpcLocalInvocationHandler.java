package vip.isass.framework.nocode.grpc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.stream.FileStream;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class GrpcLocalInvocationHandler implements GrpcInvocationHandler {

    private final ServiceRegistry services;
    private final ObjectMapper objectMapper;

    public GrpcLocalInvocationHandler(ServiceRegistry services, ObjectMapper objectMapper) {
        this.services = services;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] invoke(
            ServiceContract serviceContract,
            OperationContract operation,
            byte[] request
    ) {
        try {
            return objectMapper.writeValueAsBytes(invokeResult(serviceContract, operation, request));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke  gRPC operation " + operation.name(), exception);
        }
    }

    @Override
    public FileStream invokeFile(
            ServiceContract serviceContract,
            OperationContract operation,
            byte[] request
    ) {
        try {
            Object result = invokeResult(serviceContract, operation, request);
            if (result instanceof FileStream fileStream) {
                return fileStream;
            }
            throw new IllegalStateException(" gRPC file operation did not return FileStream: " + operation.name());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot invoke  gRPC file operation " + operation.name(), exception);
        }
    }

    private Object invokeResult(
            ServiceContract serviceContract,
            OperationContract operation,
            byte[] request
    ) throws ReflectiveOperationException {
        Object service = services.require(serviceContract.service(), serviceContract.entity());
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
            throw new IllegalStateException(" gRPC operation failed: " + operation.name(), cause);
        }
    }
}
