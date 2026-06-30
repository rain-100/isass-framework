package vip.isass.framework.web.servicedocs;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Rain
 */
@RestController
public class ServiceDocsController {

    private static final String OPEN_API_RESOURCE = "classpath:/openapi3/openapi.json";

    private static final MediaType APPLICATION_JSON_UTF8 =
            new MediaType("application", "json", StandardCharsets.UTF_8);

    private final ResourceLoader resourceLoader;

    private final ObjectProvider<OpenApiEnhancerSpi> openApiEnhancerProvider;

    private final Object cacheLock = new Object();

    private volatile String cachedJson;

    public ServiceDocsController(
            ResourceLoader resourceLoader,
            ObjectProvider<OpenApiEnhancerSpi> openApiEnhancerProvider
    ) {
        this.resourceLoader = resourceLoader;
        this.openApiEnhancerProvider = openApiEnhancerProvider;
    }

    @GetMapping({"/v3/api-docs", "/${spring.application.name}/v3/api-docs"})
    public ResponseEntity<String> openApi() {
        return ResponseEntity.ok()
                .contentType(APPLICATION_JSON_UTF8)
                .body(getOpenApiJson());
    }

    private String getOpenApiJson() {
        String snapshot = cachedJson;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (cacheLock) {
            snapshot = cachedJson;
            if (snapshot == null) {
                String rawJson = readOpenApiJson();
                OpenApiEnhancerSpi enhancer = openApiEnhancerProvider.getIfAvailable();
                snapshot = enhancer == null ? rawJson : enhancer.enhance(rawJson);
                cachedJson = snapshot;
            }
            return snapshot;
        }
    }

    private String readOpenApiJson() {
        Resource resource = resourceLoader.getResource(OPEN_API_RESOURCE);
        if (!resource.isReadable()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "openapi doc not found: openapi3/openapi.json"
            );
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
