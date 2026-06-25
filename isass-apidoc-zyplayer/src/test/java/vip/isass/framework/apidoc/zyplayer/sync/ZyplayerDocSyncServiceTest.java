package vip.isass.framework.apidoc.zyplayer.sync;

import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.apidoc.zyplayer.ZyplayerServiceDescriptor;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerClientOperations;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPage;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPageContent;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpace;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpaceGroup;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerDocSyncServiceTest {

    @Test
    void createsSpaceAndInsertsMissingManagedPages() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务", "isass"),
                List.of(new ZyplayerSyncDocument("guide/token", "Token 使用说明", 2, "# Token")),
                new ZyplayerSyncOptions(false, true));

        assertThat(result.createdSpaces()).isEqualTo(1);
        assertThat(result.createdPages()).isEqualTo(1);
        assertThat(client.spaceGroups).extracting(ZyplayerSpaceGroup::groupName).containsExactly("isass");
        assertThat(client.spaces).extracting(ZyplayerSpace::name).containsExactly("附件微服务");
        assertThat(client.spaces).extracting(ZyplayerSpace::uuid)
                .singleElement()
                .asString()
                .matches("attachment-service@\\d{17}");
        assertThat(client.upsertedPages).singleElement().satisfies(payload -> {
            assertThat(payload.get("spaceId")).isEqualTo(1L);
            assertThat(payload.get("name")).isEqualTo("Token 使用说明");
            assertThat(payload.get("editorType")).isEqualTo(2);
            assertThat((String) payload.get("content")).contains("isass-doc-sync");
        });
        assertThat(client.releasedPages).containsExactly(1L);
    }

    @Test
    void createsFoldersAndInsertsPageUnderFolderPath() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(new ZyplayerSyncDocument("api/get/fileBrowse", "GET /fileBrowse 文件列表", 6,
                        "{\"method\":\"get\",\"apiUrl\":\"/attachment-service/fileBrowse\"}",
                        List.of("api接口", "FileBrowseController"))),
                new ZyplayerSyncOptions(false, true));

        assertThat(result.createdPages()).isEqualTo(1);
        assertThat(client.upsertedPages).hasSize(6);
        assertThat(client.upsertedPages.get(0)).containsEntry("name", "api接口")
                .containsEntry("editorType", 0)
                .containsEntry("parentId", 0L)
                .containsEntry("seqNo", 10);
        assertThat(client.upsertedPages.get(1)).containsEntry("name", "使用文档")
                .containsEntry("editorType", 0)
                .containsEntry("parentId", 0L)
                .containsEntry("seqNo", 20);
        assertThat(client.upsertedPages.get(2)).containsEntry("name", "设计文档")
                .containsEntry("editorType", 0)
                .containsEntry("parentId", 0L)
                .containsEntry("seqNo", 30);
        assertThat(client.upsertedPages.get(3)).containsEntry("name", "数据库文档")
                .containsEntry("editorType", 0)
                .containsEntry("parentId", 0L)
                .containsEntry("seqNo", 40);
        assertThat(client.upsertedPages.get(4)).containsEntry("name", "FileBrowseController")
                .containsEntry("editorType", 0)
                .containsEntry("parentId", 1L);
        assertThat(client.upsertedPages.get(5)).containsEntry("name", "GET /fileBrowse 文件列表")
                .containsEntry("editorType", 6)
                .containsEntry("parentId", 5L);
    }

    @Test
    void selectsLatestTimestampSpaceForSameApplicationName() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service@20260619010101001"));
        client.spaces.add(new ZyplayerSpace(19L, "附件微服务", 1, null, "attachment-service@20260620010101001"));
        client.spaces.add(new ZyplayerSpace(29L, "附件微服务", 1, null, "attachment-service"));
        client.spaces.add(new ZyplayerSpace(39L, "附件微服务", 1, null, "other-service@20260621010101"));
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务", "isass"),
                List.of(new ZyplayerSyncDocument("guide/token", "Token 使用说明", 2, "# Token")),
                new ZyplayerSyncOptions(false, false));

        assertThat(result.createdSpaces()).isZero();
        assertThat(client.upsertedPages).singleElement()
                .satisfies(payload -> assertThat(payload).containsEntry("spaceId", 19L));
    }

    @Test
    void reusesExistingManagedRootFolderWithZeroParentId() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service@20260619010101001"));
        client.pages.add(new ZyplayerPage(41L, 9L, "使用文档", 0L, 0, null, List.of()));
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(new ZyplayerSyncDocument("guide/token", "Token 使用说明", 2, "# Token",
                        List.of("使用文档"))),
                new ZyplayerSyncOptions(false, false));

        assertThat(result.createdPages()).isEqualTo(1);
        assertThat(client.upsertedPages).extracting(payload -> payload.get("name"))
                .containsExactly("api接口", "设计文档", "数据库文档", "Token 使用说明");
        assertThat(client.upsertedPages.get(3)).containsEntry("name", "Token 使用说明")
                .containsEntry("parentId", 41L);
    }

    @Test
    void skipsUnchangedPagesAndUpdatesChangedPages() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service:4.0.0"));
        client.pages.add(new ZyplayerPage(21L, 9L, "Token 使用说明", null, 2, null, List.of()));
        client.pageContents.put(21L, ZyplayerSyncMarker.prepend(
                new ZyplayerSyncMarker("attachment-service", "guide/token", "old-hash"), "# Old"));
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(new ZyplayerSyncDocument("guide/token", "Token 使用说明", 2, "# New")),
                new ZyplayerSyncOptions(false, false));

        assertThat(result.updatedPages()).isEqualTo(1);
        assertThat(result.skippedPages()).isZero();
        assertThat(client.upsertedPages).singleElement().satisfies(payload -> {
            assertThat(payload.get("id")).isEqualTo(21L);
            assertThat(payload).containsKey("editVersion");
            assertThat((String) payload.get("content")).contains("# New");
        });
        assertThat(client.releasedPages).isEmpty();
    }

    @Test
    void updatesChangedApiPagesUsingJsonMarker() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service:4.0.0"));
        client.pages.add(new ZyplayerPage(22L, 9L, "GET /fileBrowse 文件列表", null, 6, null, List.of()));
        client.pageContents.put(22L, """
                {"method":"get","apiUrl":"/attachment-service/fileBrowse","_isassSyncMarker":{"service":"attachment-service","id":"api/get/fileBrowse","hash":"old-hash"}}
                """);
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(new ZyplayerSyncDocument("api/get/fileBrowse", "GET /fileBrowse 文件列表", 6, """
                        {"method":"get","apiUrl":"/attachment-service/fileBrowse","description":"文件列表"}
                        """)),
                new ZyplayerSyncOptions(false, false));

        assertThat(result.updatedPages()).isEqualTo(1);
        assertThat(result.createdPages()).isZero();
        assertThat(client.upsertedPages).singleElement().satisfies(payload -> {
            assertThat(payload.get("id")).isEqualTo(22L);
            assertThat(payload).containsKey("editVersion");
            assertThat((String) payload.get("content")).contains("文件列表");
        });
    }

    @Test
    void deletesOnlyManagedPagesMissingLocallyWhenEnabled() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service:4.0.0"));
        client.pages.add(new ZyplayerPage(31L, 9L, "旧文档", null, 2, null, List.of()));
        client.pages.add(new ZyplayerPage(32L, 9L, "手工文档", null, 2, null, List.of()));
        client.pageContents.put(31L, ZyplayerSyncMarker.prepend(
                new ZyplayerSyncMarker("attachment-service", "guide/old", "old-hash"), "# Old"));
        client.pageContents.put(32L, "# Manual");
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(),
                new ZyplayerSyncOptions(true, false));

        assertThat(result.deletedPages()).isEqualTo(1);
        assertThat(client.deletedPages).containsExactly(31L);
    }

    @Test
    void deletesEmptyDuplicateFoldersUnderManagedRootsWhenDeleteMissingEnabled() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service@20260619010101001"));
        client.pages.add(new ZyplayerPage(41L, 9L, "api接口", 0L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(42L, 9L, "文件系统", 41L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(43L, 9L, "api接口", 0L, 0, null, List.of()));
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(new ZyplayerSyncDocument("api/get/fileSystem", "查询服务器文件列表", 6,
                        "{\"method\":\"get\",\"apiUrl\":\"/attachment-service/fileSystem\"}",
                        List.of("api接口", "文件系统"))),
                new ZyplayerSyncOptions(true, false));

        assertThat(result.deletedPages()).isEqualTo(1);
        assertThat(client.deletedPages).containsExactly(43L);
    }

    @Test
    void deletesDuplicateRootFolderAfterItsObsoleteChildrenBecomeEmpty() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service@20260619010101001"));
        client.pages.add(new ZyplayerPage(41L, 9L, "api接口", 0L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(42L, 9L, "old-controller", 41L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(43L, 9L, "api接口", 0L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(44L, 9L, "文件系统", 43L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(45L, 9L, "查询服务器文件列表", 44L, 6, null, List.of()));
        client.pageContents.put(45L, """
                {"method":"get","apiUrl":"/attachment-service/fileSystem","_isassSyncMarker":{"service":"attachment-service","id":"api/get/fileSystem","hash":"old-hash"}}
                """);
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(new ZyplayerSyncDocument("api/get/fileSystem", "查询服务器文件列表", 6,
                        "{\"method\":\"get\",\"apiUrl\":\"/attachment-service/fileSystem\"}",
                        List.of("api接口", "文件系统"))),
                new ZyplayerSyncOptions(true, false));

        assertThat(result.deletedPages()).isEqualTo(2);
        assertThat(client.deletedPages).containsExactly(42L, 41L);
    }

    @Test
    void deletesEmptyObsoleteFoldersUnderManagedRootsWhenDeleteMissingEnabled() {
        FakeZyplayerClient client = new FakeZyplayerClient();
        client.spaces.add(new ZyplayerSpace(9L, "附件微服务", 1, null, "attachment-service:4.0.0"));
        client.pages.add(new ZyplayerPage(41L, 9L, "api接口", 0L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(42L, 9L, "file-system-controller", 41L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(43L, 9L, "文件系统", 41L, 0, null, List.of()));
        client.pages.add(new ZyplayerPage(44L, 9L, "手工文件夹", null, 0, null, List.of()));
        ZyplayerDocSyncService service = new ZyplayerDocSyncService(client, new ObjectMapper());

        ZyplayerSyncResult result = service.sync(new ZyplayerServiceDescriptor(
                        "attachment-service", "附件微服务"),
                List.of(new ZyplayerSyncDocument("api/get/fileSystem", "查询服务器文件列表", 6,
                        "{\"method\":\"get\",\"apiUrl\":\"/attachment-service/fileSystem\"}",
                        List.of("api接口", "文件系统"))),
                new ZyplayerSyncOptions(true, false));

        assertThat(result.deletedPages()).isEqualTo(1);
        assertThat(client.deletedPages).containsExactly(42L);
    }

    static class FakeZyplayerClient implements ZyplayerClientOperations {

        final List<ZyplayerSpace> spaces = new ArrayList<>();

        final List<ZyplayerSpaceGroup> spaceGroups = new ArrayList<>();

        final List<ZyplayerPage> pages = new ArrayList<>();

        final Map<Long, String> pageContents = new LinkedHashMap<>();

        final Map<Long, ZyplayerPage> detailPages = new LinkedHashMap<>();

        final List<Map<String, Object>> upsertedPages = new ArrayList<>();

        final List<Long> releasedPages = new ArrayList<>();

        final List<Long> deletedPages = new ArrayList<>();

        int listSpacesCalls;

        long nextSpaceId = 1;

        long nextPageId = 1;

        @Override
        public List<ZyplayerSpace> listSpaces() {
            listSpacesCalls++;
            return spaces;
        }

        @Override
        public List<ZyplayerSpaceGroup> listSpaceGroups() {
            return spaceGroups;
        }

        @Override
        public ZyplayerSpaceGroup updateSpaceGroup(Map<String, Object> payload) {
            ZyplayerSpaceGroup group = new ZyplayerSpaceGroup((long) (spaceGroups.size() + 1),
                    (String) payload.get("groupName"), (Integer) payload.get("seqNo"));
            spaceGroups.add(group);
            return group;
        }

        @Override
        public ZyplayerSpace updateSpace(Map<String, Object> payload) {
            ZyplayerSpace space = new ZyplayerSpace(nextSpaceId++, (String) payload.get("name"), 1,
                    (String) payload.get("spaceExplain"), (String) payload.get("uuid"));
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
            return new ZyplayerPageContent(detailPages.getOrDefault(pageId, page), pageContents.get(pageId));
        }

        @Override
        public ZyplayerPage updatePage(Map<String, Object> payload) {
            upsertedPages.add(payload);
            Long id = (Long) payload.getOrDefault("id", nextPageId++);
            ZyplayerPage page = new ZyplayerPage(id, (Long) payload.get("spaceId"), (String) payload.get("name"),
                    (Long) payload.get("parentId"), (Integer) payload.get("editorType"),
                    (Long) payload.get("editVersion"), List.of());
            pages.removeIf(item -> item.id().equals(id));
            pages.add(page);
            pageContents.put(id, (String) payload.getOrDefault("content", ""));
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
