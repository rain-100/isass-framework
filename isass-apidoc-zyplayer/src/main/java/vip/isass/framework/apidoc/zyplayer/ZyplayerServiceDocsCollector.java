package vip.isass.framework.apidoc.zyplayer;

import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerEditorTypes;
import vip.isass.framework.apidoc.zyplayer.openapi.ZyplayerOpenApiDocsCollector;
import vip.isass.framework.web.servicedocs.ServiceDoc;
import vip.isass.framework.web.servicedocs.ServiceDocsScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rain
 */
public class ZyplayerServiceDocsCollector {

    private final ServiceDocsScanner serviceDocsScanner;

    private final ZyplayerOpenApiDocsCollector openApiDocsCollector;

    public ZyplayerServiceDocsCollector(ServiceDocsScanner serviceDocsScanner) {
        this(serviceDocsScanner, null);
    }

    public ZyplayerServiceDocsCollector(
            ServiceDocsScanner serviceDocsScanner,
            ZyplayerOpenApiDocsCollector openApiDocsCollector) {
        this.serviceDocsScanner = serviceDocsScanner;
        this.openApiDocsCollector = openApiDocsCollector;
    }

    public List<ZyplayerSyncDocument> collect() {
        List<ZyplayerSyncDocument> documents = new ArrayList<>(serviceDocsScanner.findAll().stream()
                .map(this::toSyncDocument)
                .toList());
        if (openApiDocsCollector != null) {
            documents.addAll(openApiDocsCollector.collect());
        }
        return documents;
    }

    private ZyplayerSyncDocument toSyncDocument(ServiceDoc serviceDoc) {
        return new ZyplayerSyncDocument(
                serviceDoc.id(),
                serviceDoc.title(),
                ZyplayerEditorTypes.MARKDOWN,
                serviceDocsScanner.readContent(serviceDoc.id()),
                List.of(markdownFolderName(serviceDoc)));
    }

    private String markdownFolderName(ServiceDoc serviceDoc) {
        return switch (serviceDoc.type()) {
            case "api" -> "设计文档";
            case "database" -> "数据库文档";
            case "design" -> "设计文档";
            default -> "使用文档";
        };
    }
}
