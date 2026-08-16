// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.PropertyPresenceAware;
import vip.isass.framework.entrypoint.annotation.BodyParam;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.annotation.QueryParam;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.registry.DefaultServiceDefinitionRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntrypointGrpcRoundTripTest {

    @Test
    void invokesLocalEntrypointThroughDynamicGrpcDefinition() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultServiceDefinitionRegistry registry = new DefaultServiceDefinitionRegistry(
                List.of(new CalculatorImpl()), List.of(), List.of());
        EntrypointGrpcServerAdapter adapter = new EntrypointGrpcServerAdapter(registry, registry, objectMapper);
        Server server = NettyServerBuilder.forPort(0)
                .addService(adapter.serviceDefinitions().getFirst())
                .build().start();
        EntrypointGrpcTransport transport = null;
        try {
            EntrypointGrpcProperties properties = new EntrypointGrpcProperties();
            EntrypointGrpcProperties.Endpoint endpoint = new EntrypointGrpcProperties.Endpoint();
            endpoint.setHost("127.0.0.1");
            endpoint.setPort(server.getPort());
            endpoint.setPlaintext(true);
            properties.getServices().put("sample-service", endpoint);
            transport = new EntrypointGrpcTransport(properties, objectMapper);
            var service = registry.require("sample-service", "demo", "calculator");
            var operation = service.operations().stream()
                    .filter(candidate -> candidate.operationName().equals("multiply"))
                    .findFirst().orElseThrow();

            Object result = transport.invoke(service, operation, new Object[]{6, 7});

            assertEquals(42, result);

            PresencePayload payload = new PresencePayload();
            payload.markPresentProperty("name");
            var inspect = service.operations().stream()
                    .filter(candidate -> candidate.operationName().equals("inspect"))
                    .findFirst().orElseThrow();
            assertEquals("name=true,description=false",
                    transport.invoke(service, inspect, new Object[]{payload}));
        } finally {
            if (transport != null) transport.close();
            server.shutdownNow().awaitTermination();
        }
    }

    @EntrypointInfo(serviceName = "sample-service", contextName = "demo", resourceName = "calculator")
    public interface Calculator extends IEntrypoint {
        @EntrypointOperation(operationName = "multiply", displayName = "乘法", httpMethod = HttpMethod.GET)
        Integer multiply(@QueryParam("left") Integer left, @QueryParam("right") Integer right);

        @EntrypointOperation(operationName = "inspect", displayName = "检查属性", httpMethod = HttpMethod.POST)
        String inspect(@BodyParam PresencePayload payload);
    }

    static final class CalculatorImpl implements Calculator {
        @Override
        public Integer multiply(Integer left, Integer right) {
            return left * right;
        }

        @Override
        public String inspect(PresencePayload payload) {
            return "name=" + payload.isPropertyPresent("name")
                    + ",description=" + payload.isPropertyPresent("description");
        }
    }

    static final class PresencePayload implements PropertyPresenceAware {
        private String name;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
