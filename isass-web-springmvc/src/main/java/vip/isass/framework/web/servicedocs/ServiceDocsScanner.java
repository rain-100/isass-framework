package vip.isass.framework.web.servicedocs;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Rain
 */
@Component
public class ServiceDocsScanner {

    private static final String SERVICE_DOCS_PREFIX = "service-docs/";

    private static final String SERVICE_DOCS_PATTERN = "classpath*:/service-docs/**/*.md";

    private final ResourcePatternResolver resourcePatternResolver;

    public ServiceDocsScanner(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<ServiceDoc> findAll() {
        try {
            return List.of(resourcePatternResolver.getResources(SERVICE_DOCS_PATTERN)).stream()
                    .filter(Resource::isReadable)
                    .map(this::toServiceDoc)
                    .sorted(Comparator.comparing(ServiceDoc::id))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String readContent(String docId) {
        String normalizedDocId = normalizeDocId(docId);
        return findResource(normalizedDocId).map(this::readString)
                .orElseThrow(() -> new NoSuchElementException("service doc not found: " + normalizedDocId));
    }

    private ServiceDoc toServiceDoc(Resource resource) {
        String path = extractPath(resource);
        String id = path.substring(0, path.length() - ".md".length());
        return new ServiceDoc(id, extractTitle(resource, id), extractType(id), "markdown", SERVICE_DOCS_PREFIX + path);
    }

    private java.util.Optional<Resource> findResource(String docId) {
        String expectedPath = docId + ".md";
        try {
            return List.of(resourcePatternResolver.getResources(SERVICE_DOCS_PATTERN)).stream()
                    .filter(Resource::isReadable)
                    .filter(resource -> extractPath(resource).equals(expectedPath))
                    .findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String extractPath(Resource resource) {
        try {
            String url = URLDecoder.decode(resource.getURL().toExternalForm(), StandardCharsets.UTF_8);
            int index = url.lastIndexOf(SERVICE_DOCS_PREFIX);
            if (index < 0) {
                throw new IllegalStateException("resource is not under service-docs: " + url);
            }
            return url.substring(index + SERVICE_DOCS_PREFIX.length());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String extractTitle(Resource resource, String id) {
        String content = readString(resource);
        return content.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("# "))
                .map(line -> line.substring(2).trim())
                .filter(title -> !title.isBlank())
                .findFirst()
                .orElseGet(() -> id.substring(id.lastIndexOf('/') + 1));
    }

    private String extractType(String id) {
        int separatorIndex = id.indexOf('/');
        return separatorIndex > 0 ? id.substring(0, separatorIndex) : "guide";
    }

    private String normalizeDocId(String docId) {
        String normalizedDocId = docId == null ? "" : docId.trim();
        while (normalizedDocId.startsWith("/")) {
            normalizedDocId = normalizedDocId.substring(1);
        }
        if (normalizedDocId.endsWith(".md")) {
            normalizedDocId = normalizedDocId.substring(0, normalizedDocId.length() - ".md".length());
        }
        if (normalizedDocId.isBlank() || normalizedDocId.contains("..") || normalizedDocId.contains("\\")) {
            throw new IllegalArgumentException("invalid service doc id: " + docId);
        }
        return normalizedDocId;
    }

    private String readString(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
