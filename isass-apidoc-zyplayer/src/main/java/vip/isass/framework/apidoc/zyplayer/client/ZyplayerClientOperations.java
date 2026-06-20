package vip.isass.framework.apidoc.zyplayer.client;

import java.util.List;
import java.util.Map;

/**
 * @author Rain
 */
public interface ZyplayerClientOperations {

    List<ZyplayerSpace> listSpaces();

    default List<ZyplayerSpaceGroup> listSpaceGroups() {
        return List.of();
    }

    default ZyplayerSpaceGroup updateSpaceGroup(Map<String, Object> payload) {
        return null;
    }

    ZyplayerSpace updateSpace(Map<String, Object> payload);

    default List<ZyplayerSpaceVersion> listSpaceVersions(Long spaceId) {
        return List.of();
    }

    default ZyplayerSpaceVersion createSpaceVersion(Map<String, Object> payload) {
        return null;
    }

    List<ZyplayerPage> listPages(Long spaceId);

    ZyplayerPageContent pageDetail(Long spaceId, Long pageId);

    ZyplayerPage updatePage(Map<String, Object> payload);

    void deletePage(Long spaceId, Long pageId);

    void releasePage(Long pageId);
}
