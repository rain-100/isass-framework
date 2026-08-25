// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrudWriteExecutorTest {

    @Test
    void executesAllGroupsInFixedOrderAndReturnsAggregateCounts() {
        RecordingRepository repository = new RecordingRepository();
        LocalService service = new LocalService(repository);
        Criteria updateCriteria = new Criteria().lessThan(Entity::getId, 100L)
                .setMatchFields(List.of("name"));
        Criteria deleteCriteria = new Criteria().setName("obsolete");
        SuperCudReq<Entity, Criteria> request = new SuperCudReq<>(
                List.of(new Entity(2L, "conditional")),
                List.of("name"),
                List.of(new Entity(4L, "scoped")),
                updateCriteria,
                List.of(5L),
                List.of(deleteCriteria));

        var result = new CrudWriteExecutor().superCud(service, request);

        assertEquals(List.of("exists", "add", "updateCriteria", "deleteIds", "deleteCriteria"),
                repository.operations);
        assertEquals(1, result.addedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(3, result.deletedCount());
        assertEquals(null, repository.lastUpdateCriteria.getId());
        assertEquals("scoped", repository.lastUpdateCriteria.getName());
        assertEquals(100L, repository.lastUpdateCriteria.getLessThan("id", Long.class));
    }

    @Test
    void validatesTheWholeChangeSetBeforeWriting() {
        RecordingRepository repository = new RecordingRepository();
        LocalService service = new LocalService(repository);
        SuperCudReq<Entity, Criteria> request = new SuperCudReq<>(
                List.of(new Entity(1L, "add")), null,
                List.of(new Entity(null, "update")), null,
                null, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new CrudWriteExecutor().superCud(service, request));

        assertTrue(error.getMessage().contains("updateEntities"));
        assertTrue(repository.operations.isEmpty());
    }

    @Test
    void acceptsAnEmptyChangeSetAsAnIdempotentNoOp() {
        RecordingRepository repository = new RecordingRepository();

        var result = new CrudWriteExecutor().superCud(new LocalService(repository), SuperCudReq.empty());

        assertEquals(0, result.addedCount());
        assertEquals(0, result.updatedCount());
        assertEquals(0, result.deletedCount());
        assertTrue(repository.operations.isEmpty());
    }

    @Test
    void reportsExistingConditionalCreateAsANormalResult() {
        RecordingRepository repository = new RecordingRepository();
        repository.existingByCriteria = true;

        var result = new CrudWriteExecutor().superCud(
                new LocalService(repository),
                SuperCudReq.<Entity, Criteria>builder()
                        .addEntity(new Entity(12L, "new"))
                        .addByFields(Entity::getName)
                        .build());

        assertEquals(0, result.addedCount());
        assertEquals(List.of("exists"), repository.operations);
    }

    @Test
    void updatesEntitiesByIdWithoutRequiringPublicCriteria() {
        RecordingRepository repository = new RecordingRepository();

        var result = new CrudWriteExecutor().superCud(
                new LocalService(repository),
                SuperCudReq.update(new Entity(42L, "updated")));

        assertEquals(1, result.updatedCount());
        assertEquals(42L, repository.lastUpdatedId);
    }

    @Test
    void allowsMultipleEntitiesToUseTheSameRangeCriteria() {
        RecordingRepository repository = new RecordingRepository();
        Criteria criteria = new Criteria().lessThan(Entity::getId, 100L);

        var result = new CrudWriteExecutor().superCud(
                new LocalService(repository),
                SuperCudReq.updateByCriteria(List.of(
                        new Entity(null, "first"),
                        new Entity(null, "second")), criteria));

        assertEquals(2, result.updatedCount());
        assertEquals(List.of("updateCriteria", "updateCriteria"), repository.operations);
    }

    @Test
    void rejectsAnIdlessUpdateWithoutEffectiveCriteriaBeforeWriting() {
        RecordingRepository repository = new RecordingRepository();

        assertThrows(IllegalArgumentException.class, () -> new CrudWriteExecutor().superCud(
                new LocalService(repository),
                SuperCudReq.updateByCriteria(List.of(new Entity(null, "updated")), null)));

        assertTrue(repository.operations.isEmpty());
    }

    static final class LocalService implements ILocalCrudService<Entity, Criteria, Long> {

        private final RecordingRepository repository;

        LocalService(RecordingRepository repository) {
            this.repository = repository;
        }

        @Override
        public IRepository<Entity, Criteria> getRepository() {
            return repository;
        }

        @Override
        public Criteria newCriteria() {
            return new Criteria();
        }
    }

    static final class RecordingRepository implements IRepository<Entity, Criteria> {

        private final List<String> operations = new ArrayList<>();
        private boolean existingByCriteria;
        private Criteria lastUpdateCriteria;
        private Long lastUpdatedId;

        @Override
        public boolean add(Entity entity) {
            operations.add("add");
            return true;
        }

        @Override
        public boolean isPresentByCriteria(
                vip.isass.framework.nocode.criteria.ICriteria<Entity, Criteria> criteria) {
            operations.add("exists");
            return existingByCriteria;
        }

        @Override
        public boolean updateById(Entity entity) {
            operations.add("updateId");
            lastUpdatedId = entity.getId();
            return true;
        }

        @Override
        public int updateCountByCriteria(
                Entity entity,
                vip.isass.framework.nocode.criteria.ICriteria<Entity, Criteria> criteria
        ) {
            operations.add("updateCriteria");
            lastUpdateCriteria = (Criteria) criteria;
            return 1;
        }

        @Override
        public int deleteCountByIds(Collection<? extends Serializable> ids) {
            operations.add("deleteIds");
            return ids.size();
        }

        @Override
        public int deleteCountByCriteria(vip.isass.framework.nocode.criteria.ICriteria<Entity, Criteria> criteria) {
            operations.add("deleteCriteria");
            return 2;
        }
    }

    static final class Entity implements IIdEntity<Long, Entity> {

        private Long id;
        private String name;

        Entity(Long id, String name) {
            this.id = id;
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static final class Criteria extends FullTypeCriteria<Entity, Criteria>
            implements IIdCriteria<Long, Entity, Criteria> {

        public String getName() {
            return getEquals("name", String.class);
        }

        public Criteria setName(String name) {
            return equals("name", name);
        }
    }
}
