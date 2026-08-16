// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCalls;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.entrypoint.PropertyPresenceBinder;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterSource;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.transport.EntrypointTransport;
import vip.isass.framework.entrypoint.transport.EntrypointTransportException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EntrypointGrpcTransport implements EntrypointTransport, AutoCloseable {

    private final EntrypointGrpcProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public EntrypointGrpcTransport(EntrypointGrpcProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override public String name() { return "GRPC"; }

    @Override
    public boolean supports(ServiceDefinition service, OperationDefinition operation) {
        EntrypointGrpcProperties.Endpoint endpoint = properties.getServices().get(service.serviceName());
        return endpoint != null && endpoint.getHost() != null && endpoint.getPort() > 0
                && operation.parameters().stream().noneMatch(p -> p.source() == ParameterSource.FORM_FILE);
    }

    @Override
    public Object invoke(ServiceDefinition service, OperationDefinition operation, Object[] arguments) {
        if (!supports(service, operation)) {
            throw new EntrypointTransportException("未配置 gRPC 地址: " + service.serviceName(), true);
        }
        try {
            Object encoded = objectMapper.convertValue(arguments, Object.class);
            byte[] request = objectMapper.writeValueAsBytes(PropertyPresenceBinder.project(arguments, encoded));
            byte[] response = ClientCalls.blockingUnaryCall(channel(service.serviceName()),
                    EntrypointGrpcDescriptors.method(service, operation), CallOptions.DEFAULT, request);
            if (operation.returnType() == void.class || operation.returnType() == Void.class) return null;
            return objectMapper.readValue(response,
                    objectMapper.getTypeFactory().constructType(operation.returnType()));
        } catch (RuntimeException exception) {
            throw new EntrypointTransportException(
                    "gRPC 调用失败: " + service.key() + "#" + operation.operationName(), false, exception);
        }
    }

    private ManagedChannel channel(String serviceName) {
        return channels.computeIfAbsent(serviceName, ignored -> {
            EntrypointGrpcProperties.Endpoint endpoint = properties.getServices().get(serviceName);
            ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(endpoint.getHost(), endpoint.getPort());
            if (endpoint.isPlaintext()) builder.usePlaintext();
            return builder.build();
        });
    }

    @Override
    public void close() {
        channels.values().forEach(ManagedChannel::shutdown);
        channels.clear();
    }
}
