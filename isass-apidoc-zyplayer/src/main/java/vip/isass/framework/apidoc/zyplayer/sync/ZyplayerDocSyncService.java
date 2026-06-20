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
import java.util.Comparator;
import java.util.HashSet;
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

    private static final List<String> ROOT_FOLDER_NAMES = List.of("api接口", "使用文档", "设计文档", "数据库文档");

    private static final Map<String, Integer> ROOT_FOLDER_SEQ_NO = Map.of(
            "api接口", 10,
            "使用文档", 20,
            "设计文档", 30,
            "数据库文档", 40
    );

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
        Map<Long, Integer> descendantCounts = descendantCounts(remotePages.stream().map(PageState::page).toList());
        Map<FolderKey, ZyplayerPage> foldersByKey = remotePages.stream()
                .map(PageState::page)
                .filter(page -> Objects.equals(page.editorType(), ZyplayerEditorTypes.FOLDER))
                .collect(Collectors.toMap(page -> FolderKey.of(page.parentId(), page.name()),
                        Function.identity(), (left, right) -> canonicalFolder(left, right, descendantCounts), LinkedHashMap::new));
        Map<String, PageState> managedPagesById = remotePages.stream()
                .filter(page -> page.marker() != null)
                .filter(page -> descriptor.applicationName().equals(page.marker().service()))
                .collect(Collectors.toMap(page -> page.marker().id(), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        int createdPages = 0;
        int updatedPages = 0;
        int skippedPages = 0;
        ensureManagedRootFolders(spaceState.space().id(), documents, foldersByKey);
        Set<String> localIds = documents.stream().map(ZyplayerSyncDocument::id).collect(Collectors.toSet());
        for (ZyplayerSyncDocument document : documents) {
            String hash = ZyplayerSyncMarker.hash(document.content());
            Long parentId = ensureFolderPath(spaceState.space().id(), document.folderPath(), foldersByKey);
            PageState remotePage = managedPagesById.get(document.id());
            if (remotePage != null && hash.equals(remotePage.marker().hash())
                    && Objects.equals(document.title(), remotePage.page().name())
                    && Objects.equals(parentId, remotePage.page().parentId())) {
                skippedPages++;
                continue;
            }
            ZyplayerPage savedPage = upsertPageWithRetry(spaceState.space().id(), parentId, descriptor, document, remotePage, hash);
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
            deletedPages += deleteEmptyObsoleteFolders(spaceState.space().id(), documents);
        }
        ensureSpaceVersion(spaceState.space(), descriptor, !spaceState.created());
        return new ZyplayerSyncResult(spaceState.created() ? 1 : 0, createdPages, updatedPages, skippedPages, deletedPages);
    }

    private void ensureManagedRootFolders(
            Long spaceId,
            List<ZyplayerSyncDocument> documents,
            Map<FolderKey, ZyplayerPage> foldersByKey) {
        boolean hasManagedFolder = documents.stream()
                .map(ZyplayerSyncDocument::folderPath)
                .filter(path -> !path.isEmpty())
                .map(List::getFirst)
                .anyMatch(ROOT_FOLDER_SEQ_NO::containsKey);
        if (!hasManagedFolder) {
            return;
        }
        for (String rootFolderName : ROOT_FOLDER_NAMES) {
            ensureFolderPath(spaceId, List.of(rootFolderName), foldersByKey);
        }
    }

    private int deleteEmptyObsoleteFolders(Long spaceId, List<ZyplayerSyncDocument> documents) {
        Set<List<String>> localFolderPaths = localFolderPaths(documents);
        if (localFolderPaths.isEmpty()) {
            return 0;
        }
        Set<String> managedRootNames = localFolderPaths.stream()
                .filter(path -> !path.isEmpty())
                .map(List::getFirst)
                .collect(Collectors.toSet());
        List<ZyplayerPage> pages = flatten(client.listPages(spaceId));
        Map<Long, ZyplayerPage> pagesById = pages.stream()
                .filter(page -> page.id() != null)
                .collect(Collectors.toMap(ZyplayerPage::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, Integer> childCounts = childCounts(pages);
        Map<Long, Integer> descendantCounts = descendantCounts(pages);

        List<FolderState> obsoleteFolders = pages.stream()
                .filter(page -> Objects.equals(page.editorType(), ZyplayerEditorTypes.FOLDER))
                .map(page -> new FolderState(page, folderPath(page, pagesById)))
                .filter(folder -> !folder.path().isEmpty())
                .filter(folder -> managedRootNames.contains(folder.path().getFirst()))
                .filter(folder -> !localFolderPaths.contains(folder.path()))
                .filter(folder -> childCounts.getOrDefault(folder.page().id(), 0) == 0)
                .sorted(Comparator.comparingInt((FolderState folder) -> folder.path().size()).reversed())
                .toList();
        Set<Long> obsoleteFolderIds = obsoleteFolders.stream()
                .map(folder -> folder.page().id())
                .collect(Collectors.toSet());
        Map<Long, Integer> effectiveChildCounts = effectiveChildCounts(pages, obsoleteFolderIds);

        Map<List<String>, List<FolderState>> foldersByPath = pages.stream()
                .filter(page -> Objects.equals(page.editorType(), ZyplayerEditorTypes.FOLDER))
                .map(page -> new FolderState(page, folderPath(page, pagesById)))
                .filter(folder -> !folder.path().isEmpty())
                .filter(folder -> managedRootNames.contains(folder.path().getFirst()))
                .collect(Collectors.groupingBy(FolderState::path, LinkedHashMap::new, Collectors.toList()));
        Set<Long> duplicateFolderIds = new HashSet<>();
        for (List<FolderState> duplicatedFolders : foldersByPath.values()) {
            if (duplicatedFolders.size() <= 1) {
                continue;
            }
            Long canonicalId = duplicatedFolders.stream()
                    .max(Comparator.comparingInt((FolderState folder) -> descendantCounts.getOrDefault(folder.page().id(), 0))
                            .thenComparingInt(folder -> childCounts.getOrDefault(folder.page().id(), 0))
                            .thenComparing(folder -> folder.page().id(), Comparator.reverseOrder()))
                    .map(folder -> folder.page().id())
                    .orElse(null);
            for (FolderState folder : duplicatedFolders) {
                if (!Objects.equals(folder.page().id(), canonicalId)
                        && effectiveChildCounts.getOrDefault(folder.page().id(), 0) == 0) {
                    duplicateFolderIds.add(folder.page().id());
                }
            }
        }

        Set<Long> deletedFolderIds = new HashSet<>();
        for (FolderState folder : obsoleteFolders) {
            deletedFolderIds.add(folder.page().id());
            client.deletePage(spaceId, folder.page().id());
        }
        for (Long folderId : duplicateFolderIds) {
            if (deletedFolderIds.add(folderId)) {
                client.deletePage(spaceId, folderId);
            }
        }
        return deletedFolderIds.size();
    }

    private Set<List<String>> localFolderPaths(List<ZyplayerSyncDocument> documents) {
        Set<List<String>> folderPaths = new HashSet<>();
        for (ZyplayerSyncDocument document : documents) {
            List<String> path = document.folderPath();
            for (int index = 1; index <= path.size(); index++) {
                folderPaths.add(List.copyOf(path.subList(0, index)));
            }
        }
        return folderPaths;
    }

    private List<String> folderPath(ZyplayerPage page, Map<Long, ZyplayerPage> pagesById) {
        List<String> names = new ArrayList<>();
        Set<Long> visitedIds = new HashSet<>();
        ZyplayerPage current = page;
        while (current != null && current.id() != null && visitedIds.add(current.id())) {
            names.add(current.name());
            current = current.parentId() == null ? null : pagesById.get(current.parentId());
        }
        return names.reversed();
    }

    private ZyplayerPage upsertPageWithRetry(
            Long spaceId,
            Long parentId,
            ZyplayerServiceDescriptor descriptor,
            ZyplayerSyncDocument document,
            PageState remotePage,
            String hash) {
        try {
            return upsertPage(spaceId, parentId, descriptor, document, remotePage, hash);
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
            return upsertPage(spaceId, parentId, descriptor, document, latestPage, hash);
        }
    }

    private boolean isStaleEditVersion(ZyplayerOpenApiException e) {
        String message = e.getMessage();
        return message != null && (message.contains("版本已过期") || message.contains("已被更新"));
    }

    private Long ensureFolderPath(
            Long spaceId,
            List<String> folderPath,
            Map<FolderKey, ZyplayerPage> foldersByKey) {
        Long parentId = null;
        for (String folderName : folderPath) {
            FolderKey key = FolderKey.of(parentId, folderName);
            ZyplayerPage folder = foldersByKey.get(key);
            Integer seqNo = parentId == null ? ROOT_FOLDER_SEQ_NO.get(folderName) : null;
            if (folder == null) {
                folder = createFolder(spaceId, parentId, folderName, seqNo);
                foldersByKey.put(key, folder);
            }
            parentId = folder.id();
        }
        return parentId;
    }

    private ZyplayerPage createFolder(Long spaceId, Long parentId, String folderName, Integer seqNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("spaceId", spaceId);
        payload.put("parentId", parentId == null ? 0L : parentId);
        payload.put("name", folderName);
        payload.put("editorType", ZyplayerEditorTypes.FOLDER);
        payload.put("content", "");
        payload.put("preview", "");
        payload.put("release", false);
        if (seqNo != null) {
            payload.put("seqNo", seqNo);
        }
        return client.updatePage(payload);
    }

    private SpaceState findOrCreateSpace(ZyplayerServiceDescriptor descriptor) {
        Long groupId = findOrCreateSpaceGroup(descriptor);
        Optional<ZyplayerSpace> space = findSpace(descriptor);
        if (space.isPresent()) {
            return new SpaceState(space.get(), false);
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
        ZyplayerSpace createdSpace;
        try {
            createdSpace = client.updateSpace(payload);
        } catch (ZyplayerOpenApiException e) {
            if (!isDuplicateSpaceUuid(e)) {
                throw e;
            }
            Optional<ZyplayerSpace> refreshedSpace = findSpace(descriptor);
            if (refreshedSpace.isPresent()) {
                return new SpaceState(refreshedSpace.get(), false);
            }
            throw new ZyplayerOpenApiException("zyplayer space uuid prefix is occupied but not visible in open-api list: "
                    + descriptor.spaceUuidPrefix() + ". Please check zyplayer-doc recycle bin or database deleted spaces.", e);
        }
        return new SpaceState(createdSpace, true);
    }

    private Optional<ZyplayerSpace> findSpace(ZyplayerServiceDescriptor descriptor) {
        return client.listSpaces().stream()
                .filter(item -> isManagedSpace(descriptor, item))
                .max(Comparator.comparingLong(item -> spaceUuidTimestamp(descriptor, item.uuid())));
    }

    private boolean isManagedSpace(ZyplayerServiceDescriptor descriptor, ZyplayerSpace space) {
        return Objects.equals(descriptor.spaceName(), space.name())
                && spaceUuidTimestamp(descriptor, space.uuid()) > 0;
    }

    private long spaceUuidTimestamp(ZyplayerServiceDescriptor descriptor, String uuid) {
        String prefix = descriptor.spaceUuidPrefix() + "@";
        if (uuid == null || !uuid.startsWith(prefix)) {
            return -1;
        }
        String timestamp = uuid.substring(prefix.length());
        if (timestamp.length() != 17) {
            return -1;
        }
        try {
            return Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return -1;
        }
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

    private Long ensureSpaceVersion(ZyplayerSpace space, ZyplayerServiceDescriptor descriptor, boolean existingSpace) {
        if (space == null) {
            return null;
        }
        if (existingSpace) {
            try {
                Optional<ZyplayerSpaceVersion> existingVersion = client.listSpaceVersions(space.id()).stream()
                        .filter(item -> descriptor.version().equals(item.versionName()))
                        .findFirst();
                if (existingVersion.isPresent()) {
                    return existingVersion.get().id();
                }
            } catch (ZyplayerOpenApiException e) {
                LOGGER.warn("zyplayer space version query failed, skip creating version for existing space to avoid duplicates: {}",
                        e.getMessage());
                return null;
            }
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

    private boolean isDuplicateSpaceUuid(ZyplayerOpenApiException e) {
        String message = e.getMessage();
        return message != null && (message.contains("唯一编码已被占用")
                || message.contains("space uuid")
                || message.contains("uuid is occupied"));
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

    private Map<Long, Integer> childCounts(List<ZyplayerPage> pages) {
        Map<Long, Integer> childCounts = new LinkedHashMap<>();
        for (ZyplayerPage page : pages) {
            Long parentId = normalizeParentIdForKey(page.parentId());
            if (parentId != null) {
                childCounts.merge(parentId, 1, Integer::sum);
            }
        }
        return childCounts;
    }

    private Map<Long, Integer> effectiveChildCounts(List<ZyplayerPage> pages, Set<Long> deletedPageIds) {
        Map<Long, Integer> childCounts = new LinkedHashMap<>();
        for (ZyplayerPage page : pages) {
            if (page.id() != null && deletedPageIds.contains(page.id())) {
                continue;
            }
            Long parentId = normalizeParentIdForKey(page.parentId());
            if (parentId != null) {
                childCounts.merge(parentId, 1, Integer::sum);
            }
        }
        return childCounts;
    }

    private Map<Long, Integer> descendantCounts(List<ZyplayerPage> pages) {
        Map<Long, List<ZyplayerPage>> childrenByParentId = pages.stream()
                .filter(page -> normalizeParentIdForKey(page.parentId()) != null)
                .collect(Collectors.groupingBy(page -> normalizeParentIdForKey(page.parentId()), LinkedHashMap::new, Collectors.toList()));
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (ZyplayerPage page : pages) {
            if (page.id() != null) {
                counts.put(page.id(), descendantCount(page.id(), childrenByParentId, new HashSet<>()));
            }
        }
        return counts;
    }

    private int descendantCount(Long pageId, Map<Long, List<ZyplayerPage>> childrenByParentId, Set<Long> visitedIds) {
        if (pageId == null || !visitedIds.add(pageId)) {
            return 0;
        }
        int count = 0;
        for (ZyplayerPage child : childrenByParentId.getOrDefault(pageId, List.of())) {
            count++;
            count += descendantCount(child.id(), childrenByParentId, visitedIds);
        }
        return count;
    }

    private ZyplayerPage canonicalFolder(ZyplayerPage left, ZyplayerPage right, Map<Long, Integer> descendantCounts) {
        int leftDescendantCount = descendantCounts.getOrDefault(left.id(), 0);
        int rightDescendantCount = descendantCounts.getOrDefault(right.id(), 0);
        if (leftDescendantCount != rightDescendantCount) {
            return leftDescendantCount > rightDescendantCount ? left : right;
        }
        if (left.id() == null) {
            return right;
        }
        if (right.id() == null) {
            return left;
        }
        return left.id() <= right.id() ? left : right;
    }

    private Long normalizeParentIdForKey(Long parentId) {
        return parentId == null || parentId == 0 ? null : parentId;
    }

    private Long normalizeParentIdForPayload(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private ZyplayerPage upsertPage(
            Long spaceId,
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

    private record SpaceState(ZyplayerSpace space, boolean created) {
    }

    private record PageState(ZyplayerPage page, ZyplayerSyncMarker marker) {
    }

    private record FolderKey(Long parentId, String name) {

        private static FolderKey of(Long parentId, String name) {
            Long normalizedParentId = parentId == null || parentId == 0 ? null : parentId;
            return new FolderKey(normalizedParentId, name);
        }
    }

    private record FolderState(ZyplayerPage page, List<String> path) {
    }
}
