package vip.isass.framework.web.servicedocs;

final class ServiceDocsPaths {

    static final String SERVICE_DOCS_PREFIX = "service-docs/";

    static final String SERVICE_DOCS_PATTERN = "classpath*:/service-docs/**/*.md";

    static final String OPEN_API_DOC_PATH = "service-docs/api/openapi.json";

    private ServiceDocsPaths() {
    }

    static String normalizeDocId(String docId) {
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

    static String resourcePathFromUrl(String url) {
        String normalizedUrl = url.replace('\\', '/');
        int index = normalizedUrl.lastIndexOf(SERVICE_DOCS_PREFIX);
        if (index < 0) {
            throw new IllegalStateException("resource is not under service-docs: " + url);
        }
        return normalizedUrl.substring(index + SERVICE_DOCS_PREFIX.length());
    }
}
