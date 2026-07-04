package vip.isass.framework.nocode.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3HttpMethod;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class V3GrpcServerAdapterTest {

    @Test
    void oneAdapterCreatesDistinctLogicalGrpcMethods() {
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service",
                "icon",
                "vip.isass.attachment.api.IV3IconService",
                "vip.isass.attachment.api.V3Icon",
                "vip.isass.attachment.api.V3IconCriteria",
                List.of(
                        operation("add"),
                        operation("findAvailableIcons")
                ));
        V3GrpcServerAdapter adapter = new V3GrpcServerAdapter(
                List.of(contract), (service, operation, request) -> request);

        var definition = adapter.serviceDefinitions().getFirst();
        assertEquals("vip.isass.attachment.v3.IconService", definition.getServiceDescriptor().getName());
        assertEquals(List.of(
                        "vip.isass.attachment.v3.IconService/Add",
                        "vip.isass.attachment.v3.IconService/FindAvailableIcons"),
                definition.getMethods().stream()
                        .map(method -> method.getMethodDescriptor().getFullMethodName())
                        .toList());
        assertSame(adapter, adapter);
    }

    @Test
    void clientTransportInvokesLogicalGrpcMethod() throws Exception {
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "icon", "vip.isass.attachment.api.IV3IconService",
                "sample.Icon", "sample.IconCriteria",
                List.of(new V3OperationContract(
                        "findName", V3HttpMethod.GET, "/name", 301, true,
                        List.of(), "java.lang.String", "查询名称")));
        ObjectMapper mapper = new ObjectMapper();
        V3GrpcServerAdapter adapter = new V3GrpcServerAdapter(
                List.of(contract),
                (service, operation, request) -> mapper.writeValueAsBytes("outline"));
        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(adapter.serviceDefinitions().getFirst()).build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        try {
            V3GrpcClientTransport client = new V3GrpcClientTransport(
                    channel, new V3ContractRegistry(List.of(contract)), mapper);
            Object result = client.invoke(new vip.isass.framework.nocode.v3.transport.V3Invocation(
                    "attachment-service", "icon", "findName", List.of(), true));

            assertEquals("outline", result);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    private V3OperationContract operation(String name) {
        return new V3OperationContract(
                name, V3HttpMethod.POST, "/action/" + name, 1, false,
                List.of(), byte[].class.getName(), name);
    }
}
