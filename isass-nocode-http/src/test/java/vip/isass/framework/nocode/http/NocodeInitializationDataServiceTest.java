package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.repository.IRepository;
import vip.isass.framework.nocode.service.ILocalService;

import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

class NocodeInitializationDataServiceTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void importsOnlyAbsentRowsAndKeepsDistributedIds() {
        ILocalService service = mock(ILocalService.class);
        IRepository repository = mock(IRepository.class);
        when(service.service()).thenReturn("sample-service");
        when(service.entity()).thenReturn("sample");
        when(service.entityClass()).thenReturn((Class) Sample.class);
        when(service.criteriaClass()).thenReturn((Class) SampleCriteria.class);
        Sample existing = new Sample();
        existing.setId(2L);
        when(service.findByCriteria(any())).thenReturn(List.of(existing));
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
        verify(service).findByCriteria(any());
        verify(service, never()).isPresentById(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void preloadsExistingIdsInBatchesOfOneHundred() {
        ILocalService service = mock(ILocalService.class);
        IRepository repository = mock(IRepository.class);
        when(service.service()).thenReturn("sample-service");
        when(service.entity()).thenReturn("sample");
        when(service.entityClass()).thenReturn((Class) Sample.class);
        when(service.criteriaClass()).thenReturn((Class) SampleCriteria.class);
        when(service.findByCriteria(any())).thenReturn(List.of());
        when(service.getRepository()).thenReturn(repository);

        List<Map<String, Object>> rows = IntStream.rangeClosed(1, 201)
                .mapToObj(id -> Map.<String, Object>of("id", id, "name", "sample-" + id))
                .toList();
        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(service)), new ObjectMapper());

        NocodeInitializationDataService.ImportResult result = dataService.importData(
                "sample-service", Map.of("sample", rows));

        assertThat(result.total()).isEqualTo(201);
        assertThat(result.inserted()).isEqualTo(201);
        var criteriaCaptor = forClass(SampleCriteria.class);
        verify(service, times(3)).findByCriteria(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getAllValues()).allSatisfy(criteria -> assertThat(criteria.getWhereConditions())
                .singleElement()
                .satisfies(condition -> assertThat((List<?>) condition.getValue()).hasSizeLessThanOrEqualTo(100)));
        assertThat(criteriaCaptor.getAllValues())
                .allSatisfy(criteria -> assertThat(criteria.getSelectColumns()).containsExactly("id"));
        verify(service, never()).isPresentById(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void continuesOtherEntitiesAndReportsEntityFailureForHttpImport() {
        ILocalService validService = mock(ILocalService.class);
        IRepository repository = mock(IRepository.class);
        when(validService.service()).thenReturn("sample-service");
        when(validService.entity()).thenReturn("sample");
        when(validService.entityClass()).thenReturn((Class) Sample.class);
        when(validService.criteriaClass()).thenReturn((Class) SampleCriteria.class);
        when(validService.findByCriteria(any())).thenReturn(List.of());
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

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void recognizesWhenTargetServiceIsImplementedLocally() {
        ILocalService sampleService = mock(ILocalService.class);
        when(sampleService.service()).thenReturn("sample-service");
        when(sampleService.entity()).thenReturn("sample");
        when(sampleService.entityClass()).thenReturn((Class) Sample.class);

        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(sampleService)), new ObjectMapper());

        assertThat(dataService.hasLocalService("sample-service")).isTrue();
        assertThat(dataService.hasLocalService("other-service")).isFalse();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void importsTargetServiceDataLocallyWhenItsImplementationIsPresent() throws Exception {
        ILocalService sampleService = mock(ILocalService.class);
        IRepository repository = mock(IRepository.class);
        when(sampleService.service()).thenReturn("sample-service");
        when(sampleService.entity()).thenReturn("sample");
        when(sampleService.entityClass()).thenReturn((Class) Sample.class);
        when(sampleService.criteriaClass()).thenReturn((Class) SampleCriteria.class);
        when(sampleService.findByCriteria(any())).thenReturn(List.of());
        when(sampleService.getRepository()).thenReturn(repository);

        Path initDirectory = Files.createTempDirectory("nocode-init").resolve("init/sample-service");
        Files.createDirectories(initDirectory);
        Files.writeString(initDirectory.resolve("sample.json"), "{\"sample\":[{\"id\":1,\"name\":\"first\"}]}");
        NocodeInitializationProperties properties = new NocodeInitializationProperties();
        properties.setLocation("file:" + initDirectory.getParent().getParent() + "/**/*.json");
        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(sampleService)), new ObjectMapper());
        NocodeInitializationRemoteClient remoteClient = mock(NocodeInitializationRemoteClient.class);

        new NocodeInitializationRunner(dataService, remoteClient, properties,
                new ContractRegistry(List.of(new ServiceContract("sample-service", "sample", "ISampleService",
                        Sample.class.getName(), SampleCriteria.class.getName(), List.of()))))
                .runner().run(new DefaultApplicationArguments());

        verify(repository).add(any(Sample.class));
        verify(remoteClient, never()).importData(any(), any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void routesEachEntityByItsContractInsteadOfTheInitDirectoryName() throws Exception {
        ILocalService sampleService = mock(ILocalService.class);
        IRepository repository = mock(IRepository.class);
        when(sampleService.service()).thenReturn("sample-service");
        when(sampleService.entity()).thenReturn("sample");
        when(sampleService.entityClass()).thenReturn((Class) Sample.class);
        when(sampleService.criteriaClass()).thenReturn((Class) SampleCriteria.class);
        when(sampleService.findByCriteria(any())).thenReturn(List.of());
        when(sampleService.getRepository()).thenReturn(repository);

        Path initDirectory = Files.createTempDirectory("nocode-init").resolve("init/asset-service");
        Files.createDirectories(initDirectory);
        Files.writeString(initDirectory.resolve("mixed.json"),
                "{\"sample\":[{\"id\":1,\"name\":\"local\"}],\"remote\":[{\"id\":2}]} ");
        NocodeInitializationProperties properties = new NocodeInitializationProperties();
        properties.setLocation("file:" + initDirectory.getParent().getParent() + "/**/*.json");
        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(sampleService)), new ObjectMapper());
        NocodeInitializationRemoteClient remoteClient = mock(NocodeInitializationRemoteClient.class);
        when(remoteClient.importData(any(), any()))
                .thenReturn(new NocodeInitializationDataService.ImportResult(1, 1, 0, Map.of()));
        ContractRegistry contracts = new ContractRegistry(List.of(
                new ServiceContract("sample-service", "sample", "ISampleService",
                        Sample.class.getName(), SampleCriteria.class.getName(), List.of()),
                new ServiceContract("remote-service", "remote", "IRemoteService",
                        Object.class.getName(), Object.class.getName(), List.of())
        ));

        new NocodeInitializationRunner(dataService, remoteClient, properties, contracts)
                .runner().run(new DefaultApplicationArguments());

        verify(repository).add(any(Sample.class));
        verify(remoteClient).importData(eq("remote-service"), eq(Map.of("remote", List.of(Map.of("id", 2)))));
        verify(remoteClient, never()).importData(eq("asset-service"), any());
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

    static class SampleCriteria extends FullTypeCriteria<Sample, SampleCriteria>
            implements IIdCriteria<Long, Sample, SampleCriteria>, ICriteria<Sample, SampleCriteria> {
    }
}
