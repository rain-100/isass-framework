package vip.isass.framework.nocode.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Imports local and target-service JSON documents in {@code resources/init} after startup. */
final class NocodeInitializationRunner {

    private static final Logger log = LoggerFactory.getLogger(NocodeInitializationRunner.class);

    private final NocodeInitializationDataService dataService;
    private final NocodeInitializationRemoteClient remoteClient;
    private final NocodeInitializationProperties properties;

    NocodeInitializationRunner(
            NocodeInitializationDataService dataService,
            NocodeInitializationRemoteClient remoteClient,
            NocodeInitializationProperties properties
    ) {
        this.dataService = dataService;
        this.remoteClient = remoteClient;
        this.properties = properties;
    }

    ApplicationRunner runner() {
        return arguments -> {
            if (!properties.isEnabled()) {
                return;
            }
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(properties.getLocation());
            for (Resource resource : resources) {
                try (var input = resource.getInputStream()) {
                    String targetService = targetService(resource);
                    boolean localTarget = targetService != null && dataService.hasLocalService(targetService);
                    if (targetService != null && !localTarget && !properties.isRemoteEnabled()) {
                        log.info("Skipped remote nocode init data [{}]; remote initialization is disabled",
                                resource.getFilename());
                        continue;
                    }
                    NocodeInitializationDataService.ImportResult result = targetService == null
                            ? dataService.importResource(input)
                            : localTarget
                            ? dataService.importData(targetService, dataService.readDocument(input))
                            : remoteClient.importData(targetService, dataService.readDocument(input));
                    if (!result.failures().isEmpty() && properties.isFailFast()) {
                        throw new IllegalStateException("Initialization contains failed entities: " + result.failures());
                    }
                    log.info("Imported nocode init data [{}]: total={}, inserted={}, skipped={}",
                            resource.getFilename(), result.total(), result.inserted(), result.skipped());
                } catch (Exception exception) {
                    if (properties.isFailFast()) {
                        throw new IllegalStateException("Cannot import nocode init data: " + resource, exception);
                    }
                    log.error("Cannot import nocode init data: {}", resource, exception);
                }
            }
        };
    }

    private String targetService(Resource resource) {
        String path = resource.getDescription().replace('\\', '/');
        int initIndex = path.indexOf("/init/");
        if (initIndex < 0) return null;
        String remaining = path.substring(initIndex + "/init/".length());
        int separator = remaining.indexOf('/');
        if (separator < 0) return null;
        String firstDirectory = remaining.substring(0, separator);
        return firstDirectory.endsWith("-service") ? firstDirectory : null;
    }
}
