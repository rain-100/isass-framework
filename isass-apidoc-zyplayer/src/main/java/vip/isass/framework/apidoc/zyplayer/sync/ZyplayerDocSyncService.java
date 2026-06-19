package vip.isass.framework.apidoc.zyplayer.sync;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.isass.framework.apidoc.zyplayer.ZyplayerServiceDescriptor;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerClientOperations;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerOpenApiException;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPage;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPageContent;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpace;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpaceGroup;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpaceVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
public class ZyplayerDocSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZyplayerDocSyncService.class);

    private final ZyplayerClientOperations client;

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    public ZyplayerDocSyncService(ZyplayerClientOperations client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public ZyplayerSyncResult sync(
            ZyplayerServiceDescriptor descriptor,
            List<ZyplayerSyncDocument> documents,
            ZyplayerSyncOptions options) {
        SpaceState spaceState = findOrCreateSpace(descriptor);
        List<PageState> remotePages = loadPageStates(spaceState.space().id());
        Map<FolderKey, ZyplayerPage> foldersByKey = remotePages.stream()
                .map(PageState::page)
                .filter(page -> Objects.equals(page.editorType(), ZyplayerEditorTypes.FOLDER))
                .collect(Collectors.toMap(page -> new FolderKey(page.parentId(), page.name()),
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, PageState> managedPagesById = remotePages.stream()
                .filter(page -> page.marker() != null)
                .filter(page -> descriptor.applicationName().equals(page.marker().service()))
                .collect(Collectors.toMap(page -> page.marker().id(), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        int createdPages = 0;
        int updatedPages = 0;
        int skippedPages = 0;
        Set<String> localIds = documents.stream().map(ZyplayerSyncDocument::id).collect(Collectors.toSet());
        for (ZyplayerSyncDocument document : documents) {
            String hash = ZyplayerSyncMarker.hash(document.content());
            Long parentId = ensureFolderPath(spaceState.space().id(), spaceState.versionId(), document.folderPath(), foldersByKey);
            PageState remotePage = managedPagesById.get(document.id());
            if (remotePage != null && hash.equals(remotePage.marker().hash())
                    && Objects.equals(document.title(), remotePage.page().name())
                    && Objects.equals(parentId, remotePage.page().parentId())) {
                skippedPages++;
                continue;
            }
            ZyplayerPage savedPage = upsertPageWithRetry(spaceState.space().id(), spaceState.versionId(), parentId, descriptor, document, remotePage, hash);
            if (remotePage == null) {
                createdPages++;
            } else {
                updatedPages++;
            }
            if (options.release()) {
                client.releasePage(savedPage.id());
            }
        }

        int deletedPages = 0;
        if (options.deleteMissing()) {
            for (PageState remotePage : managedPagesById.values()) {
                if (!localIds.contains(remotePage.marker().id())) {
                    client.deletePage(spaceState.space().id(), remotePage.page().id());
                    deletedPages++;
                }
            }
        }
        return new ZyplayerSyncResult(spaceState.created() ? 1 : 0, createdPages, updatedPages, skippedPages, deletedPages);
    }

    private ZyplayerPage upsertPageWithRetry(
            Long spaceId,
            Long versionId,
            Long parentId,
            ZyplayerServiceDescriptor descriptor,
            ZyplayerSyncDocument document,
            PageState remotePage,
            String hash) {
        try {
            return upsertPage(spaceId, versionId, parentId, descriptor, document, remotePage, hash);
        } catch (ZyplayerOpenApiException e) {
            if (!isStaleEditVersion(e)) {
                throw e;
            }
            PageState latestPage = loadPageStates(spaceId).stream()
                    .filter(page -> page.marker() != null)
                    .filter(page -> descriptor.applicationName().equals(page.marker().service()))
                    .filter(page -> document.id().equals(page.marker().id()))
                    .findFirst()
                    .orElseThrow(() -> e);
            return upsertPage(spaceId, versionId, parentId, descriptor, document, latestPage, hash);
        }
    }

    private boolean isStaleEditVersion(ZyplayerOpenApiException e) {
        String message = e.getMessage();
        return message != null && (message.contains("版本已过期") || message.contains("已被更新"));
    }

    private Long ensureFolderPath(Long spaceId, Long versionId, List<String> folderPath, Map<FolderKey, ZyplayerPage> foldersByKey) {
        Long parentId = null;
        for (String folderName : folderPath) {
            FolderKey key = new FolderKey(parentId, folderName);
            ZyplayerPage folder = foldersByKey.get(key);
            if (folder == null) {
                folder = createFolder(spaceId, versionId, parentId, folderName);
                foldersByKey.put(key, folder);
            }
            parentId = folder.id();
        }
        return parentId;
    }

    private ZyplayerPage createFolder(Long spaceId, Long versionId, Long parentId, String folderName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("spaceId", spaceId);
        if (versionId != null) {
            payload.put("versionId", versionId);
        }
        payload.put("parentId", parentId);
        payload.put("name", folderName);
        payload.put("editorType", ZyplayerEditorTypes.FOLDER);
        payload.put("content", "");
        payload.put("preview", "");
        payload.put("release", false);
        return client.updatePage(payload);
    }

    private SpaceState findOrCreateSpace(ZyplayerServiceDescriptor descriptor) {
        Long groupId = findOrCreateSpaceGroup(descriptor);
        Optional<ZyplayerSpace> space = client.listSpaces().stream()
                .filter(item -> descriptor.spaceUuid().equals(item.uuid()))
                .findFirst();
        if (space.isPresent()) {
            return new SpaceState(space.get(), false, ensureSpaceVersion(space.get(), descriptor));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", descriptor.spaceName());
        payload.put("uuid", descriptor.spaceUuid());
        payload.put("type", 1);
        payload.put("spaceExplain", descriptor.applicationName() + " API docs synced by isass");
        payload.put("versionControl", 1);
        if (groupId != null) {
            payload.put("groupId", groupId);
        }
        ZyplayerSpace createdSpace = client.updateSpace(payload);
        return new SpaceState(createdSpace, true, ensureSpaceVersion(createdSpace, descriptor));
    }

    private Long findOrCreateSpaceGroup(ZyplayerServiceDescriptor descriptor) {
        try {
            Optional<ZyplayerSpaceGroup> group = client.listSpaceGroups().stream()
                    .filter(item -> descriptor.groupName().equals(item.groupName()))
                    .findFirst();
            if (group.isPresent()) {
                return group.get().id();
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("groupName", descriptor.groupName());
            ZyplayerSpaceGroup createdGroup = client.updateSpaceGroup(payload);
            return createdGroup == null ? null : createdGroup.id();
        } catch (ZyplayerOpenApiException e) {
            LOGGER.warn("zyplayer space group sync skipped: {}", e.getMessage());
            return null;
        }
    }

    private Long ensureSpaceVersion(ZyplayerSpace space, ZyplayerServiceDescriptor descriptor) {
        if (space == null) {
            return null;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("spaceId", space.id());
            payload.put("versionName", descriptor.version());
            ZyplayerSpaceVersion version = client.createSpaceVersion(payload);
            return version == null ? null : version.id();
        } catch (ZyplayerOpenApiException e) {
            if (isDuplicateVersion(e)) {
                LOGGER.info("zyplayer space version already exists: {}", descriptor.version());
                return null;
            }
            LOGGER.warn("zyplayer space version sync skipped: {}", e.getMessage());
            return null;
        }
    }

    private boolean isDuplicateVersion(ZyplayerOpenApiException e) {
        String message = e.getMessage();
        return message != null && (message.contains("已存在") || message.contains("重复") || message.contains("duplicate"));
    }

    private List<PageState> loadPageStates(Long spaceId) {
        List<PageState> states = new ArrayList<>();
        for (ZyplayerPage page : flatten(client.listPages(spaceId))) {
            if (Objects.equals(page.editorType(), ZyplayerEditorTypes.FOLDER)) {
                states.add(new PageState(page, null));
                continue;
            }
            ZyplayerPageContent detail = client.pageDetail(spaceId, page.id());
            ZyplayerSyncMarker marker = ZyplayerSyncMarker.parse(detail.content()).orElse(null);
            states.add(new PageState(detail.wikiPage() == null ? page : detail.wikiPage(), marker));
        }
        return states;
    }

    private ZyplayerPage upsertPage(
            Long spaceId,
            Long versionId,
            Long parentId,
            ZyplayerServiceDescriptor descriptor,
            ZyplayerSyncDocument document,
            PageState remotePage,
            String hash) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (remotePage != null) {
            payload.put("id", remotePage.page().id());
            payload.put("editVersion", remotePage.page().editVersion());
        }
        payload.put("spaceId", spaceId);
        if (versionId != null) {
            payload.put("versionId", versionId);
        }
        payload.put("parentId", parentId);
        payload.put("name", document.title());
        payload.put("editorType", document.editorType());
        ZyplayerSyncMarker marker = new ZyplayerSyncMarker(descriptor.applicationName(), document.id(), hash);
        payload.put("content", markContent(document, marker));
        payload.put("preview", document.content());
        payload.put("release", false);
        return client.updatePage(payload);
    }

    private String markContent(ZyplayerSyncDocument document, ZyplayerSyncMarker marker) {
        if (document.editorType() == ZyplayerEditorTypes.API) {
            return ZyplayerSyncMarker.attachToJson(marker, document.content());
        }
        return ZyplayerSyncMarker.prepend(marker, document.content());
    }

    private List<ZyplayerPage> flatten(List<ZyplayerPage> pages) {
        List<ZyplayerPage> flatPages = new ArrayList<>();
        for (ZyplayerPage page : pages) {
            flatPages.add(page);
            if (page.children() != null && !page.children().isEmpty()) {
                flatPages.addAll(flatten(page.children()));
            }
        }
        return flatPages;
    }

    private record SpaceState(ZyplayerSpace space, boolean created, Long versionId) {
    }

    private record PageState(ZyplayerPage page, ZyplayerSyncMarker marker) {
    }

    private record FolderKey(Long parentId, String name) {
    }
}
