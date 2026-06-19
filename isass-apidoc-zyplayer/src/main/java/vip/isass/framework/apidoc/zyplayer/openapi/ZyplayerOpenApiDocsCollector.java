package vip.isass.framework.apidoc.zyplayer.openapi;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import vip.isass.framework.apidoc.zyplayer.ZyplayerApidocProperties;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;

import java.util.List;

/**
 * @author Rain
 */
public class ZyplayerOpenApiDocsCollector {

    private final Environment environment;

    private final ZyplayerApidocProperties properties;

    private final ZyplayerOpenApiDocumentConverter converter;

    private final RestClient restClient;

    public ZyplayerOpenApiDocsCollector(
            Environment environment,
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter) {
        this(environment, properties, converter, RestClient.create());
    }

    public ZyplayerOpenApiDocsCollector(
            Environment environment,
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter,
            RestClient restClient) {
        this.environment = environment;
        this.properties = properties;
        this.converter = converter;
        this.restClient = restClient;
    }

    public List<ZyplayerSyncDocument> collect() {
        if (!properties.isOpenApiEnabled()) {
            return List.of();
        }
        String openApiJson = restClient.get()
                .uri(openApiDocsUrl())
                .retrieve()
                .body(String.class);
        return converter.convert(openApiJson, apiBaseUrl());
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
                environment.getProperty("springdoc.api-docs.path"),
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
