package vip.isass.framework.apidoc.zyplayer.sync;

import java.util.List;

/**
 * @author Rain
 */
public record ZyplayerSyncDocument(
        String id,
        String title,
        int editorType,
        String content,
        List<String> folderPath
) {

    public ZyplayerSyncDocument(String id, String title, int editorType, String content) {
        this(id, title, editorType, content, List.of());
    }

    public ZyplayerSyncDocument {
        folderPath = folderPath == null ? List.of() : folderPath.stream()
                .filter(folder -> folder != null && !folder.isBlank())
                .map(String::trim)
                .toList();
    }
}
