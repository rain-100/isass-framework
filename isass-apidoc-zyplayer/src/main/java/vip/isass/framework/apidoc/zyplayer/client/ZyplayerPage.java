package vip.isass.framework.apidoc.zyplayer.client;

import java.util.List;

/**
 * @author Rain
 */
public record ZyplayerPage(
        Long id,
        Long spaceId,
        String name,
        Long parentId,
        Integer editorType,
        Integer editVersion,
        List<ZyplayerPage> children
) {
}
