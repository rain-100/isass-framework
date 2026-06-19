package vip.isass.framework.apidoc.zyplayer;

import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;
import vip.isass.framework.web.servicedocs.ServiceDoc;
import vip.isass.framework.web.servicedocs.ServiceDocsScanner;

import java.util.List;

/**
 * @author Rain
 */
public class ZyplayerServiceDocsCollector {

    private static final int MARKDOWN_EDITOR_TYPE = 2;

    private final ServiceDocsScanner serviceDocsScanner;

    public ZyplayerServiceDocsCollector(ServiceDocsScanner serviceDocsScanner) {
        this.serviceDocsScanner = serviceDocsScanner;
    }

    public List<ZyplayerSyncDocument> collect() {
        return serviceDocsScanner.findAll().stream()
                .map(this::toSyncDocument)
                .toList();
    }

    private ZyplayerSyncDocument toSyncDocument(ServiceDoc serviceDoc) {
        return new ZyplayerSyncDocument(
                serviceDoc.id(),
                serviceDoc.title(),
                MARKDOWN_EDITOR_TYPE,
                serviceDocsScanner.readContent(serviceDoc.id()));
    }
}
