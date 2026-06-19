package vip.isass.framework.apidoc.zyplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerDocSyncService;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncOptions;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncResult;

/**
 * @author Rain
 */
public class ZyplayerApidocRunner implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZyplayerApidocRunner.class);

    private final ZyplayerServiceDescriptor descriptor;

    private final ZyplayerServiceDocsCollector collector;

    private final ZyplayerDocSyncService syncService;

    private final ZyplayerApidocProperties properties;

    public ZyplayerApidocRunner(
            ZyplayerServiceDescriptor descriptor,
            ZyplayerServiceDocsCollector collector,
            ZyplayerDocSyncService syncService,
            ZyplayerApidocProperties properties) {
        this.descriptor = descriptor;
        this.collector = collector;
        this.syncService = syncService;
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            ZyplayerSyncResult result = syncService.sync(descriptor, collector.collect(),
                    new ZyplayerSyncOptions(properties.isDeleteMissing(), properties.isRelease()));
            LOGGER.info("zyplayer apidoc sync completed: {}", result);
        } catch (Exception e) {
            if (properties.isFailOnError()) {
                throw e;
            }
            LOGGER.warn("zyplayer apidoc sync failed, application startup continues", e);
        }
    }
}
