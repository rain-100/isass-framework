// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.annotation.QueryParam;
import vip.isass.framework.entrypoint.grpc.EntrypointGrpcProperties;
import vip.isass.framework.entrypoint.grpc.EntrypointGrpcServerAdapter;
import vip.isass.framework.entrypoint.grpc.EntrypointGrpcTransport;
import vip.isass.framework.entrypoint.http.EntrypointHttpServer;
import vip.isass.framework.entrypoint.http.EntrypointHttpTransport;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.registry.DefaultServiceDefinitionRegistry;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.nocode.query.CriteriaQueryParamConverter;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 防止跨 HTTP QUERY / gRPC 往返时丢失嵌套类型、空 Criteria、特殊字符或重复登记条件。
 * HTTP 使用真实 MVC 绑定链，gRPC 使用随机本机端口；不访问业务数据库、不改变鉴权配置。
 */
class CriteriaTransportTest {
    @Test
    void preservesTheSameCriteriaTreeAcrossHttpAndGrpc() throws Exception {
        CriteriaEntityTypes.register(TransportItemCriteria.class);
        var registry = new DefaultServiceDefinitionRegistry(List.of(new EchoService()), List.of(), List.of());
        var definition = registry.require("criteria-test", "probe", "echo");
        var operation = definition.operations().getFirst();
        TransportItemCriteria child = new TransportItemCriteria().equals("name", "中文 &+%,=?");
        TransportItemCriteria query = new TransportItemCriteria()
                .leftJoin(TransportItem.class, TransportItem::getId, TransportItem::getId)
                .exists(child, TransportItem::getId, TransportItem::getId)
                .in(TransportItem::getId, child, TransportItem::getId)
                .loadRelated("items", child)
                .setFromCriteria(new TransportItemCriteria().setId(7L));
        var expected = JsonUtil.valueToTree(query);

        var converter = new CriteriaQueryParamConverter();
        var controller = new EntrypointHttpServer(registry, registry, List.of(converter));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new JacksonJsonHttpMessageConverter((JsonMapper) JsonUtil.DEFAULT_INSTANCE))
                .build();
        var client = RestClient.builder().requestFactory(new MockMvcClientHttpRequestFactory(mvc)).build();
        var http = new EntrypointHttpTransport(client, ignored -> URI.create("http://localhost"),
                List.of(), List.of(converter));
        Object httpResult = http.invoke(definition, operation, new Object[]{query});
        assertInstanceOf(TransportItemCriteria.class, httpResult);
        assertEquals(expected, JsonUtil.valueToTree(httpResult));

        var adapter = new EntrypointGrpcServerAdapter(registry, registry);
        Server server = NettyServerBuilder.forPort(0).addService(adapter.serviceDefinitions().getFirst()).build().start();
        EntrypointGrpcTransport grpc = null;
        try {
            var properties = new EntrypointGrpcProperties();
            var endpoint = new EntrypointGrpcProperties.Endpoint();
            endpoint.setHost("127.0.0.1");
            endpoint.setPort(server.getPort());
            endpoint.setPlaintext(true);
            properties.getServices().put("criteria-test", endpoint);
            grpc = new EntrypointGrpcTransport(properties);
            Object grpcResult = grpc.invoke(definition, operation, new Object[]{query});
            assertInstanceOf(TransportItemCriteria.class, grpcResult);
            assertEquals(expected, JsonUtil.valueToTree(grpcResult));
        } finally {
            if (grpc != null) {
                grpc.close();
            }
            server.shutdownNow().awaitTermination();
        }
    }

    @EntrypointInfo(serviceName = "criteria-test", contextName = "probe", resourceName = "echo")
    public interface Echo extends IEntrypoint {
        @EntrypointOperation(operationName = "echo", displayName = "条件往返", httpMethod = HttpMethod.GET)
        TransportItemCriteria echo(@QueryParam TransportItemCriteria criteria);
    }

    public static final class EchoService implements Echo {
        @Override
        public TransportItemCriteria echo(TransportItemCriteria criteria) {
            return criteria.copy();
        }
    }

    @Getter
    @Setter
    public static class TransportItem implements IIdEntity<Long, TransportItem> {
        private Long id;
        private String name;

        @Override
        public TransportItem randomEntity() {
            return this;
        }
    }

    public static class TransportItemCriteria extends FullTypeCriteria<TransportItem, TransportItemCriteria> {
        public TransportItemCriteria setId(Long id) {
            return equals("id", id);
        }
    }
}
