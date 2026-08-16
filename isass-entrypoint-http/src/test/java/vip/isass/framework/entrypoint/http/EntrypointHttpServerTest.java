// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.PropertyPresenceAware;
import vip.isass.framework.entrypoint.annotation.BodyParam;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.annotation.FormFieldParam;
import vip.isass.framework.entrypoint.annotation.FormFileParam;
import vip.isass.framework.entrypoint.annotation.QueryParam;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.registry.DefaultServiceDefinitionRegistry;
import vip.isass.framework.entrypoint.registry.EntrypointDefinitionParser;
import vip.isass.framework.entrypoint.stream.FileStream;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class EntrypointHttpServerTest {

    @Test
    void serializesObjectQueryAndIgnoresEmptyComplexCollections() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        ObjectMapper objectMapper = new ObjectMapper();
        var service = new EntrypointDefinitionParser(List.of()).parse(TestService.class, false);
        var operation = service.operations().stream()
                .filter(candidate -> candidate.operationName().equals("search"))
                .findFirst().orElseThrow();
        var transport = new EntrypointHttpTransport(builder.build(),
                ignored -> URI.create("http://localhost"), objectMapper, List.of());
        SearchQuery query = new SearchQuery();
        query.setTags(List.of());
        query.setWhereConditions(List.of(new QueryCondition("name", "EQUAL", "rain")));
        mockServer.expect(requestTo("http://localhost/sample-service/demo/test/search?name=rain"))
                .andRespond(withSuccess("{\"success\":true,\"data\":\"ok\"}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        assertEquals("ok", transport.invoke(service, operation, new Object[]{query}));
        mockServer.verify();
    }

    @Test
    void bindsRepeatedObjectQueryParametersAndUsesNocodeNamespace() throws Exception {
        TestServiceImpl implementation = new TestServiceImpl();
        DefaultServiceDefinitionRegistry registry = registry(implementation);
        EntrypointHttpServer server = new EntrypointHttpServer(registry, registry, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/sample-service/nocode/demo/test/search");
        LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("name", "rain");
        query.add("tags", "one");
        query.add("tags", "two");
        query.add("appId", "2084540564932513794");

        var response = server.invoke("sample-service", "demo", "test", "search", query, request,
                new MockHttpServletResponse());

        assertEquals("rain:one,two", assertInstanceOf(Resp.class, response).getData());
        assertThrows(IllegalArgumentException.class, () -> {
            MockHttpServletRequest wrong = new MockHttpServletRequest("GET",
                    "/sample-service/demo/test/search");
            server.invoke("sample-service", "demo", "test", "search", query, wrong,
                    new MockHttpServletResponse());
        });
    }

    @Test
    void bindsJsonFormFieldAndFileBytes() throws Exception {
        TestServiceImpl implementation = new TestServiceImpl();
        DefaultServiceDefinitionRegistry registry = registry(implementation);
        EntrypointHttpServer server = new EntrypointHttpServer(registry, registry, new ObjectMapper());
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/sample-service/nocode/demo/test/upload");
        request.addParameter("metadata", "{\"name\":\"avatar\"}");
        request.addFile(new MockMultipartFile("file", "avatar.bin",
                "application/octet-stream", new byte[]{1, 2, 3}));

        var response = server.invoke("sample-service", "demo", "test", "upload",
                new LinkedMultiValueMap<>(), request, new MockHttpServletResponse());

        assertEquals("avatar:3", assertInstanceOf(Resp.class, response).getData());
    }

    @Test
    void preservesExplicitBodyPropertyPresence() throws Exception {
        TestServiceImpl implementation = new TestServiceImpl();
        DefaultServiceDefinitionRegistry registry = registry(implementation);
        EntrypointHttpServer server = new EntrypointHttpServer(registry, registry, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/sample-service/nocode/demo/test/inspect");
        request.setContentType("application/json");
        request.setContent("{\"name\":null}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var response = server.invoke("sample-service", "demo", "test", "inspect",
                new LinkedMultiValueMap<>(), request, new MockHttpServletResponse());

        assertEquals("name=true,description=false", assertInstanceOf(Resp.class, response).getData());
    }

    @Test
    void supportsBusinessAndNocodeOperationsOnTheSameResource() throws Exception {
        TestServiceImpl implementation = new TestServiceImpl();
        DefaultServiceDefinitionRegistry registry = registry(implementation);
        EntrypointHttpServer server = new EntrypointHttpServer(registry, registry, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/sample-service/demo/test/business");

        var response = server.invoke("sample-service", "demo", "test", "business",
                new LinkedMultiValueMap<>(), request, new MockHttpServletResponse());

        assertEquals("business", assertInstanceOf(Resp.class, response).getData());
        assertThrows(IllegalArgumentException.class, () -> {
            MockHttpServletRequest wrong = new MockHttpServletRequest("GET",
                    "/sample-service/nocode/demo/test/business");
            server.invoke("sample-service", "demo", "test", "business",
                    new LinkedMultiValueMap<>(), wrong, new MockHttpServletResponse());
        });
    }

    @Test
    void streamsFileResponsesWithoutJsonWrapping() throws Exception {
        TestServiceImpl implementation = new TestServiceImpl();
        DefaultServiceDefinitionRegistry registry = registry(implementation);
        EntrypointHttpServer server = new EntrypointHttpServer(registry, registry, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/sample-service/nocode/demo/test/download");

        MockHttpServletResponse response = new MockHttpServletResponse();
        Object result = server.invoke("sample-service", "demo", "test", "download",
                new LinkedMultiValueMap<>(), request, response);

        assertNull(result);
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertEquals("4", response.getHeader(HttpHeaders.CONTENT_LENGTH));
        ContentDisposition disposition = ContentDisposition.parse(
                response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("inline", disposition.getType());
        assertEquals("测试.txt", disposition.getFilename());
        assertEquals("data", response.getContentAsString());
    }

    private DefaultServiceDefinitionRegistry registry(TestServiceImpl implementation) {
        return new DefaultServiceDefinitionRegistry(List.of(implementation),
                List.of((type, method) -> type == TestService.class && !method.getName().equals("business")),
                List.of());
    }

    @EntrypointInfo(serviceName = "sample-service", contextName = "demo", resourceName = "test")
    public interface TestService extends IEntrypoint {
        @EntrypointOperation(operationName = "search", displayName = "查询", httpMethod = HttpMethod.GET)
        String search(@QueryParam SearchQuery query);

        @EntrypointOperation(operationName = "upload", displayName = "上传", httpMethod = HttpMethod.POST)
        String upload(@FormFieldParam("metadata") Metadata metadata,
                      @FormFileParam("file") byte[] file);

        @EntrypointOperation(operationName = "inspect", displayName = "检查属性", httpMethod = HttpMethod.POST)
        String inspect(@BodyParam PresencePayload payload);

        @EntrypointOperation(operationName = "business", displayName = "业务操作", httpMethod = HttpMethod.GET)
        String business();

        @EntrypointOperation(operationName = "download", displayName = "下载", httpMethod = HttpMethod.GET)
        FileStream download();
    }

    static final class TestServiceImpl implements TestService {
        @Override
        public String search(SearchQuery query) {
            return query.getName() + ":" + String.join(",", query.getTags());
        }

        @Override
        public String upload(Metadata metadata, byte[] file) {
            return metadata.name() + ":" + file.length;
        }

        @Override
        public String inspect(PresencePayload payload) {
            return "name=" + payload.isPropertyPresent("name")
                    + ",description=" + payload.isPropertyPresent("description");
        }

        @Override
        public String business() {
            return "business";
        }

        @Override
        public FileStream download() {
            return new FileStream("测试.txt", "text/plain", 4L, false,
                    output -> output.write("data".getBytes(StandardCharsets.UTF_8)));
        }
    }

    static final class SearchQuery {
        private String name;
        private List<String> tags;
        private List<QueryCondition> whereConditions;

        @java.beans.Transient
        public String getName() { return name; }
        public SearchQuery setName(String name) { this.name = name; return this; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public List<QueryCondition> getWhereConditions() { return whereConditions; }
        public void setWhereConditions(List<QueryCondition> whereConditions) { this.whereConditions = whereConditions; }
    }

    record QueryCondition(String propertyName, String condition, Object value) {
    }

    record Metadata(String name) {
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
