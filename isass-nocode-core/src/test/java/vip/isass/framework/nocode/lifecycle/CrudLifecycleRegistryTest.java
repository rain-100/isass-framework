package vip.isass.framework.nocode.lifecycle;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.security.CurrentPrincipalUtil;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.repository.IRepository;
import vip.isass.framework.nocode.service.ILocalService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrudLifecycleRegistryTest {

    @Test
    void invokesCallbacksAroundSuccessfulCrudOperation() {
        IRepository<TestEntity, TestCriteria> repository = mock(IRepository.class);
        List<String> events = new ArrayList<>();
        CrudLifecycleListener listener = listener(events);
        CrudLifecycleRegistry.register(listener);
        try {
            new TestService(repository).add(new TestEntity());
            assertEquals(List.of("before", "success"), events);
        } finally {
            CrudLifecycleRegistry.unregister(listener);
        }
    }

    @Test
    void invokesFailureCallbackWhenCrudOperationThrows() {
        IRepository<TestEntity, TestCriteria> repository = mock(IRepository.class);
        doThrow(new IllegalStateException("database unavailable")).when(repository).add(org.mockito.ArgumentMatchers.any());
        List<String> events = new ArrayList<>();
        CrudLifecycleListener listener = listener(events);
        CrudLifecycleRegistry.register(listener);
        try {
            assertThrows(IllegalStateException.class, () -> new TestService(repository).add(new TestEntity()));
            assertEquals(List.of("before", "failure"), events);
        } finally {
            CrudLifecycleRegistry.unregister(listener);
        }
    }

    @Test
    void fillsCurrentTenantBeforeRepositoryInsert() {
        IRepository<TestEntity, TestCriteria> repository = mock(IRepository.class);
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER)
                .setTenantId(99L);
        CurrentPrincipalUtil.setCurrentPrincipalService(() -> principal);
        try {
            TestEntity entity = new TestEntity();

            new TestService(repository).add(entity);

            assertEquals(99L, entity.getTenantId());
        } finally {
            CurrentPrincipalUtil.setCurrentPrincipalService(null);
        }
    }

    private CrudLifecycleListener listener(List<String> events) {
        return new CrudLifecycleListener() {
            @Override
            public void before(CrudLifecycleContext context) {
                events.add("before");
            }

            @Override
            public void afterSuccess(CrudLifecycleContext context) {
                events.add("success");
            }

            @Override
            public void onFailure(CrudLifecycleContext context, Throwable error) {
                events.add("failure");
            }
        };
    }

    static class TestService implements ILocalService<TestEntity, TestCriteria> {
        private final IRepository<TestEntity, TestCriteria> repository;

        TestService(IRepository<TestEntity, TestCriteria> repository) {
            this.repository = repository;
        }

        @Override
        public IRepository<TestEntity, TestCriteria> getRepository() {
            return repository;
        }
    }

    static class TestEntity implements IIdEntity<Long, TestEntity>, ITenantEntity<Long, TestEntity> {
        private Long id;
        private Long tenantId;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public Long getTenantId() {
            return tenantId;
        }

        @Override
        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public TestEntity randomEntity() {
            return this;
        }
    }

    static class TestCriteria extends FullTypeCriteria<TestEntity, TestCriteria> {
    }
}
