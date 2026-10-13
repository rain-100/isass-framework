// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleListener;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleListener;
import vip.isass.framework.nocode.entity.CrudQueryReq;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KeylessCrudServiceTest {

    @Test
    void standardFacadesSupportCreateAndCriteriaBasedWritesWithoutAnId() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        LocalService service = new LocalService(repository);
        Entity entity = new Entity("setting", "value");
        when(repository.updateCountByCriteria(any(), any())).thenReturn(2);
        when(repository.deleteCountByCriteria(any())).thenReturn(3);

        assertSame(entity, service.create(entity));
        assertEquals(2L, service.createBatch(List.of(entity, entity)));
        assertTrue(service.createIfAbsent(entity, Entity::getCode));
        Criteria update = new Criteria().equals("code", "setting");
        assertEquals(2L, service.update(entity, update));
        Criteria delete = new Criteria().equals("code", "obsolete");
        assertEquals(3L, service.delete(delete));

        verify(repository).updateCountByCriteria(same(entity), any());
        verify(repository).deleteCountByCriteria(delete);
        assertNull(update.getEquals("id"));
        assertEquals(List.of(), delete.getReturnFields());
    }

    @Test
    void matchFieldsProvideTheRequiredUpdateConditionWithoutAnId() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        LocalService service = new LocalService(repository);
        Entity entity = new Entity("setting", "new value");
        when(repository.updateCountByCriteria(any(), any())).thenAnswer(invocation -> {
            Criteria criteria = invocation.getArgument(1);
            assertEquals("setting", criteria.getEquals("code"));
            assertNull(criteria.getEquals("id"));
            return 1;
        });

        assertEquals(1L, service.update(entity, new Criteria().setMatchFields(List.of("code"))));
    }

    @Test
    void pageGetOneListCountAndExistsRemainAvailable() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        Entity entity = new Entity("setting", "value");
        when(repository.findPageByCriteria(any())).thenReturn(Page.of(List.of(entity), 1, 20, 1));
        when(repository.countByCriteria(any())).thenReturn(1);
        when(repository.isPresentByCriteria(any())).thenReturn(true);
        LocalService service = new LocalService(repository);

        assertEquals(List.of(entity), service.page(null).getRecords());
        assertSame(entity, service.getOne(new Criteria().equals("code", "setting")));
        assertEquals(List.of(entity), service.list(new Criteria()));
        assertEquals(1L, service.countAll());
        assertTrue(service.exists(new Criteria()));
        assertEquals(Entity.class, CrudServiceTypeResolver.resolveEntityClass(LocalService.class));
        assertEquals(Criteria.class, CrudServiceTypeResolver.resolveCriteriaClass(LocalService.class));
        assertEquals(Entity.class, CrudServiceTypeResolver.resolveEntityClass(RemoteFacade.class));
    }

    @Test
    void allIdDependentOperationsFailBeforeAccessingTheRepository() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        LocalService service = new LocalService(repository);
        Entity entity = new Entity("setting", "value");
        List<Runnable> unsupported = List.of(
                () -> service.getById(1L),
                () -> service.existsById(1L),
                () -> service.delete((Serializable) 1L),
                () -> service.deleteIds(List.of(1L)),
                () -> service.update(entity),
                () -> service.updateAllColumns(entity),
                () -> service.update(entity, new Criteria()),
                () -> service.cursorPage(new Criteria(), null, 20L),
                () -> service.superCud(SuperCudReq.<Entity, Criteria, Serializable>delete(1L)),
                () -> new CrudQueryExecutor().query(service, CrudQueryReq.tree(new Criteria())));

        for (Runnable operation : unsupported) {
            UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class, operation::run);
            assertTrue(error.getMessage().contains("IIdEntity"));
        }
        verifyNoInteractions(repository);
        assertTrue(entity.presentProperties().isEmpty());
    }

    @Test
    void rejectsAnInvalidMixedChangeSetAndUnconditionalDeletesBeforeAnyWrites() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        LocalService service = new LocalService(repository);
        SuperCudReq<Entity, Criteria, Serializable> mixed = SuperCudReq.<Entity, Criteria, Serializable>builder()
                .addEntity(new Entity("new", "value"))
                .deleteId(1L)
                .build();
        assertThrows(UnsupportedOperationException.class, () -> service.superCud(mixed));
        assertThrows(IllegalArgumentException.class, () -> service.delete(new Criteria()));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsIdDependentNestedWritesAndRelatedTargetLoadingBeforeDatabaseAccess() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        LocalService service = new LocalService(repository);
        Entity source = new Entity("parent", "value");
        source.setMembers(List.of(new Entity("child", "value")));
        AssociationWriteCoordinator writer = new AssociationWriteCoordinator(List.of(service));
        assertThrows(UnsupportedOperationException.class, () -> new CrudWriteExecutor(writer)
                .superCud(service, SuperCudReq.add(source)));
        AssociationQueryCoordinator reader = new AssociationQueryCoordinator(List.of(service));
        assertThrows(UnsupportedOperationException.class, () -> new CrudQueryExecutor(reader, List.of())
                .query(service, CrudQueryReq.page(new Criteria().loadRelated("members"))));
        verifyNoInteractions(repository);
    }

    @Test
    void keylessOperationsStillRunThroughTheQueryAndWriteLifecycles() {
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        LocalService service = new LocalService(repository);
        List<String> events = new ArrayList<>();
        CrudQueryLifecycleListener queryListener = new CrudQueryLifecycleListener() {
            @Override
            public void beforeQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                assertEquals(Entity.class, context.entityClass());
                events.add("beforeQuery");
            }

            @Override
            public void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
                events.add("afterQuery");
            }
        };
        CrudWriteLifecycleListener writeListener = new CrudWriteLifecycleListener() {
            @Override
            public void beforeExecute(CrudWriteLifecycleContext<?, ?, ?> context) {
                assertEquals(Entity.class, context.entityClass());
                events.add("beforeWrite");
            }

            @Override
            public void afterCommit(CrudWriteLifecycleContext<?, ?, ?> context) {
                events.add("afterCommit");
            }
        };
        when(repository.countByCriteria(any())).thenReturn(2);
        assertEquals(2L, new CrudQueryExecutor(null, List.of(queryListener))
                .query(service, CrudQueryReq.count(new Criteria())).count());
        assertEquals(1L, new CrudWriteExecutor(null, null, List.of(writeListener))
                .superCud(service, SuperCudReq.add(new Entity("new", "value"))).addedCount());
        assertEquals(List.of("beforeQuery", "afterQuery", "beforeWrite", "afterCommit"), events);
    }

    public static class Entity implements IEntity<Entity> {
        private final String code;
        private final String value;
        private List<Entity> members;

        public Entity() {
            this(null, null);
        }

        public Entity(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }

        public List<Entity> getMembers() {
            return members;
        }

        public void setMembers(List<Entity> members) {
            this.members = members;
        }

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("members", Entity.class, "code", "code"));
        }

        @Override
        public Entity randomEntity() {
            return this;
        }
    }

    public static class Criteria extends FullTypeCriteria<Entity, Criteria> {
    }

    interface RemoteFacade extends ICrudService<Entity, Criteria, Serializable> {
    }

    static class LocalService implements RemoteFacade, ILocalCrudService<Entity, Criteria, Serializable> {
        private final IRepository<Entity, Criteria> repository;

        LocalService(IRepository<Entity, Criteria> repository) {
            this.repository = repository;
        }

        @Override
        public IRepository<Entity, Criteria> getRepository() {
            return repository;
        }
    }
}
