package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.repository.IRepository;
import vip.isass.framework.nocode.service.ILocalService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NocodeInitializationDataServiceTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void importsOnlyAbsentRowsAndKeepsDistributedIds() {
        ILocalService service = mock(ILocalService.class);
        IRepository repository = mock(IRepository.class);
        when(service.service()).thenReturn("sample-service");
        when(service.entity()).thenReturn("sample");
        when(service.entityClass()).thenReturn((Class) Sample.class);
        when(service.isPresentById(1L)).thenReturn(false);
        when(service.isPresentById(2L)).thenReturn(true);
        when(service.getRepository()).thenReturn(repository);

        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(service)), new ObjectMapper());
        NocodeInitializationDataService.ImportResult result = dataService.importData("sample-service", Map.of(
                "sample", List.of(Map.of("id", "1", "name", "first"), Map.of("id", "2", "name", "second"))));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failures()).isEmpty();
        verify(repository).add(any(Sample.class));
        verify(service, never()).add(any(Sample.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void continuesOtherEntitiesAndReportsEntityFailureForHttpImport() {
        ILocalService validService = mock(ILocalService.class);
        IRepository repository = mock(IRepository.class);
        when(validService.service()).thenReturn("sample-service");
        when(validService.entity()).thenReturn("sample");
        when(validService.entityClass()).thenReturn((Class) Sample.class);
        when(validService.isPresentById(1L)).thenReturn(false);
        when(validService.getRepository()).thenReturn(repository);

        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(validService)), new ObjectMapper());
        NocodeInitializationDataService.ImportResult result = dataService.importDataWithFailures("sample-service", Map.of(
                "sample", List.of(Map.of("id", "1", "name", "first")),
                "unknown", List.of(Map.of("id", "2"))));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failures()).containsKey("unknown");
        verify(repository).add(any(Sample.class));
        verify(validService, never()).add(any(Sample.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listsOnlyEntitiesImplementedByRequestedService() {
        ILocalService sampleService = mock(ILocalService.class);
        when(sampleService.service()).thenReturn("sample-service");
        when(sampleService.entity()).thenReturn("sample");
        when(sampleService.entityClass()).thenReturn((Class) Sample.class);
        ILocalService otherService = mock(ILocalService.class);
        when(otherService.service()).thenReturn("other-service");
        when(otherService.entity()).thenReturn("other");
        when(otherService.entityClass()).thenReturn((Class) Sample.class);

        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(sampleService, otherService)), new ObjectMapper());

        assertThat(dataService.entities("sample-service"))
                .containsExactly(new NocodeInitializationDataService.EntityInfo("sample", "测试数据模型"));
    }

    static class Sample implements IIdEntity<Long, Sample> {
        public static final String COMMENT = "测试数据模型";

        private Long id;
        private String name;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public Sample randomEntity() {
            return this;
        }
    }
}
