// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.ResolvableType;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.service.ICrudService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NocodeInitializationRunner {

    private static final Logger log = LoggerFactory.getLogger(NocodeInitializationRunner.class);

    private final NocodeInitializationDataService dataService;
    private final NocodeInitializationRemoteClient remoteClient;
    private final NocodeInitializationProperties properties;
    private final Map<String, String> entityOwners;

    public NocodeInitializationRunner(NocodeInitializationDataService dataService,
                                      NocodeInitializationRemoteClient remoteClient,
                                      NocodeInitializationProperties properties,
                                      ServiceDefinitionRegistry definitions) {
        this.dataService = dataService;
        this.remoteClient = remoteClient;
        this.properties = properties;
        Map<String, String> owners = new LinkedHashMap<>();
        for (ServiceDefinition definition : definitions.all()) {
            if (!definition.hasNocodeOperations()) continue;
            Class<?> entity = ResolvableType.forClass(definition.serviceInterface())
                    .as(ICrudService.class).getGeneric(0).resolve();
            if (entity != null) owners.putIfAbsent(definition.resourceName(), definition.serviceName());
        }
        entityOwners = Map.copyOf(owners);
    }

    public ApplicationRunner runner() {
        return arguments -> {
            if (!properties.isEnabled()) return;
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(properties.getLocation());
            for (Resource resource : resources) {
                try (var input = resource.getInputStream()) {
                    var result = importDocument(dataService.readDocument(input));
                    if (!result.failures().isEmpty() && properties.isFailFast()) {
                        throw new IllegalStateException("初始化存在失败实体: " + result.failures());
                    }
                    log.info("已导入初始化数据 [{}]: total={}, inserted={}, skipped={}",
                            resource.getFilename(), result.total(), result.inserted(), result.skipped());
                } catch (Exception exception) {
                    if (properties.isFailFast()) {
                        throw new IllegalStateException("无法导入初始化数据: " + resource, exception);
                    }
                    log.error("无法导入初始化数据: {}", resource, exception);
                }
            }
        };
    }

    private NocodeInitializationDataService.ImportResult importDocument(
            Map<String, ? extends Collection<?>> document) {
        Map<String, Map<String, Collection<?>>> byService = new LinkedHashMap<>();
        document.forEach((entity, rows) -> {
            String service = dataService.localServiceName(entity);
            if (service == null) service = entityOwners.get(entity);
            if (service == null) throw new IllegalArgumentException("未知 NoCode 初始化实体: " + entity);
            byService.computeIfAbsent(service, ignored -> new LinkedHashMap<>()).put(entity, rows);
        });
        int total = 0;
        int inserted = 0;
        int skipped = 0;
        Map<String, String> failures = new LinkedHashMap<>();
        for (var entry : byService.entrySet()) {
            var result = dataService.hasLocalService(entry.getKey())
                    ? dataService.importData(entry.getKey(), entry.getValue())
                    : remoteClient.importData(entry.getKey(), entry.getValue());
            total += result.total();
            inserted += result.inserted();
            skipped += result.skipped();
            failures.putAll(result.failures());
        }
        return new NocodeInitializationDataService.ImportResult(total, inserted, skipped, Map.copyOf(failures));
    }
}
