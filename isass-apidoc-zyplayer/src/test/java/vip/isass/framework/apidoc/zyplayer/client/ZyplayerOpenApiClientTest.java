package vip.isass.framework.apidoc.zyplayer.client;

import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerOpenApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private CapturedRequest capturedRequest;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void postsSignedContentFormToZyplayerOpenApi() throws Exception {
        KeyPair keyPair = createKeyPair();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openApi/v1/space/list", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            Map<String, String> form = parseForm(new String(requestBody, StandardCharsets.UTF_8));
            capturedRequest = new CapturedRequest(form.get("key"), form.get("signature"), form.get("content"));
            byte[] response = """
                    {"errCode":200,"data":[{"id":7,"name":"附件微服务v4.0.0","uuid":"attachment-service:4.0.0"}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ZyplayerOpenApiClient client = new ZyplayerOpenApiClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-api-key",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                objectMapper);

        List<ZyplayerSpace> spaces = client.listSpaces();

        assertThat(spaces)
                .extracting(ZyplayerSpace::uuid)
                .containsExactly("attachment-service:4.0.0");
        assertThat(capturedRequest.key()).isEqualTo("test-api-key");
        assertThat(capturedRequest.content()).contains("\"salt\"");
        assertThat(verifySignature(capturedRequest.content(), capturedRequest.signature(), keyPair)).isTrue();
    }

    @Test
    void readsPageListFromPagedDataObject() throws Exception {
        KeyPair keyPair = createKeyPair();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openApi/v1/space/page/list", exchange -> {
                byte[] response = """
                        {"errCode":200,"data":{"total":1,"records":[{"id":12,"spaceId":7,"name":"Token 使用说明","editorType":2}]}}
                        """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ZyplayerOpenApiClient client = new ZyplayerOpenApiClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-api-key",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                objectMapper);

        List<ZyplayerPage> pages = client.listPages(7L);

        assertThat(pages)
                .extracting(ZyplayerPage::name)
                .containsExactly("Token 使用说明");
    }

    @Test
    void readsCollectionFromOnlyArrayFieldWhenDataObjectFieldNameDiffers() throws Exception {
        KeyPair keyPair = createKeyPair();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openApi/v1/space/page/list", exchange -> {
                byte[] response = """
                        {"errCode":200,"data":{"pageTree":[{"id":13,"spaceId":7,"name":"数据库文档","editorType":2}]}}
                        """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ZyplayerOpenApiClient client = new ZyplayerOpenApiClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-api-key",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                objectMapper);

        List<ZyplayerPage> pages = client.listPages(7L);

        assertThat(pages)
                .extracting(ZyplayerPage::name)
                .containsExactly("数据库文档");
    }

    @Test
    void readsEmptyObjectDataAsEmptyPageList() throws Exception {
        KeyPair keyPair = createKeyPair();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openApi/v1/space/page/list", exchange -> {
            byte[] response = """
                    {"errCode":200,"data":{}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ZyplayerOpenApiClient client = new ZyplayerOpenApiClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-api-key",
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                objectMapper);

        List<ZyplayerPage> pages = client.listPages(7L);

        assertThat(pages).isEmpty();
    }

    private KeyPair createKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private boolean verifySignature(String content, String signatureHex, KeyPair keyPair) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(keyPair.getPublic());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return signature.verify(hexToBytes(signatureHex));
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] nameAndValue = pair.split("=", 2);
            form.put(decode(nameAndValue[0]), nameAndValue.length > 1 ? decode(nameAndValue[1]) : "");
        }
        return form;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record CapturedRequest(String key, String signature, String content) {
    }
}
