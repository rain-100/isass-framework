// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import vip.isass.framework.common.security.CurrentPrincipalUtil;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.repository.IRepository;
import vip.isass.framework.nocode.service.CrudWriteExecutor;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CrudWriteLifecycleTest {

    @Test
    void exposesTheCanonicalRequestAndInvokesCommitCallbacks() {
        IRepository<TestEntity, TestCriteria> repository = mock(IRepository.class);
        List<String> events = new ArrayList<>();
        SuperCudReq<TestEntity, TestCriteria> request = SuperCudReq.add(new TestEntity());
        CrudWriteLifecycleListener listener = new CrudWriteLifecycleListener() {
            @Override
            public void beforeExecute(CrudWriteLifecycleContext<?, ?> context) {
                assertSame(request, context.request());
                events.add("before");
            }

            @Override
            public void afterExecute(CrudWriteLifecycleContext<?, ?> context) {
                events.add("afterExecute");
            }

            @Override
            public void afterCommit(CrudWriteLifecycleContext<?, ?> context) {
                events.add("afterCommit");
            }
        };

        new CrudWriteExecutor(null, null, List.of(listener))
                .superCud(new TestService(repository), request);

        assertEquals(List.of("before", "afterExecute", "afterCommit"), events);
    }

    @Test
    void invokesRollbackCallbackWhenCrudOperationThrows() {
        IRepository<TestEntity, TestCriteria> repository = mock(IRepository.class);
        doThrow(new IllegalStateException("database unavailable"))
                .when(repository).add(org.mockito.ArgumentMatchers.any());
        List<String> events = new ArrayList<>();
        CrudWriteLifecycleListener listener = new CrudWriteLifecycleListener() {
            @Override
            public void beforeExecute(CrudWriteLifecycleContext<?, ?> context) {
                events.add("before");
            }

            @Override
            public void afterRollback(CrudWriteLifecycleContext<?, ?> context, Throwable error) {
                events.add("rollback:" + error.getMessage());
            }
        };

        assertThrows(IllegalStateException.class, () ->
                new CrudWriteExecutor(null, null, List.of(listener))
                        .superCud(new TestService(repository), SuperCudReq.add(new TestEntity())));

        assertEquals(List.of("before", "rollback:database unavailable"), events);
    }

    @Test
    void invokesCompletionCallbacksOnlyAfterTheSurroundingTransactionCompletes() {
        TestTransactionManager transactionManager = new TestTransactionManager();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<String> events = new ArrayList<>();
        CrudWriteLifecycleListener listener = new CrudWriteLifecycleListener() {
            @Override
            public void beforeExecute(CrudWriteLifecycleContext<?, ?> context) {
                events.add("before");
            }

            @Override
            public void afterExecute(CrudWriteLifecycleContext<?, ?> context) {
                events.add("afterExecute");
            }

            @Override
            public void afterCommit(CrudWriteLifecycleContext<?, ?> context) {
                events.add("afterCommit");
            }

            @Override
            public void afterRollback(CrudWriteLifecycleContext<?, ?> context, Throwable error) {
                events.add("afterRollback");
            }
        };
        CrudWriteExecutor executor = new CrudWriteExecutor(null, transactionManager, List.of(listener));
        TestService service = new TestService(mock(IRepository.class));

        transaction.executeWithoutResult(status -> {
            executor.superCud(service, SuperCudReq.empty());
            assertEquals(List.of("before", "afterExecute"), events);
        });
        assertEquals(List.of("before", "afterExecute", "afterCommit"), events);

        events.clear();
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            executor.superCud(service, SuperCudReq.empty());
            throw new IllegalStateException("force rollback");
        }));
        assertEquals(List.of("before", "afterExecute", "afterRollback"), events);
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

            new CrudWriteExecutor().superCud(new TestService(repository), SuperCudReq.add(entity));

            assertEquals(99L, entity.getTenantId());
        } finally {
            CurrentPrincipalUtil.setCurrentPrincipalService(null);
        }
    }

    static class TestService implements ILocalCrudService<TestEntity, TestCriteria, Long> {
        private final IRepository<TestEntity, TestCriteria> repository;

        TestService(IRepository<TestEntity, TestCriteria> repository) {
            this.repository = repository;
        }

        @Override
        public IRepository<TestEntity, TestCriteria> getRepository() {
            return repository;
        }

        @Override
        public TestCriteria newCriteria() {
            return new TestCriteria();
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

    static class TestCriteria extends FullTypeCriteria<TestEntity, TestCriteria>
            implements IIdCriteria<Long, TestEntity, TestCriteria> {
    }

    static final class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
