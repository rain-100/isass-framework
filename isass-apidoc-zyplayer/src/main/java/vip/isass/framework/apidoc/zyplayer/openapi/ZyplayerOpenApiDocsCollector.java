package vip.isass.framework.apidoc.zyplayer.openapi;

import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import vip.isass.framework.apidoc.zyplayer.ZyplayerApidocProperties;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Rain
 */
public class ZyplayerOpenApiDocsCollector {

    private final Environment environment;

    private final ZyplayerApidocProperties properties;

    private final ZyplayerOpenApiDocumentConverter converter;

    private final RestClient restClient;

    private final ResourceLoader resourceLoader;

    public ZyplayerOpenApiDocsCollector(
            Environment environment,
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter) {
        this(environment, properties, converter, RestClient.create(), new DefaultResourceLoader());
    }

    public ZyplayerOpenApiDocsCollector(
            Environment environment,
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter,
            RestClient restClient) {
        this(environment, properties, converter, restClient, new DefaultResourceLoader());
    }

    public ZyplayerOpenApiDocsCollector(
            Environment environment,
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter,
            ResourceLoader resourceLoader) {
        this(environment, properties, converter, RestClient.create(), resourceLoader);
    }

    public ZyplayerOpenApiDocsCollector(
            Environment environment,
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter,
            RestClient restClient,
            ResourceLoader resourceLoader) {
        this.environment = environment;
        this.properties = properties;
        this.converter = converter;
        this.restClient = restClient;
        this.resourceLoader = resourceLoader;
    }

    public List<ZyplayerSyncDocument> collect() {
        if (!properties.isOpenApiEnabled()) {
            return List.of();
        }
        String openApiJson = loadGeneratedOpenApiJson();
        if (!StringUtils.hasText(openApiJson)) {
            openApiJson = restClient.get()
                    .uri(openApiDocsUrl())
                    .retrieve()
                    .body(String.class);
        }
        return converter.convert(openApiJson, apiBaseUrl());
    }

    private String loadGeneratedOpenApiJson() {
        String sourcePath = properties.getOpenApiSourcePath();
        if (!StringUtils.hasText(sourcePath)) {
            return null;
        }
        try {
            Resource resource = resourceLoader.getResource(sourcePath);
            if (!resource.exists()) {
                return null;
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private String openApiDocsUrl() {
        return "http://127.0.0.1:" + serverPort() + openApiDocsPath();
    }

    private String apiBaseUrl() {
        if (StringUtils.hasText(properties.getApiBaseUrl())) {
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
        return firstText(
                environment.getProperty("local.server.port"),
                environment.getProperty("server.port"),
                "8080");
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
