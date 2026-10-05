// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.field.IIdCriteria;
import vip.isass.framework.common.criteria.field.IParentIdCriteria;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.CrudQueryReq;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.entity.IParentIdEntity;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleListener;
import vip.isass.framework.database.core.repository.IRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class CrudQueryExecutorTest {

    @Test
    void lifecycleAndRepositoryUseTheCallersCompleteCriteria() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        List<Long> ids = new ArrayList<>(List.of(1L));
        Criteria child = new Criteria().in("id", ids);
        Criteria source = new Criteria().exists(child);
        ids.add(2L);
        CrudQueryLifecycleListener listener = new CrudQueryLifecycleListener() {
            @Override
            public void beforeQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                Criteria executing = (Criteria) context.request().criteria();
                assertSame(source, executing);
                Criteria nested = (Criteria) executing.getWhereConditions().getFirst().getTargetCriteria();
                assertSame(child, nested);
                WhereCondition condition = nested.getWhereConditions().getFirst();
                assertEquals(List.of(1L, 2L), condition.getValue());
                condition.setValue(List.of(3L));
            }

            @Override
            public void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                assertSame(source, context.request().criteria());
                assertEquals(List.of(3L), child.getWhereConditions().getFirst().getValue());
            }
        };
        when(repository.countByCriteria(any())).thenAnswer(invocation -> {
            Criteria executing = invocation.getArgument(0);
            assertSame(source, executing);
            Criteria nested = (Criteria) executing.getWhereConditions().getFirst().getTargetCriteria();
            assertEquals(List.of(3L), nested.getWhereConditions().getFirst().getValue());
            return 1;
        });
        assertEquals(1L, new CrudQueryExecutor(null, List.of(listener))
                .query(new LocalService(repository), CrudQueryReq.count(source)).count());
        assertEquals(List.of(3L), child.getWhereConditions().getFirst().getValue());
    }

    @Test
    void cursorChangesRemainVisibleAndCallersCanExplicitlyCopyForIsolation() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        Criteria original = new Criteria().setReturnField("id");
        Criteria execution = original.copy();
        when(repository.findPageByCriteria(any())).thenAnswer(invocation -> {
            Criteria actual = invocation.getArgument(0);
            assertSame(execution, actual);
            assertEquals(10L, actual.getGreaterThan("id", Long.class));
            assertEquals("id asc", actual.getOrderBy());
            assertEquals(1L, actual.getPageNum());
            assertEquals(3L, actual.getPageSize());
            assertEquals(false, actual.getSearchCountFlag());
            return Page.of(List.of(new Entity(11L), new Entity(12L), new Entity(13L)), 1, 3, 0);
        });
        CrudQueryExecutor executor = new CrudQueryExecutor();
        var result = executor.query(new LocalService(repository), CrudQueryReq.cursorPage(execution, 10L, 2L))
                .cursorPage();
        assertEquals(List.of(11L, 12L), result.records().stream().map(Entity::getId).toList());
        assertTrue(result.hasMore());
        assertEquals(12L, result.nextCursorId());
        assertEquals(10L, execution.getGreaterThan("id", Long.class));
        assertEquals(3L, execution.getPageSize());
        assertTrue(original.getWhereConditions().isEmpty());
        assertEquals(20L, original.getPageSize());
    }

    @Test
    void routesAllQueryFacadesThroughOneNormalizedLifecycleRequest() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        when(repository.findPageByCriteria(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Page.of(List.of(new Entity(1L)), 1, 20, 1));
        when(repository.countByCriteria(org.mockito.ArgumentMatchers.any())).thenReturn(7);
        when(repository.isPresentByCriteria(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        List<String> events = new ArrayList<>();
        Criteria source = new Criteria();
        CrudQueryLifecycleListener listener = new CrudQueryLifecycleListener() {
            @Override
            public void beforeQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                assertSame(source, context.request().criteria());
                events.add("before:" + context.request().queryType());
            }

            @Override
            public void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                events.add("after:" + context.result().queryType());
            }
        };
        CrudQueryExecutor executor = new CrudQueryExecutor(null, List.of(listener));
        LocalService service = new LocalService(repository);

        assertEquals(1, executor.query(service, CrudQueryReq.page(source)).page().getRecords().size());
        assertEquals(1, executor.query(service, CrudQueryReq.cursorPage(source, null, 20L))
                .cursorPage().records().size());
        assertEquals(7L, executor.query(service, CrudQueryReq.count(source)).count());
        assertEquals(true, executor.query(service, CrudQueryReq.exists(source)).exists());
        assertEquals(List.of(
                "before:PAGE", "after:PAGE",
                "before:CURSOR_PAGE", "after:CURSOR_PAGE",
                "before:COUNT", "after:COUNT",
                "before:EXISTS", "after:EXISTS"), events);
    }

    @Test
    void rejectsAResultReplacementWithAnotherQueryType() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        when(repository.countByCriteria(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        CrudQueryLifecycleListener listener = new CrudQueryLifecycleListener() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                ((CrudQueryLifecycleContext) context).setResult(
                        vip.isass.framework.nocode.entity.CrudQueryResult.exists(true));
            }
        };

        assertThrows(IllegalArgumentException.class, () ->
                new CrudQueryExecutor(null, List.of(listener)).query(
                        new LocalService(repository), CrudQueryReq.count(new Criteria())));
    }

    @Test
    void buildsTreeThroughTheSameLifecycleAndExposesEveryNodeToListeners() {
        IRepository<TreeEntity, TreeCriteria> repository = mock(IRepository.class);
        TreeEntity child = new TreeEntity(2L, 1L);
        TreeEntity root = new TreeEntity(1L, null);
        TreeEntity filteredOrphan = new TreeEntity(3L, 99L);
        TreeEntity zeroRoot = new TreeEntity(4L, 0L);
        when(repository.findByCriteria(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(child, root, filteredOrphan, zeroRoot));
        AtomicReference<List<?>> lifecycleRecords = new AtomicReference<>();
        CrudQueryLifecycleListener listener = new CrudQueryLifecycleListener() {
            @Override
            public void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                lifecycleRecords.set(context.result().records());
            }
        };
        CrudQueryExecutor executor = new CrudQueryExecutor(null, List.of(listener));
        CrudQueryExecutorProvider provider = new CrudQueryExecutorProvider(executor);
        TreeLocalService service = new TreeLocalService(repository);

        List<TreeEntity> trees;
        try {
            trees = service.tree(new TreeCriteria());
        } finally {
            provider.destroy();
        }

        assertEquals(List.of(1L, 3L, 4L), trees.stream().map(TreeEntity::getId).toList());
        assertEquals(List.of(2L), trees.getFirst().getChildren().stream().map(TreeEntity::getId).toList());
        assertEquals(List.of(1L, 2L, 3L, 4L), lifecycleRecords.get().stream()
                .map(entity -> ((TreeEntity) entity).getId()).toList());
    }

    @Test
    void rejectsCyclesInTreeResults() {
        IRepository<TreeEntity, TreeCriteria> repository = mock(IRepository.class);
        when(repository.findByCriteria(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new TreeEntity(1L, 2L),
                new TreeEntity(2L, 1L)));

        assertThrows(IllegalStateException.class, () -> new CrudQueryExecutor().query(
                new TreeLocalService(repository), CrudQueryReq.tree(new TreeCriteria())));
    }

    @Test
    void returnsDescendantIdsThroughTheTreeQueryLifecycleInBreadthFirstOrder() {
        IRepository<TreeEntity, TreeCriteria> repository = mock(IRepository.class);
        when(repository.findByCriteria(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new TreeEntity(1L, null),
                new TreeEntity(2L, 1L),
                new TreeEntity(3L, 1L),
                new TreeEntity(4L, 2L)));
        List<String> events = new ArrayList<>();
        CrudQueryLifecycleListener listener = new CrudQueryLifecycleListener() {
            @Override
            public void beforeQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                events.add("before:" + context.request().queryType());
            }

            @Override
            public void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                events.add("after:" + context.result().queryType());
            }
        };
        CrudQueryExecutorProvider provider = new CrudQueryExecutorProvider(
                new CrudQueryExecutor(null, List.of(listener)));

        try {
            assertEquals(List.of(2L, 3L, 4L),
                    new TreeLocalService(repository).descendantIds(1L, new TreeCriteria()));
        } finally {
            provider.destroy();
        }
        assertEquals(List.of("before:TREE", "after:TREE"), events);
    }

    @Test
    void rejectsMissingDescendantRoot() {
        IRepository<TreeEntity, TreeCriteria> repository = mock(IRepository.class);
        when(repository.findByCriteria(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new TreeEntity(1L, null)));
        CrudQueryExecutorProvider provider = new CrudQueryExecutorProvider(new CrudQueryExecutor());

        try {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new TreeLocalService(repository).descendantIds(99L, new TreeCriteria()));
            assertEquals("树查询结果中不存在节点: 99", exception.getMessage());
        } finally {
            provider.destroy();
        }
    }

    static final class LocalService implements ILocalCrudService<Entity, Criteria, Long> {
        private final IRepository<Entity, Criteria> repository;

        LocalService(IRepository<Entity, Criteria> repository) {
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

    static final class Entity implements IIdEntity<Long, Entity> {
        private Long id;

        Entity(Long id) {
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
    }

    static final class Criteria extends FullTypeCriteria<Entity, Criteria>
            implements IIdCriteria<Long, Entity, Criteria> {
    }

    static final class TreeLocalService
            implements ILocalCrudService<TreeEntity, TreeCriteria, Long>,
            ILocalTreeQueryService<TreeEntity, TreeCriteria, Long> {
        private final IRepository<TreeEntity, TreeCriteria> repository;

        TreeLocalService(IRepository<TreeEntity, TreeCriteria> repository) {
            this.repository = repository;
        }

        @Override
        public IRepository<TreeEntity, TreeCriteria> getRepository() {
            return repository;
        }

        @Override
        public TreeCriteria newCriteria() {
            return new TreeCriteria();
        }
    }

    static final class TreeEntity
            implements IIdEntity<Long, TreeEntity>, IParentIdEntity<Long, TreeEntity> {
        private Long id;
        private Long parentId;
        private TreeEntity parent;
        private List<TreeEntity> children;

        TreeEntity(Long id, Long parentId) {
            this.id = id;
            this.parentId = parentId;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public Long getParentId() {
            return parentId;
        }

        @Override
        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        @Override
        public TreeEntity getParent() {
            return parent;
        }

        @Override
        public void setParent(TreeEntity parent) {
            this.parent = parent;
        }

        @Override
        public List<TreeEntity> getChildren() {
            return children;
        }

        @Override
        public void setChildren(List<TreeEntity> children) {
            this.children = children;
        }

        @Override
        public TreeEntity randomEntity() {
            IIdEntity.super.randomEntity();
            IParentIdEntity.super.randomEntity();
            return this;
        }
    }

    static final class TreeCriteria extends FullTypeCriteria<TreeEntity, TreeCriteria>
            implements IIdCriteria<Long, TreeEntity, TreeCriteria>,
            IParentIdCriteria<Long, TreeEntity, TreeCriteria> {
    }
}
