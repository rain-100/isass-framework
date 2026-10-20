// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.common.criteria.field.IIdCriteria;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Collection;
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
    void unknownEntityIsReportedAndTheNextEntityIsStillImported() {
        IRepository<Sample, SampleCriteria> repository = mock(IRepository.class);
        when(repository.findByCriteria(any())).thenReturn(List.of());
        var service = new NocodeInitializationDataService(List.of(new SampleService(repository)));
        Map<String, Collection<?>> document = new LinkedHashMap<>();
        document.put("unknown", List.of(Map.of("id", "10")));
        document.put("sample", List.of(Map.of("id", "1", "name", "valid")));

        var result = service.importData("sample-service", document);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failures()).containsOnlyKeys("unknown");
        verify(repository).add(any(Sample.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void conversionFailureDoesNotBlockTheNextEntity() {
        IRepository<Sample, SampleCriteria> repository = mock(IRepository.class);
        when(repository.findByCriteria(any())).thenReturn(List.of());
        var service = new NocodeInitializationDataService(
                List.of(new SampleService(repository), new OtherSampleService(repository)));
        Map<String, Collection<?>> document = new LinkedHashMap<>();
        document.put("sample", List.of(Map.of("id", "not-a-long"), Map.of("id", "2")));
        document.put("otherSample", List.of(Map.of("id", "3", "name", "valid")));

        var result = service.importData("sample-service", document);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failures()).containsOnlyKeys("sample");
        var inserted = ArgumentCaptor.forClass(Sample.class);
        verify(repository).add(inserted.capture());
        assertThat(inserted.getValue().getId()).isEqualTo(3L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void excelRowsUseTheSameInsertOnlyAndDuplicateIdRules() throws Exception {
        IRepository<Sample, SampleCriteria> repository = mock(IRepository.class);
        Sample existing = new Sample();
        existing.setId(2L);
        existing.setName("用户修改后的名称");
        when(repository.findByCriteria(any())).thenReturn(List.of(existing));
        var service = new NocodeInitializationDataService(List.of(new SampleService(repository)));
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("sample");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("name");
            sheet.createRow(1).createCell(0).setCellValue("主键");
            String[] ids = {"2081264465596485633", "2", "2081264465596485633"};
            for (int index = 0; index < ids.length; index++) {
                var row = sheet.createRow(index + 2);
                row.createCell(0).setCellValue(ids[index]);
                row.createCell(1).setCellValue("初始化名称");
            }
            var output = new ByteArrayOutputStream();
            workbook.write(output);
            var document = service.readDocument(new ByteArrayInputStream(output.toByteArray()), "data.xlsx");
            var result = service.importData("sample-service", document);
            assertThat(result.total()).isEqualTo(3);
            assertThat(result.inserted()).isEqualTo(1);
            assertThat(result.skipped()).isEqualTo(2);
            assertThat(existing.getName()).isEqualTo("用户修改后的名称");
            var inserted = ArgumentCaptor.forClass(Sample.class);
            verify(repository).add(inserted.capture());
            assertThat(inserted.getValue().getId()).isEqualTo(2081264465596485633L);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void importsOnlyAbsentRowsThroughTheLocalRepository() {
        IRepository<Sample, SampleCriteria> repository = mock(IRepository.class);
        Sample existing = new Sample();
        existing.setId(2L);
        when(repository.findByCriteria(any())).thenReturn(List.of(existing));
        NocodeInitializationDataService service = new NocodeInitializationDataService(
                List.of(new SampleService(repository)));

        var result = service.importData("sample-service", Map.of("sample", List.of(
                Map.of("id", 1, "name", "first"),
                Map.of("id", 2, "name", "second"))));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failures()).isEmpty();
        assertThat(service.entities("sample-service"))
                .containsExactly(new NocodeInitializationDataService.EntityInfo(
                        "sample-service", "sample", "sample", "测试数据模型"));
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

    @EntrypointInfo(serviceName = "sample-service", contextName = "sample", resourceName = "otherSample")
    interface OtherSampleEntrypoint extends ILocalCrudService<Sample, SampleCriteria, Long> {
    }

    record OtherSampleService(IRepository<Sample, SampleCriteria> repository) implements OtherSampleEntrypoint {
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
