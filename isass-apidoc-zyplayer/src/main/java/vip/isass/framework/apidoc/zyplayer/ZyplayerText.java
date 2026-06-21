package vip.isass.framework.apidoc.zyplayer;

/**
 * @author Rain
 */
public final class ZyplayerText {

    private ZyplayerText() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String trimToDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    public static String firstText(String defaultValue, String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return defaultValue;
    }
}
