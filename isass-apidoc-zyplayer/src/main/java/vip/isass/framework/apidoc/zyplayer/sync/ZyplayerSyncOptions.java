package vip.isass.framework.apidoc.zyplayer.sync;

/**
 * @author Rain
 */
public record ZyplayerSyncOptions(
        boolean deleteMissing,
        boolean release
) {
}
