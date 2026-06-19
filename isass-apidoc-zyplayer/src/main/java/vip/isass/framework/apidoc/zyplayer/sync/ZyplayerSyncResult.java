package vip.isass.framework.apidoc.zyplayer.sync;

/**
 * @author Rain
 */
public record ZyplayerSyncResult(
        int createdSpaces,
        int createdPages,
        int updatedPages,
        int skippedPages,
        int deletedPages
) {
}
