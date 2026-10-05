// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.EmptyCriteria;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import java.util.Objects;
import vip.isass.framework.common.criteria.UpdateMode;
import vip.isass.framework.common.criteria.field.IIdCriteria;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.nocode.entity.SuperCudReq;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssociationCoordinatorTest {
    private CrudQueryExecutorProvider queryProvider;

    @AfterEach
    void releaseQueryProvider() { if (queryProvider != null) queryProvider.destroy(); }


    @Test
    void savesAndReplacesSubmittedDirectAssociations() {
        ParentRepository parents = new ParentRepository();
        ChildRepository children = new ChildRepository();
        ParentService parentService = new ParentService(parents);
        ChildService childService = new ChildService(children);
        AssociationWriteCoordinator coordinator = new AssociationWriteCoordinator(
                List.of(parentService, childService));
        Parent parent = new Parent(1L);
        Child retained = new Child(10L, 1L, "updated");
        Child removed = new Child(11L, 1L, "removed");
        children.rows.put(10L, new Child(10L, 1L, "old"));
        children.rows.put(11L, removed);
        Child added = new Child(null, null, "new");
        parent.setChildren(List.of(retained, added));

        coordinator.afterSave(parent, new ParentCriteria().setUpdateMode(UpdateMode.REPLACE), false);

        assertEquals(1L, added.getParentId());
        assertEquals("updated", children.rows.get(10L).getName());
        assertEquals(List.of(10L, 12L), new ArrayList<>(children.rows.keySet()));
    }

    @Test
    void updatesExistingOneToOneAssociationByRelationKeyWhenIdIsOmitted() {
        ParentRepository parents = new ParentRepository();
        DetailRepository details = new DetailRepository();
        Detail existing = new Detail(100L, "old");
        existing.setParentId(1L);
        details.rows.put(existing.getId(), existing);
        AssociationWriteCoordinator coordinator = new AssociationWriteCoordinator(
                List.of(new ParentService(parents), new DetailService(details)));
        Parent parent = new Parent(1L);
        Detail submitted = new Detail(null, "updated");
        parent.setProfile(submitted);

        coordinator.afterSave(parent, null, false);

        assertEquals(100L, submitted.getId());
        assertEquals(1L, submitted.getParentId());
        assertSame(submitted, details.rows.get(100L));
        assertEquals("updated", details.rows.get(100L).getName());
    }

    @Test
    void criteriaIdControlsIdlessRootAssociationUpdate() {
        ParentRepository parents = new ParentRepository();
        ChildRepository children = new ChildRepository();
        children.rows.put(10L, new Child(10L, 1L, "old"));
        ParentService parentService = new ParentService(parents);
        AssociationWriteCoordinator coordinator = new AssociationWriteCoordinator(
                List.of(parentService, new ChildService(children)));
        Parent submitted = new Parent(null);
        submitted.setChildren(List.of(new Child(10L, null, "new")));

        var result = new CrudWriteExecutor(coordinator).superCud(parentService,
                vip.isass.framework.nocode.entity.SuperCudReq.updateByCriteria(
                        List.of(submitted), new ParentCriteria().setId(1L)));

        assertEquals(1, result.updatedCount());
        assertEquals(1L, children.rows.get(10L).getParentId());
        assertEquals("new", children.rows.get(10L).getName());
        assertThrows(IllegalArgumentException.class, () -> new CrudWriteExecutor(coordinator).superCud(parentService,
                vip.isass.framework.nocode.entity.SuperCudReq.updateByCriteria(
                        List.of(submitted), new ParentCriteria().equals("profile", null))));
    }

    @Test
    void missingRootDoesNotWriteSubmittedAssociation() {
        ParentRepository parents = new ParentRepository();
        ChildRepository children = new ChildRepository();
        children.rows.put(10L, new Child(10L, 1L, "old"));
        ParentService parentService = new ParentService(parents);
        AssociationWriteCoordinator coordinator = new AssociationWriteCoordinator(
                List.of(parentService, new ChildService(children)));
        Parent submitted = new Parent(null);
        submitted.setChildren(List.of(new Child(10L, null, "new")));

        assertThrows(AbsentException.class, () -> new CrudWriteExecutor(coordinator).superCud(parentService,
                SuperCudReq.updateByCriteria(List.of(submitted), new ParentCriteria().setId(2L))));

        assertEquals("old", children.rows.get(10L).getName());
        assertEquals(1L, children.rows.get(10L).getParentId());
    }

    @Test
    void cascadeDeleteBatchesOnlyDeclaredChildren() {
        ParentRepository parents = new ParentRepository();
        parents.rows.put(1L, new Parent(1L));
        ChildRepository children = new ChildRepository();
        children.rows.put(10L, new Child(10L, 1L, "a"));
        children.rows.put(11L, new Child(11L, 1L, "b"));
        children.rows.put(20L, new Child(20L, 2L, "unrelated"));
        DetailRepository details = new DetailRepository();
        Detail profile = new Detail(100L, "retained");
        profile.setParentId(1L);
        details.rows.put(100L, profile);
        ParentService parentService = new ParentService(parents);
        AssociationWriteCoordinator coordinator = new AssociationWriteCoordinator(
                List.of(parentService, new ChildService(children), new DetailService(details)));

        coordinator.beforeDelete(parentService, List.of(1L));

        assertEquals(List.of(20L), new ArrayList<>(children.rows.keySet()));
        assertEquals(List.of(100L), new ArrayList<>(details.rows.keySet()));
    }

    @Test
    void loadsRequestedAssociationsInOneTargetQuery() {
        ParentRepository parents = new ParentRepository();
        ChildRepository children = new ChildRepository();
        children.rows.put(10L, new Child(10L, 1L, "one"));
        children.rows.put(20L, new Child(20L, 2L, "two"));
        AssociationQueryCoordinator coordinator = new AssociationQueryCoordinator(List.of(
                new ParentService(parents), new ChildService(children)));
        queryProvider = new CrudQueryExecutorProvider(new CrudQueryExecutor(coordinator, List.of()));
        Parent first = new Parent(1L);
        Parent second = new Parent(2L);
        ParentCriteria criteria = new ParentCriteria().loadRelated("children");

        coordinator.populate(List.of(first, second), criteria);

        assertEquals(List.of(10L), first.getChildren().stream().map(Child::getId).toList());
        assertEquals(List.of(20L), second.getChildren().stream().map(Child::getId).toList());
        assertEquals(1, children.queryCount);
    }

    @Test
    void loadsExplicitNestedPathAndAutomaticallyLoadsItsParentPath() {
        ParentRepository parents = new ParentRepository();
        ChildRepository children = new ChildRepository();
        DetailRepository details = new DetailRepository();
        Child firstChild = new Child(10L, 1L, "one");
        firstChild.setDetailId(100L);
        Child secondChild = new Child(20L, 2L, "two");
        secondChild.setDetailId(200L);
        children.rows.put(10L, firstChild);
        children.rows.put(20L, secondChild);
        details.rows.put(100L, new Detail(100L, "first detail"));
        details.rows.put(200L, new Detail(200L, "second detail"));
        AssociationQueryCoordinator coordinator = new AssociationQueryCoordinator(List.of(
                new ParentService(parents), new ChildService(children), new DetailService(details)));
        queryProvider = new CrudQueryExecutorProvider(new CrudQueryExecutor(coordinator, List.of()));
        Parent first = new Parent(1L);
        Parent second = new Parent(2L);
        ParentCriteria criteria = new ParentCriteria()
                .loadRelated("children", new ChildCriteria().loadRelated("detail"));

        coordinator.populate(List.of(first, second), criteria);

        assertEquals(List.of(10L), first.getChildren().stream().map(Child::getId).toList());
        assertEquals("first detail", first.getChildren().iterator().next().getDetail().getName());
        assertEquals(List.of(20L), second.getChildren().stream().map(Child::getId).toList());
        assertEquals("second detail", second.getChildren().iterator().next().getDetail().getName());
        assertEquals(1, children.queryCount);
        assertEquals(1, details.queryCount);
    }

    @Test
    void reusesOneCriteriaAndInNodeAcrossBatchesWithoutReplacingBusinessFilters() {
        ChildRepository children = mock(ChildRepository.class);
        AssociationQueryCoordinator coordinator = new AssociationQueryCoordinator(List.of(
                new ParentService(new ParentRepository()), new ChildService(children)));
        queryProvider = new CrudQueryExecutorProvider(new CrudQueryExecutor(coordinator, List.of()));
        ChildCriteria filters = new ChildCriteria().in("parentId", List.of(1L, 1001L))
                .orEquals("name", "special").setReturnField("name");
        List<WhereCondition> business = filters.getWhereConditions();
        ParentCriteria query = new ParentCriteria().loadRelated("children", filters);
        List<Parent> parents = LongStream.rangeClosed(1, 1001).mapToObj(Parent::new).toList();
        AtomicReference<WhereCondition> batchNode = new AtomicReference<>();
        List<Integer> batchSizes = new ArrayList<>();
        List<Child> rows = List.of(new Child(10L, 1L, "one"), new Child(11L, 1L, "two"),
                new Child(20L, 2L, "excluded"), new Child(30L, 1001L, "last"));
        when(children.findPageByCriteria(any())).thenAnswer(invocation -> {
            assertSame(filters, invocation.getArgument(0));
            assertEquals(2, filters.getWhereConditions().size());
            assertSame(business, filters.getWhereConditions().getFirst().getChildren());
            assertEquals(List.of(1L, 1001L), business.getFirst().getValue());
            WhereCondition in = filters.getWhereConditions().getLast();
            if (batchNode.get() == null) batchNode.set(in);
            else assertSame(batchNode.get(), in);
            Collection<?> ids = (Collection<?>) in.getValue();
            if (filters.getPageNum() == 1L) batchSizes.add(ids.size());
            Collection<?> allowedIds = (Collection<?>) business.getFirst().getValue();
            List<Child> matched = rows.stream().filter(row -> ids.contains(row.getParentId()))
                    .filter(row -> allowedIds.contains(row.getParentId()) || "special".equals(row.getName()))
                    .skip(filters.getPageNum() - 1).limit(1).toList();
            // 模拟 ORM 把页大小缩小为 1；同一批必须读取多页。
            return Page.of(matched, filters.getPageNum(), 1, 0);
        });

        coordinator.prepare(query);
        coordinator.populate(parents, query);
        assertEquals(List.of(1000, 1), batchSizes);
        assertEquals(List.of(10L, 11L), parents.getFirst().getChildren().stream().map(Child::getId).toList());
        assertTrue(parents.get(1).getChildren().isEmpty());
        assertEquals(List.of(30L), parents.getLast().getChildren().stream().map(Child::getId).toList());
        assertSame(business, filters.getWhereConditions());
        assertEquals(20L, filters.getPageSize());
        assertEquals(1L, filters.getPageNum());
        assertTrue(filters.getSearchCountFlag());
        assertTrue(filters.getReturnFields().containsAll(List.of("name", "id", "parentId")));

        // 同一请求再次执行，不带前一批 ID，也不会把内部分页误判为业务分页。
        batchNode.set(null);
        coordinator.prepare(query);
        coordinator.populate(List.of(parents.getFirst()), query);
        assertEquals(List.of(1000, 1, 1), batchSizes);
        assertEquals(2, parents.getFirst().getChildren().size());
        assertSame(business, filters.getWhereConditions());
    }

    @Test
    void failedBatchCleansOnlyTemporaryStateAndMaterializesEmptyCriteriaOnce() {
        ChildRepository children = mock(ChildRepository.class);
        AssociationQueryCoordinator coordinator = new AssociationQueryCoordinator(List.of(
                new ParentService(new ParentRepository()), new ChildService(children)));
        queryProvider = new CrudQueryExecutorProvider(new CrudQueryExecutor(coordinator, List.of()));
        ParentCriteria query = new ParentCriteria().loadRelated("children", EmptyCriteria.of(Child.class));
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicReference<ChildCriteria> executing = new AtomicReference<>();
        when(children.findPageByCriteria(any())).thenAnswer(invocation -> {
            ChildCriteria criteria = invocation.getArgument(0);
            if (executing.get() == null) executing.set(criteria);
            else assertSame(executing.get(), criteria);
            if (fail.getAndSet(false)) {
                criteria.equals("name", "retained");
                throw new IllegalStateException("storage unavailable");
            }
            return Page.of(List.of(new Child(10L, 1L, "retained")), 1, 1000, 0);
        });
        Parent parent = new Parent(1L);
        assertThrows(IllegalStateException.class, () -> coordinator.populate(List.of(parent), query));
        ChildCriteria filters = executing.get();
        assertSame(filters, query.getLoadRelated().getFirst().getCriteria());
        assertEquals("retained", filters.getEquals("name"));
        assertEquals(1, filters.getWhereConditions().size());
        assertEquals(Condition.Compare.EQUAL, filters.getWhereConditions().getFirst().getCondition());
        assertEquals(20L, filters.getPageSize());
        coordinator.prepare(query);
        coordinator.populate(List.of(parent), query);
        assertEquals(List.of(10L), parent.getChildren().stream().map(Child::getId).toList());
        filters.setPageSize(50L);
        assertThrows(IllegalArgumentException.class, () -> coordinator.prepare(query));
    }

    static final class Parent implements IIdEntity<Long, Parent> {
        private Long id;
        private Collection<Child> children;
        private Detail profile;

        Parent() { }
        Parent(Long id) { this.id = id; }
        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public Collection<Child> getChildren() { return children; }
        public void setChildren(Collection<Child> children) {
            this.children = children;
            markPresentProperty("children");
        }
        public Detail getProfile() { return profile; }
        public void setProfile(Detail profile) {
            this.profile = profile;
            markPresentProperty("profile");
        }
        @Override public List<EntityAssociation> associations() {
            return List.of(
                    EntityAssociation.many("children", Child.class,
                            "id", "parentId", true),
                    EntityAssociation.one("profile", Detail.class,
                            "id", "parentId", false)
            );
        }
    }

    static final class Child implements IIdEntity<Long, Child> {
        private Long id;
        private Long parentId;
        private String name;
        private Long detailId;
        private Detail detail;

        Child() { }
        Child(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }
        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getDetailId() { return detailId; }
        public void setDetailId(Long detailId) { this.detailId = detailId; }
        public Detail getDetail() { return detail; }
        public void setDetail(Detail detail) { this.detail = detail; }
        @Override public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.one("detail", Detail.class,
                    "detailId", "id", false));
        }
    }

    static final class Detail implements IIdEntity<Long, Detail> {
        private Long id;
        private Long parentId;
        private String name;

        Detail() { }
        Detail(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    static final class ParentCriteria extends FullTypeCriteria<Parent, ParentCriteria>
            implements IIdCriteria<Long, Parent, ParentCriteria> {
    }

    static final class ChildCriteria extends FullTypeCriteria<Child, ChildCriteria>
            implements IIdCriteria<Long, Child, ChildCriteria> {
    }

    static final class DetailCriteria extends FullTypeCriteria<Detail, DetailCriteria>
            implements IIdCriteria<Long, Detail, DetailCriteria> {
    }

    record ParentService(ParentRepository repository)
            implements ILocalCrudService<Parent, ParentCriteria, Long> {
        @Override public IRepository<Parent, ParentCriteria> getRepository() { return repository; }
    }

    record ChildService(ChildRepository repository)
            implements ILocalCrudService<Child, ChildCriteria, Long> {
        @Override public IRepository<Child, ChildCriteria> getRepository() { return repository; }
    }

    record DetailService(DetailRepository repository)
            implements ILocalCrudService<Detail, DetailCriteria, Long> {
        @Override public IRepository<Detail, DetailCriteria> getRepository() { return repository; }
    }

    static final class ParentRepository implements IRepository<Parent, ParentCriteria> {
        private final Map<Long, Parent> rows = new LinkedHashMap<>();
        @Override public boolean isPresentByCriteria(ICriteria<Parent, ParentCriteria> criteria) {
            return Objects.equals(((ParentCriteria) criteria).getId(), 1L);
        }
        @Override public List<Parent> findByCriteria(ICriteria<Parent, ParentCriteria> criteria) {
            @SuppressWarnings("unchecked")
            Collection<Long> ids = ((ParentCriteria) criteria).getIn("id", Collection.class);
            return rows.values().stream().filter(parent -> ids == null || ids.contains(parent.getId())).toList();
        }
    }

    static final class ChildRepository implements IRepository<Child, ChildCriteria> {
        private final Map<Long, Child> rows = new LinkedHashMap<>();
        private int queryCount;

        @Override public boolean add(Child entity) {
            if (entity.getId() == null) entity.setId(rows.keySet().stream().mapToLong(Long::longValue)
                    .max().orElse(0L) + 1L);
            rows.put(entity.getId(), entity);
            return true;
        }

        @Override public boolean updateById(Child entity) {
            if (!rows.containsKey(entity.getId())) return false;
            rows.put(entity.getId(), entity);
            return true;
        }

        @Override public int updateCountByCriteria(Child entity, ICriteria<Child, ChildCriteria> criteria) {
            return updateById(entity) ? 1 : 0;
        }

        @Override public Child getEntityById(Serializable id) {
            return rows.get(id);
        }

        @Override public Page<Child> findPageByCriteria(ICriteria<Child, ChildCriteria> criteria) {
            List<Child> rows = findByCriteria(criteria);
            return Page.of(rows, 1, 1000, rows.size());
        }

        @Override public List<Child> findByCriteria(
                ICriteria<Child, ChildCriteria> criteria) {
            queryCount++;
            ChildCriteria filter = (ChildCriteria) criteria;
            Long parentId = filter.getEquals("parentId", Long.class);
            @SuppressWarnings("unchecked")
            Collection<Long> retained = filter.getNotIn("id", Collection.class);
            @SuppressWarnings("unchecked")
            Collection<Long> parentIds = filter.getIn("parentId", Collection.class);
            @SuppressWarnings("unchecked")
            Collection<Long> ids = filter.getIn("id", Collection.class);
            return rows.values().stream()
                    .filter(child -> parentId == null || Objects.equals(child.getParentId(), parentId))
                    .filter(child -> parentIds == null || parentIds.contains(child.getParentId()))
                    .filter(child -> ids == null || ids.contains(child.getId()))
                    .filter(child -> retained == null || !retained.contains(child.getId()))
                    .toList();
        }

        @Override public int deleteCountByIds(Collection<? extends Serializable> ids) {
            int count = 0;
            for (Serializable id : ids) if (rows.remove(id) != null) count++;
            return count;
        }

        @Override public int deleteCountByCriteria(
                ICriteria<Child, ChildCriteria> rawCriteria) {
            ChildCriteria criteria = (ChildCriteria) rawCriteria;
            Long parentId = criteria.getEquals("parentId", Long.class);
            @SuppressWarnings("unchecked")
            Collection<Long> retained = criteria.getNotIn("id", Collection.class);
            List<Long> deleting = rows.values().stream()
                    .filter(child -> Objects.equals(child.getParentId(), parentId))
                    .map(Child::getId)
                    .filter(id -> retained == null || !retained.contains(id))
                    .toList();
            deleting.forEach(rows::remove);
            return deleting.size();
        }
    }

    static final class DetailRepository implements IRepository<Detail, DetailCriteria> {
        private final Map<Long, Detail> rows = new LinkedHashMap<>();
        private int queryCount;

        @Override public boolean add(Detail entity) {
            if (entity.getId() == null) {
                entity.setId(rows.keySet().stream().mapToLong(Long::longValue)
                        .max().orElse(0L) + 1L);
            }
            rows.put(entity.getId(), entity);
            return true;
        }

        @Override public boolean updateById(Detail entity) {
            if (!rows.containsKey(entity.getId())) return false;
            rows.put(entity.getId(), entity);
            return true;
        }

        @Override public int updateCountByCriteria(Detail entity, ICriteria<Detail, DetailCriteria> criteria) {
            return updateById(entity) ? 1 : 0;
        }

        @Override public Detail getEntityById(Serializable id) {
            return rows.get(id);
        }

        @Override public Page<Detail> findPageByCriteria(ICriteria<Detail, DetailCriteria> criteria) {
            List<Detail> rows = findByCriteria(criteria);
            return Page.of(rows, 1, 1000, rows.size());
        }

        @Override public List<Detail> findByCriteria(
                ICriteria<Detail, DetailCriteria> criteria) {
            queryCount++;
            Long parentId = ((DetailCriteria) criteria).getEquals("parentId", Long.class);
            return rows.values().stream()
                    .filter(detail -> parentId == null
                            || Objects.equals(detail.getParentId(), parentId))
                    .toList();
        }
    }
}
