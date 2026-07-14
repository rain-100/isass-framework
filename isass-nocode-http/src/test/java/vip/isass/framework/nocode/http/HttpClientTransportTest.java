package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ParameterContract;
import vip.isass.framework.nocode.contract.ParameterSource;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.transport.Invocation;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpClientTransportTest {

    @Test
    void springHttpExchangeSupportsDynamicMethodAndUri() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NocodeHttpExchange exchange = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(builder.build())).build().createClient(NocodeHttpExchange.class);
        server.expect(requestTo("https://asset.example/asset-service/icon/items/9"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"name\":\"new icon\"}"))
                .andRespond(withSuccess("{\"data\":\"created\"}", MediaType.APPLICATION_JSON));

        assertEquals("created", exchange.exchange(
                HttpMethod.POST,
                URI.create("https://asset.example/asset-service/icon/items/9"),
                new org.springframework.util.LinkedMultiValueMap<>(),
                Map.of("name", "new icon")).path("data").asText());
        server.verify();
    }

    @Test
    void invokesOneDynamicHttpExchangeFromContractMetadata() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<HttpMethod> method = new AtomicReference<>();
        AtomicReference<URI> uri = new AtomicReference<>();
        AtomicReference<MultiValueMap<String, String>> query = new AtomicReference<>();
        AtomicReference<Object> body = new AtomicReference<>();
        NocodeHttpExchange exchange = (requestMethod, requestUri, requestQuery, requestBody) -> {
            method.set(requestMethod);
            uri.set(requestUri);
            query.set(requestQuery);
            body.set(requestBody);
            return objectMapper.readTree("{\"data\":\"created\"}");
        };
        OperationContract operation = new OperationContract(
                "create", vip.isass.framework.nocode.contract.HttpMethod.POST, "/items/{id}",
                101, false, List.of(
                new ParameterContract("id", Long.class.getName(), ParameterSource.PATH, true, "主键"),
                new ParameterContract("criteria", Map.class.getName(), ParameterSource.QUERY, false, "查询条件"),
                new ParameterContract("request", Map.class.getName(), ParameterSource.BODY, true, "请求体")
        ), String.class.getName(), "创建");
        ServiceContract contract = new ServiceContract(
                "asset-service", "icon", "example.IconService",
                "example.Icon", "example.IconCriteria", List.of(operation));
        HttpClientTransport transport = new HttpClientTransport(
                exchange,
                serviceName -> URI.create("https://asset.example"),
                new ContractRegistry(List.of(contract)),
                objectMapper);

        Object result = transport.invoke(new Invocation(
                "asset-service", "icon", "create",
                List.of(9L, Map.of("tag", "blue", "ids", List.of(1, 2)), Map.of("name", "new icon")),
                false));

        assertEquals("created", result);
        assertEquals(HttpMethod.POST, method.get());
        assertEquals(URI.create("https://asset.example/asset-service/icon/items/9"), uri.get());
        assertEquals(List.of("blue"), query.get().get("tag"));
        assertEquals(List.of("1", "2"), query.get().get("ids"));
        assertEquals(Map.of("name", "new icon"), body.get());
    }
}
