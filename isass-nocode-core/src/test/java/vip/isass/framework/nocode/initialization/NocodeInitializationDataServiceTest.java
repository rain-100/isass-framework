// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.repository.IRepository;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NocodeInitializationDataServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void importsOnlyAbsentRowsThroughTheLocalRepository() {
        IRepository<Sample, SampleCriteria> repository = mock(IRepository.class);
        Sample existing = new Sample();
        existing.setId(2L);
        when(repository.findByCriteria(any())).thenReturn(List.of(existing));
        NocodeInitializationDataService service = new NocodeInitializationDataService(
                List.of(new SampleService(repository)), new ObjectMapper());

        var result = service.importData("sample-service", Map.of("sample", List.of(
                Map.of("id", 1, "name", "first"),
                Map.of("id", 2, "name", "second"))));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failures()).isEmpty();
        assertThat(service.entities("sample-service"))
                .containsExactly(new NocodeInitializationDataService.EntityInfo("sample", "测试数据模型"));
        verify(repository).add(any(Sample.class));
    }

    @EntrypointInfo(serviceName = "sample-service", contextName = "sample", resourceName = "sample")
    interface SampleEntrypoint extends ILocalCrudService<Sample, SampleCriteria, Long> { }

    record SampleService(IRepository<Sample, SampleCriteria> repository) implements SampleEntrypoint {
        @Override
        public IRepository<Sample, SampleCriteria> getRepository() {
            return repository;
        }

        @Override
        public SampleCriteria newCriteria() {
            return new SampleCriteria();
        }
    }

    static final class Sample implements IIdEntity<Long, Sample> {
        public static final String COMMENT = "测试数据模型";
        private Long id;
        private String name;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        @Override public Sample randomEntity() { return this; }
    }

    static final class SampleCriteria extends FullTypeCriteria<Sample, SampleCriteria>
            implements IIdCriteria<Long, Sample, SampleCriteria> { }
}
