// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Arrays;
import java.util.Comparator;

public final class NocodeInitializationRunner {

    private static final Logger log = LoggerFactory.getLogger(NocodeInitializationRunner.class);

    private final NocodeInitializationDataService dataService;
    private final NocodeInitializationImporter importer;
    private final NocodeInitializationProperties properties;

    public NocodeInitializationRunner(NocodeInitializationDataService dataService,
                                      NocodeInitializationImporter importer,
                                      NocodeInitializationProperties properties) {
        this.dataService = dataService;
        this.importer = importer;
        this.properties = properties;
    }

    public ApplicationRunner runner() {
        return arguments -> {
            if (!properties.isEnabled()) {
                return;
            }
            Resource[] resources = Arrays.stream(new PathMatchingResourcePatternResolver()
                            .getResources(properties.getLocation()))
                    .filter(resource -> NocodeInitializationDataService.supportsDocument(resource.getFilename()))
                    .sorted(Comparator.comparing(Resource::getDescription))
                    .toArray(Resource[]::new);
            for (Resource resource : resources) {
                try (var input = resource.getInputStream()) {
                    var result = importer.importData(importer.readDocument(input, resource.getFilename()));
                    if (!result.failures().isEmpty()) {
                        log.error("初始化失败实体 [{}]: {}", resource.getFilename(), result.failures());
                    }
                    log.info("已导入初始化数据 [{}]: total={}, inserted={}, skipped={}",
                            resource.getFilename(), result.total(), result.inserted(), result.skipped());
                } catch (Exception exception) {
                    log.error("无法导入初始化数据: {}", resource, exception);
                }
            }
        };
    }
}
