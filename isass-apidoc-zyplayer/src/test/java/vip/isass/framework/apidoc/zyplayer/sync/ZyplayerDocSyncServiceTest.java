package vip.isass.framework.apidoc.zyplayer.sync;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.apidoc.zyplayer.ZyplayerServiceDescriptor;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerClientOperations;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPage;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPageContent;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpace;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerDocSyncServiceTest {

    @Test
    void createsVersionedSpaceAndInsertsMissingManagedPages() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务", "4.0.0-SNAPSHOT"),
                List.of(new ZyplayerSyncDocument("guide/token", "Token 使用说明", 2, "# Token")),
                new ZyplayerSyncOptions(false, true));

        assertThat(result.createdSpaces()).isEqualTo(1);
        assertThat(result.createdPages()).isEqualTo(1);
        assertThat(client.spaces).extracting(ZyplayerSpace::name).containsExactly("附件微服务v4.0.0");
        assertThat(client.upsertedPages).singleElement().satisfies(payload -> {
            assertThat(payload.get("spaceId")).isEqualTo(1L);
            assertThat(payload.get("name")).isEqualTo("Token 使用说明");
            assertThat(payload.get("editorType")).isEqualTo(2);
            assertThat((String) payload.get("content")).contains("isass-doc-sync");
        });
        assertThat(client.releasedPages).containsExactly(1L);
    }

    @Test
    void skipsUnchangedPagesAndUpdatesChangedPages() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务v4.0.0", 1, null, "attachment-service:4.0.0", 0));
        client.pages.add(new ZyplayerPage(21L, 9L, "Token 使用说明", null, 2, 3, List.of()));
        client.pageContents.put(21L, ZyplayerSyncMarker.prepend(
                new ZyplayerSyncMarker("attachment-service", "guide/token", "old-hash"), "# Old"));
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务", "4.0.0-SNAPSHOT"),
                List.of(new ZyplayerSyncDocument("guide/token", "Token 使用说明", 2, "# New")),
                new ZyplayerSyncOptions(false, false));

        assertThat(result.updatedPages()).isEqualTo(1);
        assertThat(result.skippedPages()).isZero();
        assertThat(client.upsertedPages).singleElement().satisfies(payload -> {
            assertThat(payload.get("id")).isEqualTo(21L);
            assertThat(payload.get("editVersion")).isEqualTo(3);
            assertThat((String) payload.get("content")).contains("# New");
        });
        assertThat(client.releasedPages).isEmpty();
    }

    @Test
    void deletesOnlyManagedPagesMissingLocallyWhenEnabled() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务v4.0.0", 1, null, "attachment-service:4.0.0", 0));
        client.pages.add(new ZyplayerPage(31L, 9L, "旧文档", null, 2, 1, List.of()));
        client.pages.add(new ZyplayerPage(32L, 9L, "手工文档", null, 2, 1, List.of()));
        client.pageContents.put(31L, ZyplayerSyncMarker.prepend(
                new ZyplayerSyncMarker("attachment-service", "guide/old", "old-hash"), "# Old"));
        client.pageContents.put(32L, "# Manual");
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务", "4.0.0-SNAPSHOT"),
                List.of(),
                new ZyplayerSyncOptions(true, false));

        assertThat(result.deletedPages()).isEqualTo(1);
        assertThat(client.deletedPages).containsExactly(31L);
    }

    static class FakeZyplayerClient implements ZyplayerClientOperations {

        final List<ZyplayerSpace> spaces = new ArrayList<>();

        final List<ZyplayerPage> pages = new ArrayList<>();

        final Map<Long, String> pageContents = new LinkedHashMap<>();

        final List<Map<String, Object>> upsertedPages = new ArrayList<>();

        final List<Long> releasedPages = new ArrayList<>();

        final List<Long> deletedPages = new ArrayList<>();

        long nextSpaceId = 1;

        long nextPageId = 1;

        @Override
        public List<ZyplayerSpace> listSpaces() {
            return spaces;
        }

        @Override
        public ZyplayerSpace updateSpace(Map<String, Object> payload) {
            ZyplayerSpace space = new ZyplayerSpace(nextSpaceId++, (String) payload.get("name"), 1,
                    (String) payload.get("spaceExplain"), (String) payload.get("uuid"), 0);
            spaces.add(space);
            return space;
        }

        @Override
        public List<ZyplayerPage> listPages(Long spaceId) {
            return pages;
        }

        @Override
        public ZyplayerPageContent pageDetail(Long spaceId, Long pageId) {
            ZyplayerPage page = pages.stream().filter(item -> item.id().equals(pageId)).findFirst().orElseThrow();
            return new ZyplayerPageContent(page, pageContents.get(pageId));
        }

        @Override
        public ZyplayerPage updatePage(Map<String, Object> payload) {
            upsertedPages.add(payload);
            Long id = (Long) payload.getOrDefault("id", nextPageId++);
            ZyplayerPage page = new ZyplayerPage(id, (Long) payload.get("spaceId"), (String) payload.get("name"),
                    (Long) payload.get("parentId"), (Integer) payload.get("editorType"),
                    (Integer) payload.get("editVersion"), List.of());
            pages.removeIf(item -> item.id().equals(id));
            pages.add(page);
            pageContents.put(id, (String) payload.get("content"));
            return page;
        }

        @Override
        public void deletePage(Long spaceId, Long pageId) {
            deletedPages.add(pageId);
        }

        @Override
        public void releasePage(Long pageId) {
            releasedPages.add(pageId);
        }
    }
}
