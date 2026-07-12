package vip.isass.framework.nocode.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.HttpMethod;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.stream.FileStream;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GrpcServerAdapterTest {

    @Test
    void oneAdapterCreatesDistinctLogicalGrpcMethods() {
        ServiceContract contract = new ServiceContract(
                "attachment-service",
                "icon",
                "vip.isass.attachment.api.IIconService",
                "vip.isass.attachment.api.Icon",
                "vip.isass.attachment.api.IconCriteria",
                List.of(
                        operation("add"),
                        operation("findAvailableIcons")
                ));
        GrpcServerAdapter adapter = new GrpcServerAdapter(
                List.of(contract), (service, operation, request) -> request, new ObjectMapper());

        var definition = adapter.serviceDefinitions().getFirst();
        assertEquals("vip.isass.attachment.nocode.IconService", definition.getServiceDescriptor().getName());
        assertEquals(java.util.Set.of(
                        "vip.isass.attachment.nocode.IconService/Add",
                        "vip.isass.attachment.nocode.IconService/FindAvailableIcons"),
                definition.getMethods().stream()
                        .map(method -> method.getMethodDescriptor().getFullMethodName())
                        .collect(java.util.stream.Collectors.toSet()));
        assertSame(adapter, adapter);
    }

    @Test
    void clientTransportInvokesLogicalGrpcMethod() throws Exception {
        ServiceContract contract = new ServiceContract(
                "attachment-service", "icon", "vip.isass.attachment.api.IIconService",
                "sample.Icon", "sample.IconCriteria",
                List.of(new OperationContract(
                        "findName", HttpMethod.GET, "/name", 301, true,
                        List.of(), "java.lang.String", "查询名称")));
        ObjectMapper mapper = new ObjectMapper();
        GrpcServerAdapter adapter = new GrpcServerAdapter(
                List.of(contract),
                (service, operation, request) -> mapper.writeValueAsBytes("outline"), mapper);
        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(adapter.serviceDefinitions().getFirst()).build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        try {
            GrpcClientTransport client = new GrpcClientTransport(
                    channel, new ContractRegistry(List.of(contract)), mapper);
            Object result = client.invoke(new vip.isass.framework.nocode.transport.Invocation(
                    "attachment-service", "icon", "findName", List.of(), true));

            assertEquals("outline", result);
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    void clientTransportStreamsFileStreamWithoutLoadingAllBytes() throws Exception {
        ServiceContract contract = new ServiceContract(
                "attachment-service", "attachment", "vip.isass.attachment.api.IAttachmentService",
                "sample.Attachment", "sample.AttachmentCriteria",
                List.of(new OperationContract(
                        "download", HttpMethod.GET, "/download/{attachmentId}", 301, true,
                        List.of(new vip.isass.framework.nocode.contract.ParameterContract(
                                "attachmentId", String.class.getName(),
                                vip.isass.framework.nocode.contract.ParameterSource.PATH,
                                true, "附件 ID")),
                        FileStream.class.getName(), "下载附件")));
        ObjectMapper mapper = new ObjectMapper();
        GrpcInvocationHandler handler = new GrpcInvocationHandler() {
            @Override
            public byte[] invoke(ServiceContract service, OperationContract operation, byte[] request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public FileStream invokeFile(ServiceContract service, OperationContract operation, byte[] request) {
                return new FileStream("test.txt", "text/plain", 4L, true,
                        output -> output.write("data".getBytes()));
            }
        };
        GrpcServerAdapter adapter = new GrpcServerAdapter(List.of(contract), handler, mapper);
        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(adapter.serviceDefinitions().getFirst()).build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        try {
            GrpcClientTransport client = new GrpcClientTransport(
                    channel, new ContractRegistry(List.of(contract)), mapper);
            assertEquals(true, client.available(new vip.isass.framework.nocode.transport.Invocation(
                    "attachment-service", "attachment", "download", List.of("1"), true)));

            FileStream result = (FileStream) client.invoke(
                    new vip.isass.framework.nocode.transport.Invocation(
                            "attachment-service", "attachment", "download", List.of("1"), true));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            result.writeTo(output);

            assertEquals("test.txt", result.fileName());
            assertEquals("data", output.toString(java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    private OperationContract operation(String name) {
        return new OperationContract(
                name, HttpMethod.POST, "/action/" + name, 1, false,
                List.of(), byte[].class.getName(), name);
    }
}
