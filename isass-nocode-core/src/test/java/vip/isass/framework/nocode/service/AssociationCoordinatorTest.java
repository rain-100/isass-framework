// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.criteria.UpdateMode;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.EntityAssociation;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssociationCoordinatorTest {

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
    void loadsRequestedAssociationsInOneTargetQuery() {
        ParentRepository parents = new ParentRepository();
        ChildRepository children = new ChildRepository();
        children.rows.put(10L, new Child(10L, 1L, "one"));
        children.rows.put(20L, new Child(20L, 2L, "two"));
        AssociationQueryCoordinator coordinator = new AssociationQueryCoordinator(List.of(
                new ParentService(parents), new ChildService(children)));
        Parent first = new Parent(1L);
        Parent second = new Parent(2L);
        ParentCriteria criteria = new ParentCriteria().setAssociationQueries(List.of("children"));

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
        Parent first = new Parent(1L);
        Parent second = new Parent(2L);
        ParentCriteria criteria = new ParentCriteria()
                .setAssociationQueries(List.of("children.detail"));

        coordinator.populate(List.of(first, second), criteria);

        assertEquals(List.of(10L), first.getChildren().stream().map(Child::getId).toList());
        assertEquals("first detail", first.getChildren().iterator().next().getDetail().getName());
        assertEquals(List.of(20L), second.getChildren().stream().map(Child::getId).toList());
        assertEquals("second detail", second.getChildren().iterator().next().getDetail().getName());
        assertEquals(1, children.queryCount);
        assertEquals(1, details.queryCount);
    }

    static final class Parent implements IIdEntity<Long, Parent> {
        private Long id;
        private Collection<Child> children;

        Parent(Long id) { this.id = id; }
        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        public Collection<Child> getChildren() { return children; }
        public void setChildren(Collection<Child> children) {
            this.children = children;
            markPresentProperty("children");
        }
        @Override public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("children", Child.class,
                    "id", "parentId", true));
        }
    }

    static final class Child implements IIdEntity<Long, Child> {
        private Long id;
        private Long parentId;
        private String name;
        private Long detailId;
        private Detail detail;

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
        private String name;

        Detail(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
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

        @Override public Child getEntityById(Serializable id) {
            return rows.get(id);
        }

        @Override public List<Child> findByCriteria(
                vip.isass.framework.nocode.criteria.ICriteria<Child, ChildCriteria> criteria) {
            queryCount++;
            return new ArrayList<>(rows.values());
        }

        @Override public int deleteCountByCriteria(
                vip.isass.framework.nocode.criteria.ICriteria<Child, ChildCriteria> rawCriteria) {
            ChildCriteria criteria = (ChildCriteria) rawCriteria;
            Long parentId = criteria.getEquals("parentId", Long.class);
            @SuppressWarnings("unchecked")
            Collection<Long> retained = criteria.getNotIn("id", Collection.class);
            List<Long> deleting = rows.values().stream()
                    .filter(child -> java.util.Objects.equals(child.getParentId(), parentId))
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

        @Override public List<Detail> findByCriteria(
                vip.isass.framework.nocode.criteria.ICriteria<Detail, DetailCriteria> criteria) {
            queryCount++;
            return new ArrayList<>(rows.values());
        }
    }
}
