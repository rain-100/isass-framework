package vip.isass.framework.apidoc.zyplayer.openapi;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import vip.isass.framework.apidoc.zyplayer.ZyplayerApidocProperties;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerOpenApiDocsCollectorTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesLocalOpenApiDocsAndConvertsOperations() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/attachment-service/v3/api-docs", exchange -> {
            byte[] response = """
                    {"openapi":"3.1.0","paths":{"/attachment-service/ping":{"get":{"summary":"Ping","responses":{"200":{"description":"OK"}}}}}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("local.server.port", String.valueOf(server.getAddress().getPort()))
                .withProperty("springdoc.api-docs.path", "/attachment-service/v3/api-docs");
        ZyplayerApidocProperties properties = new ZyplayerApidocProperties();

        ZyplayerOpenApiDocsCollector collector = new ZyplayerOpenApiDocsCollector(
                environment, properties, new ZyplayerOpenApiDocumentConverter(new ObjectMapper()));

        List<ZyplayerSyncDocument> documents = collector.collect();

        assertThat(documents).singleElement().satisfies(document -> {
            assertThat(document.id()).isEqualTo("api/get/attachment-service/ping");
            assertThat(document.title()).isEqualTo("GET /attachment-service/ping Ping");
            assertThat(document.content()).contains("http://127.0.0.1:" + server.getAddress().getPort() + "/attachment-service/ping");
        });
    }

    @Test
    void skipsCollectionWhenDisabled() {
        MockEnvironment environment = new MockEnvironment();
        ZyplayerApidocProperties properties = new ZyplayerApidocProperties();
        properties.setOpenApiEnabled(false);

        ZyplayerOpenApiDocsCollector collector = new ZyplayerOpenApiDocsCollector(
                environment, properties, new ZyplayerOpenApiDocumentConverter(new ObjectMapper()));

        assertThat(collector.collect()).isEmpty();
    }
}
