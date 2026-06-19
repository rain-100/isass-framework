package vip.isass.framework.apidoc.zyplayer.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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

    private static final String JSON_MARKER_FIELD = "_isassSyncMarker";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    public static String attachToJson(ZyplayerSyncMarker marker, String content) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(content == null || content.isBlank() ? "{}" : content);
            if (!node.isObject()) {
                node = OBJECT_MAPPER.createObjectNode();
            }
            ObjectNode markerNode = OBJECT_MAPPER.createObjectNode();
            markerNode.put("service", marker.service);
            markerNode.put("id", marker.id);
            markerNode.put("hash", marker.hash);
            ((ObjectNode) node).set(JSON_MARKER_FIELD, markerNode);
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to attach zyplayer json sync marker", e);
        }
    }

    public static Optional<ZyplayerSyncMarker> parse(String content) {
        if (content == null) {
            return Optional.empty();
        }
        Optional<ZyplayerSyncMarker> jsonMarker = parseJson(content);
        if (jsonMarker.isPresent()) {
            return jsonMarker;
        }
        Matcher matcher = MARKER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new ZyplayerSyncMarker(matcher.group(1).trim(), matcher.group(2).trim(), matcher.group(3).trim()));
    }

    private static Optional<ZyplayerSyncMarker> parseJson(String content) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(content);
            JsonNode marker = root.path(JSON_MARKER_FIELD);
            if (!marker.isObject()) {
                return Optional.empty();
            }
            String service = marker.path("service").asText("");
            String id = marker.path("id").asText("");
            String hash = marker.path("hash").asText("");
            if (service.isBlank() || id.isBlank() || hash.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ZyplayerSyncMarker(service, id, hash));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
