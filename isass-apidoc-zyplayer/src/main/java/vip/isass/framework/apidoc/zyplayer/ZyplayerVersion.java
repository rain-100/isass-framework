package vip.isass.framework.apidoc.zyplayer;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rain
 */
public final class ZyplayerVersion {

    private static final Pattern MAJOR_VERSION_PREFIX = Pattern.compile("^[vV]?(\\d+)(?:\\.|$)");

    private ZyplayerVersion() {
    }

    public static String normalize(String version) {
        if (!StringUtils.hasText(version)) {
            return "v0.x";
        }
        String trimmedVersion = version.trim();
        Matcher matcher = MAJOR_VERSION_PREFIX.matcher(trimmedVersion);
        if (matcher.find()) {
            return "v" + matcher.group(1) + ".x";
        }
        return trimmedVersion;
    }
}
