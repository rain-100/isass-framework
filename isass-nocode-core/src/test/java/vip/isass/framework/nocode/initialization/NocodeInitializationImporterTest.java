// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.junit.jupiter.api.Test;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService.ImportResult;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NocodeInitializationImporterTest {

    private final NocodeInitializationDataService local = mock(NocodeInitializationDataService.class);
    private final INocodeInitializationService remote = mock(INocodeInitializationService.class);

    @Test
    void unknownEntitiesAndUnavailableServicesDoNotBlockOtherEntities() {
        var importer = importer(definition("down-service", "down"), definition("sample-service", "sample"));
        when(local.localServiceName("sample")).thenReturn("sample-service");
        when(local.hasLocalService("sample-service")).thenReturn(true);
        when(local.importData(eq("sample-service"), any())).thenReturn(new ImportResult(1, 1, 0, Map.of()));
        when(remote.importData(eq("down-service"), any())).thenThrow(new IllegalStateException("unavailable"));
        Map<String, Collection<?>> document = new LinkedHashMap<>();
        document.put("unknown", List.of(Map.of("id", "1")));
        document.put("down", List.of(Map.of("id", "2")));
        document.put("sample", List.of(Map.of("id", "3")));

        var result = importer.importData(document);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failures()).containsOnlyKeys("unknown", "down");
        assertThat(result.failures().get("down")).isEqualTo("unavailable");
        verify(local).importData(eq("sample-service"), any());
    }

    @Test
    void addressedTargetWithoutLocalImplementationFailsWithoutForwarding() {
        var importer = importer(definition("sample-service", "sample"));

        var result = importer.importData("sample-service", Map.of("sample", List.of(Map.of("id", "1"))));

        assertThat(result.failures()).containsOnlyKeys("sample");
        assertThat(result.failures().get("sample")).contains("未部署本地实体");
        verifyNoInteractions(remote);
    }

    @Test
    void localExecutionAndRemoteExecutionUseTheSameContractAndAggregateFailures() {
        var importer = importer(definition("remote-service", "remoteSample"));
        when(local.localServiceName("sample")).thenReturn("sample-service");
        when(local.hasLocalService("sample-service")).thenReturn(true);
        when(local.importData(eq("sample-service"), any()))
                .thenReturn(new ImportResult(1, 0, 1, Map.of()));
        when(remote.importData(eq("remote-service"), any()))
                .thenReturn(new ImportResult(1, 0, 0, Map.of("remoteSample", "invalid value")));
        Map<String, Collection<?>> document = new LinkedHashMap<>();
        document.put("sample", List.of(Map.of("id", "1")));
        document.put("remoteSample", List.of(Map.of("id", "2")));

        var result = importer.importData("entry-service", document);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failures()).containsEntry("remoteSample", "invalid value");
    }

    @Test
    void ambiguousEntityIsReportedWithoutSelectingAnArbitraryService() {
        var importer = importer(definition("first-service", "sample"), definition("second-service", "sample"));
        var result = importer.importData(Map.of("sample", List.of(Map.of("id", "1"))));

        assertThat(result.failures().get("sample")).contains("归属不唯一");
        verifyNoInteractions(remote);
    }

    private NocodeInitializationImporter importer(ServiceDefinition... definitions) {
        var registry = mock(ServiceDefinitionRegistry.class);
        when(registry.all()).thenReturn(List.of(definitions));
        return new NocodeInitializationImporter(local, remote, registry);
    }

    static ServiceDefinition definition(String serviceName, String resourceName) {
        var definition = mock(ServiceDefinition.class);
        when(definition.hasNocodeOperations()).thenReturn(true);
        when(definition.serviceName()).thenReturn(serviceName);
        when(definition.contextName()).thenReturn("sample");
        when(definition.key()).thenReturn(serviceName + "/sample/" + resourceName);
        when(definition.resourceName()).thenReturn(resourceName);
        return definition;
    }
}
