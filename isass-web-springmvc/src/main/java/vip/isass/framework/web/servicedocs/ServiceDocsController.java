// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.servicedocs;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class ServiceDocsController {

    private static final MediaType APPLICATION_JSON_UTF8 =
            new MediaType("application", "json", StandardCharsets.UTF_8);

    private final OpenApiDocumentAssembler assembler;
    private final ObjectProvider<OpenApiEnhancerSpi> enhancerProvider;
    private final Object cacheLock = new Object();
    private volatile String cachedJson;

    public ServiceDocsController(OpenApiDocumentAssembler assembler,
                                 ObjectProvider<OpenApiEnhancerSpi> enhancerProvider) {
        this.assembler = assembler;
        this.enhancerProvider = enhancerProvider;
    }

    @GetMapping({"/v3/api-docs", "/${spring.application.name}/v3/api-docs"})
    public ResponseEntity<String> openApi() {
        return ResponseEntity.ok().contentType(APPLICATION_JSON_UTF8)
                .cacheControl(CacheControl.noStore()).body(getOpenApiJson());
    }

    private String getOpenApiJson() {
        String snapshot = cachedJson;
        if (snapshot != null) return snapshot;
        synchronized (cacheLock) {
            if (cachedJson == null) {
                String assembled = assembler.assemble();
                OpenApiEnhancerSpi enhancer = enhancerProvider.getIfAvailable();
                cachedJson = enhancer == null ? assembled : enhancer.enhance(assembled);
            }
            return cachedJson;
        }
    }
}
