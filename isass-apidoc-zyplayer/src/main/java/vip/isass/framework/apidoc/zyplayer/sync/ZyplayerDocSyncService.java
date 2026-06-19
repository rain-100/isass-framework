package vip.isass.framework.apidoc.zyplayer.sync;

import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.apidoc.zyplayer.ZyplayerServiceDescriptor;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerClientOperations;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPage;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerPageContent;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpace;

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
            PageState remotePage = managedPagesById.get(document.id());
            if (remotePage != null && hash.equals(remotePage.marker().hash())
                    && Objects.equals(document.title(), remotePage.page().name())) {
                skippedPages++;
                continue;
            }
            ZyplayerPage savedPage = upsertPage(spaceState.space().id(), descriptor, document, remotePage, hash);
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

    private SpaceState findOrCreateSpace(ZyplayerServiceDescriptor descriptor) {
        Optional<ZyplayerSpace> space = client.listSpaces().stream()
                .filter(item -> descriptor.spaceUuid().equals(item.uuid()))
                .findFirst();
        if (space.isPresent()) {
            return new SpaceState(space.get(), false);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", descriptor.spaceName());
        payload.put("uuid", descriptor.spaceUuid());
        payload.put("type", 1);
        payload.put("spaceExplain", descriptor.applicationName() + " API docs synced by isass");
        payload.put("versionControl", 0);
        return new SpaceState(client.updateSpace(payload), true);
    }

    private List<PageState> loadPageStates(Long spaceId) {
        List<PageState> states = new ArrayList<>();
        for (ZyplayerPage page : flatten(client.listPages(spaceId))) {
            ZyplayerPageContent detail = client.pageDetail(spaceId, page.id());
            ZyplayerSyncMarker marker = ZyplayerSyncMarker.parse(detail.content()).orElse(null);
            states.add(new PageState(page, marker));
        }
        return states;
    }

    private ZyplayerPage upsertPage(
            Long spaceId,
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
        payload.put("name", document.title());
        payload.put("editorType", document.editorType());
        payload.put("content", ZyplayerSyncMarker.prepend(
                new ZyplayerSyncMarker(descriptor.applicationName(), document.id(), hash),
                document.content()));
        payload.put("preview", document.content());
        payload.put("release", false);
        return client.updatePage(payload);
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

    private record SpaceState(ZyplayerSpace space, boolean created) {
    }

    private record PageState(ZyplayerPage page, ZyplayerSyncMarker marker) {
    }
}
