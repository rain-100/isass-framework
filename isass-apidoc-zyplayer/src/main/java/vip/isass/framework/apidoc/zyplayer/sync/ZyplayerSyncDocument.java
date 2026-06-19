package vip.isass.framework.apidoc.zyplayer.sync;

/**
 * @author Rain
 */
public record ZyplayerSyncDocument(
        String id,
        String title,
        int editorType,
        String content
) {
}
