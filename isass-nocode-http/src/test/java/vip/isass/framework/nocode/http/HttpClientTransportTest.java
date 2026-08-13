// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.mock.env.MockEnvironment;
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
import vip.isass.framework.nocode.transport.TransportInvocationException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpClientTransportTest {

    @Test
    void explicitUrlTakesPrecedenceOverServiceDiscovery() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.http.endpoints.bsp-service.url", "http://127.0.0.1:31010");
        LoadBalancerClient loadBalancerClient = mock(LoadBalancerClient.class);
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("loadBalancerClient", loadBalancerClient);

        HttpEndpointResolver resolver = new DefaultHttpEndpointResolver(new HttpEndpointProperties(environment), beans);

        assertEquals(URI.create("http://127.0.0.1:31010"), resolver.resolve("bsp-service"));
        verifyNoInteractions(loadBalancerClient);
    }

    @Test
    void resolvesServiceInstanceWhenExplicitUrlIsNotConfigured() {
        LoadBalancerClient loadBalancerClient = mock(LoadBalancerClient.class);
        ServiceInstance instance = mock(ServiceInstance.class);
        when(loadBalancerClient.choose("bsp-service")).thenReturn(instance);
        when(instance.getUri()).thenReturn(URI.create("http://10.0.0.8:31010"));
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("loadBalancerClient", loadBalancerClient);

        HttpEndpointResolver resolver = new DefaultHttpEndpointResolver(new HttpEndpointProperties(new MockEnvironment()), beans);

        assertEquals(URI.create("http://10.0.0.8:31010"), resolver.resolve("bsp-service"));
    }

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
    void serializesSingleStringBodyAsJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NocodeHttpExchange exchange = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(builder.build())).build().createClient(NocodeHttpExchange.class);
        OperationContract operation = new OperationContract(
                "authenticate", vip.isass.framework.nocode.contract.HttpMethod.POST, "/authenticate",
                1, false, List.of(new ParameterContract(
                "apiKey", String.class.getName(), ParameterSource.BODY, true, "API Key")),
                String.class.getName(), "认证");
        ServiceContract contract = new ServiceContract(
                "bsp-service", "bspApiKeyAuthentication", "example.ApiKeyAuthenticationService",
                String.class.getName(), String.class.getName(), List.of(operation));
        HttpClientTransport transport = new HttpClientTransport(
                exchange,
                service -> URI.create("https://bsp.example"),
                new ContractRegistry(List.of(contract)),
                objectMapper);
        server.expect(requestTo("https://bsp.example/bsp-service/bspApiKeyAuthentication/authenticate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string("\"isass_sk_example\""))
                .andRespond(withSuccess("{\"data\":\"authenticated\"}", MediaType.APPLICATION_JSON));

        Object result = transport.invoke(new Invocation(
                "bsp-service", "bspApiKeyAuthentication", "authenticate",
                List.of("isass_sk_example"), false));

        assertEquals("authenticated", result);
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
                service -> URI.create("https://asset.example"),
                new ContractRegistry(List.of(contract)),
                objectMapper);

        Object result = transport.invoke(new Invocation(
                "asset-service", "icon", "create",
                List.of(9L, Map.of(
                        "tag", "blue",
                        "ids", List.of(1, 2),
                        "whereConditions", List.of(Map.of("propertyName", "tag", "value", "blue"))),
                        Map.of("name", "new icon")),
                false));

        assertEquals("created", result);
        assertEquals(HttpMethod.POST, method.get());
        assertEquals(URI.create("https://asset.example/asset-service/icon/items/9"), uri.get());
        assertEquals(List.of("blue"), query.get().get("tag"));
        assertEquals(List.of("1", "2"), query.get().get("ids"));
        assertNull(query.get().get("whereConditions"));
        assertEquals(Map.of("name", "new icon"), body.get());
    }

    @Test
    void rejectsRemoteBusinessFailureInsteadOfReturningNullData() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        OperationContract operation = new OperationContract(
                "find", vip.isass.framework.nocode.contract.HttpMethod.GET, "/criteria",
                1, true, List.of(), String.class.getName(), "查询");
        ServiceContract contract = new ServiceContract(
                "asset-service", "icon", "example.IconService",
                "example.Icon", "example.IconCriteria", List.of(operation));
        HttpClientTransport transport = new HttpClientTransport(
                (method, uri, query, body) -> objectMapper.readTree(
                        "{\"success\":false,\"message\":\"criteria invalid\",\"data\":null}"),
                service -> URI.create("https://asset.example"),
                new ContractRegistry(List.of(contract)), objectMapper);

        TransportInvocationException exception = assertThrows(TransportInvocationException.class,
                () -> transport.invoke(new Invocation("asset-service", "icon", "find", List.of(), true)));

        assertEquals("HTTP invocation failed: asset-service/icon#find: criteria invalid", exception.getMessage());
    }
}
