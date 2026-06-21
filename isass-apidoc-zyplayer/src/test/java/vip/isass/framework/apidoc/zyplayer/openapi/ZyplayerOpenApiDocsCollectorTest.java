package vip.isass.framework.apidoc.zyplayer.openapi;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
        ZyplayerApidocProperties properties = new ZyplayerApidocProperties();
        properties.setOpenApiDocsPath("/attachment-service/v3/api-docs");

        ZyplayerOpenApiDocsCollector collector = new ZyplayerOpenApiDocsCollector(
                properties,
                new ZyplayerOpenApiDocumentConverter(new ObjectMapper()),
                () -> String.valueOf(server.getAddress().getPort()),
                location -> null);

        List<ZyplayerSyncDocument> documents = collector.collect();

        assertThat(documents).singleElement().satisfies(document -> {
            assertThat(document.id()).isEqualTo("api/get/attachment-service/ping");
            assertThat(document.title()).isEqualTo("Ping");
            assertThat(document.content()).contains("http://127.0.0.1:" + server.getAddress().getPort() + "/attachment-service/ping");
        });
    }

    @Test
    void prefersGeneratedServiceDocsOpenApiWhenAvailable() {
        ZyplayerApidocProperties properties = new ZyplayerApidocProperties();
        ZyplayerOpenApiDocsCollector collector = new ZyplayerOpenApiDocsCollector(
                properties,
                new ZyplayerOpenApiDocumentConverter(new ObjectMapper()),
                () -> "20320",
                location -> """
                        {
                          "openapi": "3.1.0",
                          "paths": {
                            "/attachment-service/fileSystem": {
                              "get": {
                                "summary": "查询服务器文件列表",
                                "tags": ["文件系统"],
                                "parameters": [
                                  {"name": "path", "in": "query", "required": false, "description": "服务器目录路径", "schema": {"type": "string"}}
                                ],
                                "responses": {"200": {"description": "OK"}}
                              }
                            }
                          }
                        }
                        """);

        List<ZyplayerSyncDocument> documents = collector.collect();

        assertThat(documents).singleElement().satisfies(document -> {
            assertThat(document.title()).isEqualTo("查询服务器文件列表");
            assertThat(document.folderPath()).containsExactly("api接口", "文件系统");
            assertThat(document.content()).contains("服务器目录路径");
        });
    }

    @Test
    void skipsCollectionWhenDisabled() {
        ZyplayerApidocProperties properties = new ZyplayerApidocProperties();
        properties.setOpenApiEnabled(false);

        ZyplayerOpenApiDocsCollector collector = new ZyplayerOpenApiDocsCollector(
                properties,
                new ZyplayerOpenApiDocumentConverter(new ObjectMapper()),
                () -> "8080",
                location -> null);

        assertThat(collector.collect()).isEmpty();
    }
}
