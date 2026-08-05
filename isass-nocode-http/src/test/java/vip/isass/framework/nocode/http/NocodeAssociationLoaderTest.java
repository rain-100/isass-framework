package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.EntityAssociation;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.service.IService;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NocodeAssociationLoaderTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void loadsRequestedCollectionAssociationWithTargetCriteria() {
        IService parentService = mock(IService.class);
        IService childService = mock(IService.class);
        when(parentService.service()).thenReturn("sample-service");
        when(parentService.entity()).thenReturn("sampleGroup");
        when(parentService.entityClass()).thenReturn(SampleGroup.class);
        when(childService.service()).thenReturn("sample-service");
        when(childService.entity()).thenReturn("sampleItem");
        when(childService.entityClass()).thenReturn(SampleItem.class);
        when(childService.criteriaClass()).thenReturn((Class) SampleItemCriteria.class);
        when(childService.findByCriteria(any(SampleItemCriteria.class))).thenAnswer(invocation -> {
            SampleItemCriteria criteria = invocation.getArgument(0, SampleItemCriteria.class);
            assertThat(criteria.getSampleGroupIds()).containsExactly(1L, 2L);
            assertThat(criteria.getEnabledFlag()).isEqualTo(1);
            return List.of(new SampleItem(11L, 1L, "first"), new SampleItem(12L, 2L, "second"));
        });

        SampleGroup first = new SampleGroup(1L);
        SampleGroup second = new SampleGroup(2L);
        LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("association.sampleItems.criteria.enabledFlag", "1");

        new NocodeAssociationLoader(new ServiceRegistry(List.of(parentService, childService)), new ObjectMapper())
                .load(parentService, List.of(first, second), query);

        assertThat(first.getSampleItems()).extracting(SampleItem::getName).containsExactly("first");
        assertThat(second.getSampleItems()).extracting(SampleItem::getName).containsExactly("second");
    }

    static class SampleGroup implements IIdEntity<Long, SampleGroup> {
        private Long id;
        private Collection<SampleItem> sampleItems;

        SampleGroup() {
        }

        SampleGroup(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public Collection<SampleItem> getSampleItems() {
            return sampleItems;
        }

        public void setSampleItems(Collection<SampleItem> sampleItems) {
            this.sampleItems = sampleItems;
        }

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("sampleItems", SampleItem.class, "id", "sampleGroupId"));
        }

        @Override
        public SampleGroup randomEntity() {
            return this;
        }
    }

    static class SampleItem implements IIdEntity<Long, SampleItem> {
        private Long id;
        private Long sampleGroupId;
        private String name;

        SampleItem() {
        }

        SampleItem(Long id, Long sampleGroupId, String name) {
            this.id = id;
            this.sampleGroupId = sampleGroupId;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public Long getSampleGroupId() {
            return sampleGroupId;
        }

        public void setSampleGroupId(Long sampleGroupId) {
            this.sampleGroupId = sampleGroupId;
        }

        public String getName() {
            return name;
        }

        @Override
        public SampleItem randomEntity() {
            return this;
        }
    }

    static class SampleItemCriteria implements ICriteria<SampleItem, SampleItemCriteria> {
        private Collection<Long> sampleGroupIds;
        private Integer enabledFlag;

        public SampleItemCriteria setSampleGroupIdIn(Collection<Long> sampleGroupIds) {
            this.sampleGroupIds = sampleGroupIds;
            return this;
        }

        public SampleItemCriteria setSampleGroupIdIn(Long... sampleGroupIds) {
            throw new AssertionError("Association loader must prefer the collection overload");
        }

        public Collection<Long> getSampleGroupIds() {
            return sampleGroupIds;
        }

        public SampleItemCriteria setEnabledFlag(Integer enabledFlag) {
            this.enabledFlag = enabledFlag;
            return this;
        }

        public Integer getEnabledFlag() {
            return enabledFlag;
        }
    }
}
