package vip.isass.framework.apidoc.zyplayer.client;

import java.util.List;
import java.util.Map;

/**
 * @author Rain
 */
public interface ZyplayerClientOperations {

    List<ZyplayerSpace> listSpaces();

    ZyplayerSpace updateSpace(Map<String, Object> payload);

    List<ZyplayerPage> listPages(Long spaceId);

    ZyplayerPageContent pageDetail(Long spaceId, Long pageId);

    ZyplayerPage updatePage(Map<String, Object> payload);

    void deletePage(Long spaceId, Long pageId);

    void releasePage(Long pageId);
}
