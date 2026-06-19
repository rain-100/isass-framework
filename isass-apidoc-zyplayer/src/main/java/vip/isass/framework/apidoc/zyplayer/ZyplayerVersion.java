package vip.isass.framework.apidoc.zyplayer;

import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rain
 */
public final class ZyplayerVersion {

    private static final Pattern SEMVER_PREFIX = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)");

    private ZyplayerVersion() {
    }

    public static String normalize(String version) {
        if (!StringUtils.hasText(version)) {
            return "0.0.0";
        }
        String trimmedVersion = version.trim();
        Matcher matcher = SEMVER_PREFIX.matcher(trimmedVersion);
        if (matcher.find()) {
            return matcher.group(1);
        }
        int suffixIndex = trimmedVersion.indexOf('-');
        return suffixIndex > 0 ? trimmedVersion.substring(0, suffixIndex) : trimmedVersion;
    }
}
