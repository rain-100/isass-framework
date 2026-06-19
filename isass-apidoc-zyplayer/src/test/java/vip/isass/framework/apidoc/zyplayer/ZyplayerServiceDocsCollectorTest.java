package vip.isass.framework.apidoc.zyplayer;

import org.junit.jupiter.api.Test;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;
import vip.isass.framework.web.servicedocs.ServiceDoc;
import vip.isass.framework.web.servicedocs.ServiceDocsScanner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerServiceDocsCollectorTest {

    @Test
    void classifiesMarkdownDocumentsIntoDefaultFolders() {
        ZyplayerServiceDocsCollector collector = new ZyplayerServiceDocsCollector(new FakeServiceDocsScanner());

        List<ZyplayerSyncDocument> documents = collector.collect();

        assertThat(documents).extracting(ZyplayerSyncDocument::folderPath)
                .containsExactly(List.of("设计文档"), List.of("数据库文档"), List.of("使用文档"));
    }

    static class FakeServiceDocsScanner extends ServiceDocsScanner {

        FakeServiceDocsScanner() {
            super(null);
        }

        @Override
        public List<ServiceDoc> findAll() {
            return List.of(
                    new ServiceDoc("api/attachment-api", "接口文档", "api", "markdown", "service-docs/api/attachment-api.md"),
                    new ServiceDoc("database/attachment-db", "数据库文档", "database", "markdown", "service-docs/database/attachment-db.md"),
                    new ServiceDoc("guide/token", "Token 说明", "guide", "markdown", "service-docs/guide/token.md")
            );
        }

        @Override
        public String readContent(String docId) {
            return "# " + docId;
        }
    }
}
