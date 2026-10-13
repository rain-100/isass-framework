// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.security;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.field.IIdCriteria;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.entity.ITenantEntity;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.security.CurrentPrincipalUtil;
import vip.isass.framework.security.CurrentPrincipalService;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.AuthenticatedPrincipal;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.security.data.DataReadContext;
import vip.isass.framework.security.data.TenantReadContext;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.nocode.entity.CrudQueryReq;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleContext;
import vip.isass.framework.nocode.service.CrudQueryExecutor;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantReadLifecycleListenerTest {
    @AfterEach
    void clearPrincipal() {
        CurrentPrincipalUtil.setCurrentPrincipalServiceProvider(() -> null);
        assertNull(TenantReadContext.current());
    }

    @Test
    void coversEveryReadFacadeAndCleansTheScopeOnFailure() {
        principal(7L);
        IRepository<Entity, Criteria> repository = mock(IRepository.class);
        when(repository.findPageByCriteria(any())).thenAnswer(call -> {
            assertTenant();
            return Page.empty(1L, 20L);
        });
        when(repository.countByCriteria(any())).thenAnswer(call -> {
            assertTenant();
            return 0;
        });
        when(repository.isPresentByCriteria(any())).thenAnswer(call -> {
            assertTenant();
            return false;
        });
        var service = new LocalService(repository);
        var executor = new CrudQueryExecutor(null, List.of(new TenantReadLifecycleListener(List.of())));
        executor.query(service, CrudQueryReq.page(new Criteria()));
        assertNull(TenantReadContext.current());
        executor.query(service, CrudQueryReq.cursorPage(new Criteria(), null, 20L));
        assertNull(TenantReadContext.current());
        executor.query(service, CrudQueryReq.count(new Criteria()));
        assertNull(TenantReadContext.current());
        executor.query(service, CrudQueryReq.exists(new Criteria()));
        assertNull(TenantReadContext.current());
        doThrow(new IllegalStateException("database failed")).when(repository).countByCriteria(any());
        assertThrows(IllegalStateException.class, () -> executor.query(service, CrudQueryReq.count(new Criteria())));
    }

    @Test
    void missingTenantNeverMeansUnrestrictedTenantRows() {
        principal(null);
        var listener = new TenantReadLifecycleListener(List.of());
        var context = context();
        try {
            listener.beforeQuery(context);
            var denial = TenantReadContext.current().conditions(Entity.class).getFirst();
            assertEquals(Condition.Logical.AND, denial.getCondition());
            assertEquals(Condition.NullCheck.IS_NULL, denial.getChildren().getFirst().getCondition());
            assertEquals(Condition.NullCheck.IS_NOT_NULL, denial.getChildren().getLast().getCondition());
        } finally {
            listener.onFailure(context, new IllegalStateException());
        }
    }

    @Test
    void nestedQueryKeepsTheOriginalScopeAndMetadataCannotClearIt() {
        principal(7L);
        var listener = new TenantReadLifecycleListener(List.of());
        var root = context();
        var child = context();
        try {
            listener.beforeQuery(root);
            var original = TenantReadContext.current();
            listener.beforeQuery(child);
            listener.onFailure(child, new IllegalStateException());
            assertSame(original, TenantReadContext.current());
            DataReadContext.metadata(() -> {
                assertTenant();
                return null;
            });
        } finally {
            listener.onFailure(root, new IllegalStateException());
        }
    }

    @Test
    void internalAuthenticationDoesNotBypassTheAccompanyingBusinessPrincipal() {
        var principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.APPLICATION)
                .setPrincipalId(1L)
                .setTenantId(7L);
        CurrentPrincipalUtil.setCurrentPrincipalService(new CurrentPrincipalService() {
            @Override
            public AuthenticatedPrincipal getPrincipal() {
                return principal;
            }

            @Override
            public InternalServicePrincipal getInternalServicePrincipal() {
                return new InternalServicePrincipal("bsp-service", "test-key", "request");
            }
        });
        var listener = new TenantReadLifecycleListener(List.of());
        var context = context();
        try {
            listener.beforeQuery(context);
            principal.setTenantId(8L);
            assertTenant();
        } finally {
            listener.onFailure(context, new IllegalStateException());
        }
        CurrentPrincipalUtil.setCurrentPrincipalService(new CurrentPrincipalService() {
            @Override
            public AuthenticatedPrincipal getPrincipal() {
                return null;
            }

            @Override
            public InternalServicePrincipal getInternalServicePrincipal() {
                return new InternalServicePrincipal("bsp-service", "test-key", "request");
            }
        });
        listener.beforeQuery(context);
        assertNull(TenantReadContext.current());
        listener.afterQuery(context);
    }

    private void principal(Long tenantId) {
        CurrentPrincipalUtil.setCurrentPrincipalService(() -> new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER)
                .setPrincipalId(1L)
                .setTenantId(tenantId));
    }

    private void assertTenant() {
        var condition = TenantReadContext.current().conditions(Entity.class).getFirst();
        assertEquals("tenantId", condition.getSourceProperty());
        assertEquals(7L, condition.getValue());
    }

    private CrudQueryLifecycleContext<Entity, Criteria, Long> context() {
        return new CrudQueryLifecycleContext<>(new LocalService(mock(IRepository.class)), CrudQueryReq.count(new Criteria()));
    }

    private record LocalService(IRepository<Entity, Criteria> repository) implements ILocalCrudService<Entity, Criteria, Long> {
        @Override
        public IRepository<Entity, Criteria> getRepository() {
            return repository;
        }

        @Override
        public Criteria newCriteria() {
            return new Criteria();
        }
    }

    @Getter
    @Setter
    private static class Entity implements IIdEntity<Long, Entity>, ITenantEntity<Long, Entity> {
        private Long id;
        private Long tenantId;

        @Override
        public Entity randomEntity() {
            return this;
        }
    }

    private static class Criteria extends FullTypeCriteria<Entity, Criteria> implements IIdCriteria<Long, Entity, Criteria> {
    }
}
