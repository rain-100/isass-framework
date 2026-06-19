package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
