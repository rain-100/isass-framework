// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.stub.ServerCalls;
import tools.jackson.databind.JsonNode;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.entrypoint.PropertyPresenceBinder;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.EntrypointInvocationGateway;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds dynamic unary gRPC definitions for all entrypoints implemented in the current process. */
public final class EntrypointGrpcServerAdapter {

    private final List<ServerServiceDefinition> serviceDefinitions;

    public EntrypointGrpcServerAdapter(ServiceDefinitionRegistry registry,
                                       EntrypointInvocationGateway invocations) {
        this.serviceDefinitions = registry.all().stream()
                .filter(ServiceDefinition::localImplementation)
                .map(service -> build(service, invocations))
                .toList();
    }

    public List<ServerServiceDefinition> serviceDefinitions() {
        return serviceDefinitions;
    }

    private ServerServiceDefinition build(ServiceDefinition service,
                                          EntrypointInvocationGateway invocations) {
        Map<OperationDefinition, MethodDescriptor<byte[], byte[]>> methods = new LinkedHashMap<>();
        service.operations().forEach(operation -> methods.put(
                operation, EntrypointGrpcDescriptors.method(service, operation)));
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(
                new ServiceDescriptor(EntrypointGrpcDescriptors.serviceName(service),
                        new ArrayList<>(methods.values())));
        methods.forEach((operation, descriptor) -> builder.addMethod(descriptor,
                ServerCalls.asyncUnaryCall((byte[] bytes, io.grpc.stub.StreamObserver<byte[]> observer) -> {
                    try {
                        Object[] arguments = bindArguments(operation, bytes);
                        Object result = invocations.invoke(service.serviceName(), service.contextName(),
                                service.resourceName(), operation.operationName(), arguments);
                        observer.onNext(result == null ? new byte[0] : JsonUtil.writeValueAsBytes(result));
                        observer.onCompleted();
                    } catch (Throwable error) {
                        observer.onError(error);
                    }
                })));
        return builder.build();
    }

    private Object[] bindArguments(OperationDefinition operation, byte[] bytes) {
        JsonNode array;
        try {
            array = JsonUtil.readTree(bytes);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("gRPC 请求不是合法 JSON", exception);
        }
        if (!array.isArray() || array.size() != operation.parameters().size()) {
            throw new IllegalArgumentException("gRPC 参数数量不匹配: " + operation.operationName());
        }
        Object[] arguments = new Object[operation.parameters().size()];
        operation.parameters().forEach(parameter -> {
            JsonNode node = array.get(parameter.index());
            Object value = JsonUtil.convertValue(node, parameter.javaType());
            PropertyPresenceBinder.bind(value, JsonUtil.convertValue(node, Object.class));
            arguments[parameter.index()] = value;
        });
        return arguments;
    }
}
