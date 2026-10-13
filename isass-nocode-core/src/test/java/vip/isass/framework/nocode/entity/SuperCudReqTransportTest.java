// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import vip.isass.framework.common.criteria.field.IIdCriteria;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.grpc.EntrypointGrpcProperties;
import vip.isass.framework.entrypoint.grpc.EntrypointGrpcServerAdapter;
import vip.isass.framework.entrypoint.grpc.EntrypointGrpcTransport;
import vip.isass.framework.entrypoint.http.EntrypointHttpServer;
import vip.isass.framework.entrypoint.registry.DefaultServiceDefinitionRegistry;
import vip.isass.framework.nocode.service.ICrudService;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies inherited NoCode primary-key generics through the real HTTP and gRPC binders.
 */
class SuperCudReqTransportTest {

    @Test
    void resolvesInheritedPrimaryKeyTypeBeforeHttpAndGrpcInvocation() throws Exception {
        RecordingService service = new RecordingService();
        var registry = new DefaultServiceDefinitionRegistry(List.of(service),
                List.of((type, method) -> ICrudService.class.isAssignableFrom(type)),
                List.of((definition, operation, arguments) -> assertTypedIds(
                        assertInstanceOf(SuperCudReq.class, arguments[0]))));
        var definition = registry.require("pk-test", "probe", "item");
        var operation = definition.operations().stream()
                .filter(candidate -> candidate.operationName().equals("superCud"))
                .findFirst().orElseThrow();
        ParameterizedType requestType = assertInstanceOf(ParameterizedType.class,
                operation.parameters().getFirst().javaType());
        assertEquals(List.of(Item.class, ItemCriteria.class, Long.class),
                List.of(requestType.getActualTypeArguments()));

        var controller = new EntrypointHttpServer(registry, registry);
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new JacksonJsonHttpMessageConverter((JsonMapper) JsonUtil.DEFAULT_INSTANCE))
                .build();
        mvc.perform(post("/pk-test/nocode/probe/item/superCud")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deleteIds\":[\"2087700000000000020\",\"9223372036854775807\"]}"))
                .andExpect(status().isOk());
        assertTypedIds(service.request);

        var adapter = new EntrypointGrpcServerAdapter(registry, registry);
        Server server = NettyServerBuilder.forPort(0)
                .addService(adapter.serviceDefinitions().getFirst()).build().start();
        try {
            var properties = new EntrypointGrpcProperties();
            var endpoint = new EntrypointGrpcProperties.Endpoint();
            endpoint.setHost("127.0.0.1");
            endpoint.setPort(server.getPort());
            endpoint.setPlaintext(true);
            properties.getServices().put("pk-test", endpoint);
            service.request = null;
            try (var grpc = new EntrypointGrpcTransport(properties)) {
                Object result = grpc.invoke(definition, operation, new Object[]{
                        Map.of("deleteIds", List.of("2087700000000000020", "9223372036854775807"))});
                assertEquals(new SuperCudResult(0, 0, 2), result);
                assertTypedIds(service.request);
            }
        } finally {
            server.shutdownNow().awaitTermination();
        }
    }

    private void assertTypedIds(SuperCudReq<?, ?, ?> request) {
        assertInstanceOf(Long.class, request.deleteIds().getFirst());
        assertEquals(List.of(2087700000000000020L, Long.MAX_VALUE), request.deleteIds());
    }

    @EntrypointInfo(serviceName = "pk-test", contextName = "probe", resourceName = "item")
    public interface ItemEntrypoint extends ICrudService<Item, ItemCriteria, Long> {
    }

    public static final class RecordingService
            implements ItemEntrypoint, ILocalCrudService<Item, ItemCriteria, Long> {
        private SuperCudReq<Item, ItemCriteria, Long> request;

        @Override
        public SuperCudResult superCud(SuperCudReq<Item, ItemCriteria, Long> request) {
            this.request = request;
            return new SuperCudResult(0, 0, request.deleteIds().size());
        }

        @Override
        public IRepository<Item, ItemCriteria> getRepository() {
            throw new UnsupportedOperationException("Transport test does not access a repository");
        }
    }

    public static final class Item implements IIdEntity<Long, Item> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    public static final class ItemCriteria extends FullTypeCriteria<Item, ItemCriteria>
            implements IIdCriteria<Long, Item, ItemCriteria> {
    }
}
