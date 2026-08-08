package vip.isass.framework.nocode.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.ServiceContract;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Imports local and target-service JSON documents in {@code resources/init} after startup. */
final class NocodeInitializationRunner {

    private static final Logger log = LoggerFactory.getLogger(NocodeInitializationRunner.class);

    private final NocodeInitializationDataService dataService;
    private final NocodeInitializationRemoteClient remoteClient;
    private final NocodeInitializationProperties properties;
    private final ContractRegistry contracts;

    NocodeInitializationRunner(
            NocodeInitializationDataService dataService,
            NocodeInitializationRemoteClient remoteClient,
            NocodeInitializationProperties properties,
            ContractRegistry contracts
    ) {
        this.dataService = dataService;
        this.remoteClient = remoteClient;
        this.properties = properties;
        this.contracts = contracts;
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
                    NocodeInitializationDataService.ImportResult result = importDocument(dataService.readDocument(input));
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

    private NocodeInitializationDataService.ImportResult importDocument(
            Map<String, ? extends Collection<?>> document
    ) {
        ImportSummary summary = new ImportSummary();
        Map<String, Map<String, Collection<?>>> documentsByService = new LinkedHashMap<>();
        document.forEach((entity, rows) -> {
            String service = dataService.localServiceName(entity);
            if (service == null) {
                ServiceContract contract = contracts.requireServiceByEntity(entity);
                service = contract.service();
            }
            documentsByService.computeIfAbsent(service, ignored -> new LinkedHashMap<>()).put(entity, rows);
        });
        documentsByService.forEach((service, serviceDocument) -> {
            NocodeInitializationDataService.ImportResult result = dataService.hasLocalService(service)
                    ? dataService.importData(service, serviceDocument)
                    : remoteClient.importData(service, serviceDocument);
            summary.add(result);
        });
        return summary.result();
    }

    private static final class ImportSummary {
        private int total;
        private int inserted;
        private int skipped;
        private final Map<String, String> failures = new LinkedHashMap<>();

        private void add(NocodeInitializationDataService.ImportResult result) {
            total += result.total();
            inserted += result.inserted();
            skipped += result.skipped();
            failures.putAll(result.failures());
        }

        private NocodeInitializationDataService.ImportResult result() {
            return new NocodeInitializationDataService.ImportResult(total, inserted, skipped, Map.copyOf(failures));
        }
    }
}
