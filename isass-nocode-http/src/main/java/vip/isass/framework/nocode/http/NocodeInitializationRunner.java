package vip.isass.framework.nocode.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Imports every module-owned JSON document in {@code resources/init} after startup. */
final class NocodeInitializationRunner {

    private static final Logger log = LoggerFactory.getLogger(NocodeInitializationRunner.class);

    private final NocodeInitializationDataService dataService;
    private final NocodeInitializationProperties properties;

    NocodeInitializationRunner(
            NocodeInitializationDataService dataService,
            NocodeInitializationProperties properties
    ) {
        this.dataService = dataService;
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
                    NocodeInitializationDataService.ImportResult result = dataService.importResource(input);
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
}
