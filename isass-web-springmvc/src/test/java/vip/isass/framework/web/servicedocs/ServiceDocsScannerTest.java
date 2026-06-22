package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDocsScannerTest {

    @Test
    void scansMarkdownFilesUnderServiceDocs() {
        ServiceDocsScanner scanner = new ServiceDocsScanner(new PathMatchingResourcePatternResolver());

        List<ServiceDoc> docs = scanner.findAll();

        assertThat(docs)
                .extracting(ServiceDoc::id)
                .contains("token", "database/attachment-db");
        assertThat(docs)
                .filteredOn(doc -> doc.id().equals("database/attachment-db"))
                .singleElement()
                .satisfies(doc -> {
                    assertThat(doc.title()).isEqualTo("Attachment Database");
                    assertThat(doc.type()).isEqualTo("database");
                    assertThat(doc.format()).isEqualTo("markdown");
                });
    }

    @Test
    void readsMarkdownContentById() {
        ServiceDocsScanner scanner = new ServiceDocsScanner(new PathMatchingResourcePatternResolver());

        String content = scanner.readContent("database/attachment-db");

        assertThat(content).contains("# Attachment Database");
    }

    @Test
    void normalizesServiceDocIds() {
        assertThat(ServiceDocsPaths.normalizeDocId("/guide/token.md")).isEqualTo("guide/token");
        assertThat(ServiceDocsPaths.normalizeDocId(" database/attachment-db ")).isEqualTo("database/attachment-db");
    }

    @Test
    void rejectsInvalidServiceDocIds() {
        assertThatThrownBy(() -> ServiceDocsPaths.normalizeDocId("../application.yml"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServiceDocsPaths.normalizeDocId("bad\\path"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServiceDocsPaths.normalizeDocId(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractsServiceDocsPathFromUrl() {
        assertThat(ServiceDocsPaths.resourcePathFromUrl("file:/app/classes/service-docs/guide/token.md"))
                .isEqualTo("guide/token.md");
        assertThat(ServiceDocsPaths.resourcePathFromUrl("file:/tmp/service-docs/cache/classes/service-docs/guide/token.md"))
                .isEqualTo("guide/token.md");
    }
}
