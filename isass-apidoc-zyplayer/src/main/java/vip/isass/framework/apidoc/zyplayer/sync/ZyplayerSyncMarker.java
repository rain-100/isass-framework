package vip.isass.framework.apidoc.zyplayer.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rain
 */
public record ZyplayerSyncMarker(
        String service,
        String id,
        String hash
) {

    private static final Pattern MARKER_PATTERN = Pattern.compile(
            "<!--\\s*isass-doc-sync:\\s*service=([^;]+);\\s*id=([^;]+);\\s*hash=([^\\s]+)\\s*-->");

    public static String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash zyplayer sync document", e);
        }
    }

    public static String prepend(ZyplayerSyncMarker marker, String content) {
        return "<!-- isass-doc-sync: service=" + marker.service
                + "; id=" + marker.id
                + "; hash=" + marker.hash
                + " -->\n\n"
                + (content == null ? "" : content);
    }

    public static Optional<ZyplayerSyncMarker> parse(String content) {
        if (content == null) {
            return Optional.empty();
        }
        Matcher matcher = MARKER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new ZyplayerSyncMarker(matcher.group(1).trim(), matcher.group(2).trim(), matcher.group(3).trim()));
    }
}
