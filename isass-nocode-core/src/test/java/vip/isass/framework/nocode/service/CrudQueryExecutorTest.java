// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.CrudQueryReq;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleListener;
import vip.isass.framework.nocode.repository.IRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrudQueryExecutorTest {

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
                assertNotSame(source, context.request().criteria());
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
}
