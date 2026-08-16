// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.AddIfAbsentItem;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.UpdateByCriteriaItem;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrudChangeExecutorTest {

    @Test
    void executesAllGroupsInFixedOrderAndReturnsAlignedResults() {
        RecordingRepository repository = new RecordingRepository();
        LocalService service = new LocalService(repository);
        Criteria addIfAbsentCriteria = new Criteria().setName("unique");
        Criteria updateCriteria = new Criteria().setName("editable");
        Criteria deleteCriteria = new Criteria().setName("obsolete");
        SuperCudReq<Entity, Criteria> request = new SuperCudReq<>(
                List.of(new Entity(1L, "add")),
                List.of(new AddIfAbsentItem<>(new Entity(2L, "conditional"), addIfAbsentCriteria)),
                List.of(new Entity(3L, "update")),
                List.of(new UpdateByCriteriaItem<>(List.of(new Entity(4L, "scoped")), updateCriteria)),
                List.of(5L),
                List.of(deleteCriteria));

        var result = new CrudChangeExecutor().superCud(service, request);

        assertEquals(List.of("add", "addIfAbsent", "updateId", "updateCriteria", "deleteIds", "deleteCriteria"),
                repository.operations);
        assertEquals(List.of(1L), result.addEntities().stream().map(Entity::getId).toList());
        assertTrue(result.addIfAbsentResults().getFirst().created());
        assertEquals(2L, result.addIfAbsentResults().getFirst().entity().getId());
        assertEquals(List.of(3L), result.updateEntities().stream().map(Entity::getId).toList());
        assertEquals(List.of(1), result.updateByCriteriaCounts());
        assertEquals(List.of(5L), result.deleteIds());
        assertEquals(List.of(2), result.deleteByCriteriaCounts());
        assertEquals(4L, repository.lastUpdateCriteria.getId());
        assertEquals("editable", repository.lastUpdateCriteria.getName());
    }

    @Test
    void validatesTheWholeChangeSetBeforeWriting() {
        RecordingRepository repository = new RecordingRepository();
        LocalService service = new LocalService(repository);
        SuperCudReq<Entity, Criteria> request = new SuperCudReq<>(
                null, null,
                List.of(new Entity(9L, "update")),
                null,
                List.of(9L),
                null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new CrudChangeExecutor().superCud(service, request));

        assertTrue(error.getMessage().contains("冲突 ID"));
        assertTrue(repository.operations.isEmpty());
    }

    @Test
    void acceptsAnEmptyChangeSetAsAnIdempotentNoOp() {
        RecordingRepository repository = new RecordingRepository();

        var result = new CrudChangeExecutor().superCud(new LocalService(repository), SuperCudReq.empty());

        assertTrue(result.addEntities().isEmpty());
        assertTrue(result.addIfAbsentResults().isEmpty());
        assertTrue(result.updateEntities().isEmpty());
        assertTrue(result.updateByCriteriaCounts().isEmpty());
        assertTrue(result.deleteIds().isEmpty());
        assertTrue(result.deleteByCriteriaCounts().isEmpty());
        assertTrue(repository.operations.isEmpty());
    }

    @Test
    void reportsExistingConditionalCreateAsANormalResult() {
        RecordingRepository repository = new RecordingRepository();
        repository.conditionalCreateSucceeds = false;
        repository.existing = new Entity(11L, "existing");

        var result = new CrudChangeExecutor().superCud(
                new LocalService(repository),
                SuperCudReq.addIfAbsent(new Entity(12L, "new"), new Criteria().setName("existing")));

        assertFalse(result.addIfAbsentResults().getFirst().created());
        assertEquals(11L, result.addIfAbsentResults().getFirst().entity().getId());
    }

    @Test
    void updatesEntitiesByIdWithoutRequiringPublicCriteria() {
        RecordingRepository repository = new RecordingRepository();

        var result = new CrudChangeExecutor().superCud(
                new LocalService(repository),
                SuperCudReq.updateByCriteria(List.of(new Entity(42L, "updated")), null));

        assertEquals(List.of(1), result.updateByCriteriaCounts());
        assertEquals(42L, repository.lastUpdateCriteria.getId());
    }

    @Test
    void rejectsAnIdlessUpdateWithoutEffectiveCriteriaBeforeWriting() {
        RecordingRepository repository = new RecordingRepository();

        assertThrows(IllegalArgumentException.class, () -> new CrudChangeExecutor().superCud(
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
        private boolean conditionalCreateSucceeds = true;
        private Entity existing;
        private Criteria lastUpdateCriteria;

        @Override
        public boolean add(Entity entity) {
            if (!conditionalCreateSucceeds || entity.getId() != null && entity.getId() == 2L) {
                operations.add("addIfAbsent");
                if (!conditionalCreateSucceeds) {
                    throw new org.springframework.dao.DuplicateKeyException("duplicate");
                }
            } else {
                operations.add("add");
            }
            return true;
        }

        @Override
        public Entity getByCriteria(vip.isass.framework.nocode.criteria.ICriteria<Entity, Criteria> criteria) {
            return existing;
        }

        @Override
        public boolean updateById(Entity entity) {
            operations.add("updateId");
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
