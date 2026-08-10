// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.service.ILocalService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NocodeExportServiceTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void exportsPlansWithCriteriaAndReferencesToEarlierEntities() {
        ILocalService typeService = mock(ILocalService.class);
        when(typeService.service()).thenReturn("bsp-service");
        when(typeService.entity()).thenReturn("DictionaryType");
        when(typeService.entityClass()).thenReturn((Class) DictionaryType.class);
        when(typeService.criteriaClass()).thenReturn((Class) DictionaryTypeCriteria.class);
        DictionaryType dictionaryType = new DictionaryType();
        dictionaryType.setId(100L);
        when(typeService.findByCriteria(any())).thenReturn(List.of(dictionaryType));

        ILocalService itemService = mock(ILocalService.class);
        when(itemService.service()).thenReturn("bsp-service");
        when(itemService.entity()).thenReturn("DictionaryItem");
        when(itemService.entityClass()).thenReturn((Class) DictionaryItem.class);
        when(itemService.criteriaClass()).thenReturn((Class) DictionaryItemCriteria.class);
        DictionaryItem dictionaryItem = new DictionaryItem();
        dictionaryItem.setId(200L);
        when(itemService.findByCriteria(any())).thenReturn(List.of(dictionaryItem));

        ObjectMapper objectMapper = new ObjectMapper();
        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(typeService, itemService)), objectMapper);
        NocodeExportService exportService = new NocodeExportService(
                dataService, null, new NocodeExportProfileLoader(objectMapper), objectMapper);

        NocodeExportPlan typePlan = new NocodeExportPlan();
        typePlan.setService("bsp-service");
        typePlan.setEntity("DictionaryType");
        typePlan.setCriteria(Map.of("bizType", "iimage_asset"));
        NocodeExportPlan itemPlan = new NocodeExportPlan();
        itemPlan.setService("bsp-service");
        itemPlan.setEntity("DictionaryItem");
        itemPlan.setCriteria(Map.of("dictionaryTypeIdIn", "${export.DictionaryType.id}"));

        NocodeExportRequest request = new NocodeExportRequest();
        request.setPlans(List.of(typePlan, itemPlan));

        NocodeExportPackage result = exportService.export("bsp-service", request);

        assertThat(result.services()).containsEntry("bsp-service", Map.of(
                "DictionaryType", List.of(dictionaryType),
                "DictionaryItem", List.of(dictionaryItem)));
        var typeCriteria = forClass(DictionaryTypeCriteria.class);
        var itemCriteria = forClass(DictionaryItemCriteria.class);
        verify(typeService).findByCriteria(typeCriteria.capture());
        verify(itemService).findByCriteria(itemCriteria.capture());
        assertThat(typeCriteria.getValue().getBizType()).isEqualTo("iimage_asset");
        assertThat(itemCriteria.getValue().getDictionaryTypeIdIn()).containsExactly(100L);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void skipsChildPlanWhenTheReferencedParentExportIsEmpty() {
        ILocalService typeService = mock(ILocalService.class);
        when(typeService.service()).thenReturn("bsp-service");
        when(typeService.entity()).thenReturn("DictionaryType");
        when(typeService.entityClass()).thenReturn((Class) DictionaryType.class);
        when(typeService.criteriaClass()).thenReturn((Class) DictionaryTypeCriteria.class);
        when(typeService.findByCriteria(any())).thenReturn(List.of());

        ILocalService itemService = mock(ILocalService.class);
        when(itemService.service()).thenReturn("bsp-service");
        when(itemService.entity()).thenReturn("DictionaryItem");
        when(itemService.entityClass()).thenReturn((Class) DictionaryItem.class);
        when(itemService.criteriaClass()).thenReturn((Class) DictionaryItemCriteria.class);

        ObjectMapper objectMapper = new ObjectMapper();
        NocodeInitializationDataService dataService = new NocodeInitializationDataService(
                new ServiceRegistry(List.of(typeService, itemService)), objectMapper);
        NocodeExportService exportService = new NocodeExportService(
                dataService, null, new NocodeExportProfileLoader(objectMapper), objectMapper);

        NocodeExportPlan typePlan = new NocodeExportPlan();
        typePlan.setService("bsp-service");
        typePlan.setEntity("DictionaryType");
        typePlan.setCriteria(Map.of("bizType", "missing"));
        NocodeExportPlan itemPlan = new NocodeExportPlan();
        itemPlan.setService("bsp-service");
        itemPlan.setEntity("DictionaryItem");
        itemPlan.setCriteria(Map.of("dictionaryTypeIdIn", "${export.DictionaryType.id}"));
        NocodeExportRequest request = new NocodeExportRequest();
        request.setPlans(List.of(typePlan, itemPlan));

        NocodeExportPackage result = exportService.export("bsp-service", request);

        assertThat(result.services().get("bsp-service").get("DictionaryItem")).isEmpty();
        verify(itemService, never()).findByCriteria(any());
    }

    static class DictionaryType implements IIdEntity<Long, DictionaryType> {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    static class DictionaryItem implements IIdEntity<Long, DictionaryItem> {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    static class DictionaryTypeCriteria extends FullTypeCriteria<DictionaryType, DictionaryTypeCriteria> {
        private String bizType;
        public DictionaryTypeCriteria setBizType(String bizType) { this.bizType = bizType; return this; }
        public String getBizType() { return bizType; }
    }

    static class DictionaryItemCriteria extends FullTypeCriteria<DictionaryItem, DictionaryItemCriteria> {
        private List<Long> dictionaryTypeIdIn;
        public DictionaryItemCriteria setDictionaryTypeIdIn(List<Long> dictionaryTypeIdIn) {
            this.dictionaryTypeIdIn = dictionaryTypeIdIn;
            return this;
        }
        public List<Long> getDictionaryTypeIdIn() { return dictionaryTypeIdIn; }
    }
}
