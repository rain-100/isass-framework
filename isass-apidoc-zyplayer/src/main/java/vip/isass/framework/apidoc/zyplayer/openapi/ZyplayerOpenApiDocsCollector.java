package vip.isass.framework.apidoc.zyplayer.openapi;

import vip.isass.framework.apidoc.zyplayer.ZyplayerApidocProperties;
import vip.isass.framework.apidoc.zyplayer.ZyplayerText;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Rain
 */
public class ZyplayerOpenApiDocsCollector {

    private final ZyplayerApidocProperties properties;

    private final ZyplayerOpenApiDocumentConverter converter;

    private final Supplier<String> serverPortSupplier;

    private final Function<String, String> resourceReader;

    private final HttpClient httpClient;

    public ZyplayerOpenApiDocsCollector(
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter,
            Supplier<String> serverPortSupplier,
            Function<String, String> resourceReader) {
        this(properties, converter, serverPortSupplier, resourceReader, HttpClient.newHttpClient());
    }

    public ZyplayerOpenApiDocsCollector(
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter,
            Supplier<String> serverPortSupplier,
            Function<String, String> resourceReader,
            HttpClient httpClient) {
        this.properties = properties;
        this.converter = converter;
        this.serverPortSupplier = serverPortSupplier;
        this.resourceReader = resourceReader;
        this.httpClient = httpClient;
    }

    public List<ZyplayerSyncDocument> collect() {
        if (!properties.isOpenApiEnabled()) {
            return List.of();
        }
        String openApiJson = loadGeneratedOpenApiJson();
        if (!ZyplayerText.hasText(openApiJson)) {
            openApiJson = fetch(openApiDocsUrl());
        }
        return converter.convert(openApiJson, apiBaseUrl());
    }

    private String loadGeneratedOpenApiJson() {
        String sourcePath = properties.getOpenApiSourcePath();
        if (!ZyplayerText.hasText(sourcePath)) {
            return null;
        }
        try {
            return resourceReader.apply(sourcePath);
        } catch (Exception e) {
            return null;
        }
    }

    private String fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        } catch (Exception e) {
            throw new IllegalStateException("failed to fetch openapi docs: " + url, e);
        }
    }

    private String openApiDocsUrl() {
        return "http://127.0.0.1:" + serverPort() + openApiDocsPath();
    }

    private String apiBaseUrl() {
        if (ZyplayerText.hasText(properties.getApiBaseUrl())) {
            return properties.getApiBaseUrl();
        }
        return "http://127.0.0.1:" + serverPort();
    }

    private String openApiDocsPath() {
        return firstText(
                properties.getOpenApiDocsPath(),
                "/v3/api-docs");
    }

    private String serverPort() {
        return firstText(serverPortSupplier.get(), "8080");
    }

    private String firstText(String... values) {
        return ZyplayerText.firstText("", values);
    }
}
