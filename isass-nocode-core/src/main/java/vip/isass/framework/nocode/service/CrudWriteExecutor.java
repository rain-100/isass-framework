// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.SuperCudResult;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleListener;
import vip.isass.framework.nocode.repository.IRepository;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes every standard nocode write through one validation and transaction boundary.
 */
public class CrudWriteExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrudWriteExecutor.class);
    private static final ThreadLocal<Set<Object>> ACTIVE_SERVICES = ThreadLocal.withInitial(
            () -> Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final Map<Class<?>, Map<String, PropertyDescriptor>> PROPERTY_DESCRIPTORS =
            new ConcurrentHashMap<>();

    private final AssociationWriteCoordinator associations;
    private final TransactionTemplate conditionalCreateTransaction;
    private final List<CrudWriteLifecycleListener> listeners;

    public CrudWriteExecutor() {
        this(null, null, List.of());
    }

    public CrudWriteExecutor(AssociationWriteCoordinator associations) {
        this(associations, null, List.of());
    }

    public CrudWriteExecutor(AssociationWriteCoordinator associations,
                             PlatformTransactionManager transactionManager) {
        this(associations, transactionManager, List.of());
    }

    public CrudWriteExecutor(AssociationWriteCoordinator associations,
                             PlatformTransactionManager transactionManager,
                             List<CrudWriteLifecycleListener> listeners) {
        this.associations = associations;
        this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
        conditionalCreateTransaction = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        if (conditionalCreateTransaction != null) {
            conditionalCreateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> SuperCudResult superCud(
            ILocalCrudService<E, C, PK> service,
            SuperCudReq<E, C> request
    ) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(request, "request");
        Set<Object> active = ACTIVE_SERVICES.get();
        if (!active.add(service)) {
            validate(request);
            return execute(service, request);
        }
        try {
            CrudWriteLifecycleContext<E, C> context = new CrudWriteLifecycleContext<>(service, request);
            List<CrudWriteLifecycleListener> supported = listeners.stream()
                    .filter(listener -> listener.supports(context)).toList();
            boolean transactionCallbacks = registerTransactionCallbacks(supported, context);
            try {
                validate(request);
                supported.forEach(listener -> listener.beforeExecute(context));
                context.setResult(execute(service, request));
                supported.forEach(listener -> listener.afterExecute(context));
                if (!transactionCallbacks) notifyAfterCommit(supported, context);
                return context.result();
            } catch (RuntimeException | Error error) {
                context.setFailure(error);
                if (!transactionCallbacks) notifyAfterRollback(supported, context, error);
                throw error;
            }
        } finally {
            active.remove(service);
            if (active.isEmpty()) ACTIVE_SERVICES.remove();
        }
    }

    private boolean registerTransactionCallbacks(List<CrudWriteLifecycleListener> supported,
                                                 CrudWriteLifecycleContext<?, ?> context) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    notifyAfterCommit(supported, context);
                } else {
                    notifyAfterRollback(supported, context, context.failure());
                }
            }
        });
        return true;
    }

    private void notifyAfterCommit(List<CrudWriteLifecycleListener> supported,
                                   CrudWriteLifecycleContext<?, ?> context) {
        for (CrudWriteLifecycleListener listener : supported) {
            try {
                listener.afterCommit(context);
            } catch (RuntimeException | Error error) {
                LOGGER.error("NoCode 写生命周期 afterCommit 执行失败: {}", listener.getClass().getName(), error);
            }
        }
    }

    private void notifyAfterRollback(List<CrudWriteLifecycleListener> supported,
                                     CrudWriteLifecycleContext<?, ?> context,
                                     Throwable error) {
        for (CrudWriteLifecycleListener listener : supported) {
            try {
                listener.afterRollback(context, error);
            } catch (RuntimeException | Error callbackError) {
                if (error != null) error.addSuppressed(callbackError);
                else LOGGER.error("NoCode 写生命周期 afterRollback 执行失败: {}",
                        listener.getClass().getName(), callbackError);
            }
        }
    }

    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> SuperCudResult execute(
            ILocalCrudService<E, C, PK> service,
            SuperCudReq<E, C> request
    ) {
        if (request.isEmpty()) {
            return SuperCudResult.empty();
        }

        IRepository<E, C> repository = service.getRepository();
        long addedCount = 0;
        long updatedCount = 0;
        long deletedCount = 0;

        if (!request.addEntities().isEmpty()) {
            service.prepareForInsert(request.addEntities());
            if (request.addByFields().isEmpty()) {
                if (associations != null && associations.active()) {
                    request.addEntities().forEach(entity -> associations.beforeSave(entity, true));
                }
                if (request.addEntities().size() == 1) {
                    repository.add(request.addEntities().getFirst());
                } else {
                    repository.addBatch(request.addEntities());
                }
                addedCount += request.addEntities().size();
                if (associations != null && associations.active()) {
                    request.addEntities().forEach(entity -> associations.afterSave(entity, null, true));
                }
            } else {
                for (E entity : request.addEntities()) {
                    C criteria = criteriaWithEntityFields(service, null, entity, request.addByFields());
                    if (!insertIfAbsent(repository, entity, criteria)) continue;
                    addedCount++;
                    if (associations != null && associations.active()) {
                        associations.afterSave(entity, criteria, true);
                    }
                }
            }
        }

        for (E entity : request.updateEntities()) {
            if (associations != null && associations.active()) associations.beforeSave(entity, false);
            C effectiveCriteria = null;
            int current;
            if (request.updateCriteria() == null) {
                current = repository.updateById(entity) ? 1 : 0;
                if (current == 0) {
                    throw new AbsentException("按 ID 更新失败，记录不存在: " + idOf(entity));
                }
            } else {
                effectiveCriteria = effectiveUpdateCriteria(service, request.updateCriteria(), entity);
                current = repository.updateCountByCriteria(entity, effectiveCriteria);
            }
            updatedCount += current;
            if (associations != null && associations.active()) {
                associations.afterSave(entity, effectiveCriteria, false);
            }
        }

        if (!request.deleteIds().isEmpty()) {
            if (associations != null && associations.active()) associations.beforeDelete(service, request.deleteIds());
            int affected = repository.deleteCountByIds(request.deleteIds());
            if (affected != request.deleteIds().size()) {
                throw new AbsentException("按 ID 删除失败，请求 " + request.deleteIds().size()
                        + " 条，实际删除 " + affected + " 条");
            }
            deletedCount += affected;
        }

        for (C criteria : request.deleteCriteria()) {
            if (associations != null && associations.active()) {
                List<E> deleting = repository.findByCriteria(criteria);
                associations.beforeDelete(service, deleting.stream().map(IIdEntity::getId).toList());
            }
            deletedCount += repository.deleteCountByCriteria(criteria);
        }

        return new SuperCudResult(addedCount, updatedCount, deletedCount);
    }

    private <E extends vip.isass.framework.nocode.entity.IEntity<E>, C extends ICriteria<E, C>>
    boolean insertIfAbsent(IRepository<E, C> repository, E entity, C criteria) {
        if (repository.isPresentByCriteria(criteria)) {
            return false;
        }
        try {
            if (associations != null && associations.active()) associations.beforeSave(entity, true);
            if (conditionalCreateTransaction == null) {
                repository.add(entity);
            } else {
                conditionalCreateTransaction.executeWithoutResult(status -> repository.add(entity));
            }
            return true;
        } catch (DuplicateKeyException duplicate) {
            if (repository.isPresentByCriteria(criteria)) return false;
            throw duplicate;
        }
    }

    private <E, C> void validate(SuperCudReq<E, C> request) {
        requireNoNulls(request.addEntities(), "addEntities");
        requireNoNulls(request.updateEntities(), "updateEntities");
        requireNoNulls(request.deleteIds(), "deleteIds");
        requireNoNulls(request.deleteCriteria(), "deleteCriteria");

        for (Serializable id : request.deleteIds()) {
            requireUsableId(id, "deleteIds");
        }

        if (!request.addByFields().isEmpty() && request.addEntities().isEmpty()) {
            throw new IllegalArgumentException("addByFields 仅能与 addEntities 一起使用");
        }
        if (!request.addByFields().isEmpty()) {
            validateProperties(request.addEntities().getFirst(), request.addByFields(), "addByFields");
        }
        if (request.updateCriteria() instanceof IUpdateCriteria<?> updateCriteria
                && !updateCriteria.resolveMatchFields().isEmpty()) {
            if (request.updateEntities().isEmpty()) {
                throw new IllegalArgumentException("updateCriteria.matchFields 仅能与 updateEntities 一起使用");
            }
            validateProperties(request.updateEntities().getFirst(),
                    updateCriteria.resolveMatchFields(), "updateCriteria.matchFields");
        }
        if (!request.updateEntities().isEmpty() && request.updateCriteria() == null) {
            request.updateEntities().forEach(entity -> requireUsableId(idOfOrNull(entity), "updateEntities"));
        }
        if (!request.updateEntities().isEmpty() && request.updateCriteria() != null) {
            IWhereConditionCriteria<?, ?> whereCriteria = requireWhereCriteria(
                    request.updateCriteria(), "updateCriteria");
            List<String> matchFields = ((IUpdateCriteria<?>) request.updateCriteria()).resolveMatchFields();
            if (!whereCriteria.hasConditions() && matchFields.isEmpty()) {
                request.updateEntities().forEach(entity -> requireUsableId(idOfOrNull(entity), "updateEntities"));
            }
        }
        request.deleteCriteria().forEach(criteria -> requireCriteria(criteria, "deleteCriteria"));
    }

    private void requireCriteria(Object criteria, String field) {
        IWhereConditionCriteria<?, ?> whereCriteria = requireWhereCriteria(criteria, field);
        if (!whereCriteria.hasConditions()) {
            throw new IllegalArgumentException(field + " 必须至少包含一个有效条件");
        }
    }

    private IWhereConditionCriteria<?, ?> requireWhereCriteria(Object criteria, String field) {
        if (!(criteria instanceof IWhereConditionCriteria<?, ?> whereCriteria)) {
            throw new IllegalArgumentException(field + " 必须实现 IWhereConditionCriteria");
        }
        return whereCriteria;
    }

    private void requireNoNulls(List<?> values, String field) {
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(field + " 不能包含 null");
        }
    }

    private void requireUsableId(Serializable id, String field) {
        if (id == null || id instanceof CharSequence value && value.toString().isBlank()) {
            throw new IllegalArgumentException(field + " 的 ID 不能为空");
        }
    }

    private Serializable idOf(Object entity) {
        Serializable id = idOfOrNull(entity);
        requireUsableId(id, "entity");
        return id;
    }

    private Serializable idOfOrNull(Object entity) {
        if (!(entity instanceof IIdEntity<?, ?> idEntity)) {
            return null;
        }
        return idEntity.getId();
    }

    private void validateProperties(Object entity, List<String> fields, String fieldName) {
        for (String field : fields) {
            propertyValue(entity, field, fieldName);
        }
    }

    private Object propertyValue(Object entity, String property, String fieldName) {
        PropertyDescriptor descriptor = descriptors(entity.getClass()).get(property);
        if (descriptor == null || descriptor.getReadMethod() == null) {
            throw new IllegalArgumentException(fieldName + " 包含未知或不可读属性: "
                    + entity.getClass().getSimpleName() + "." + property);
        }
        try {
            if (!descriptor.getReadMethod().canAccess(entity)) {
                descriptor.getReadMethod().setAccessible(true);
            }
            return descriptor.getReadMethod().invoke(entity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("无法读取实体属性: "
                    + entity.getClass().getSimpleName() + "." + property, exception);
        }
    }

    private Map<String, PropertyDescriptor> descriptors(Class<?> entityClass) {
        return PROPERTY_DESCRIPTORS.computeIfAbsent(entityClass, type -> {
            try {
                return List.of(Introspector.getBeanInfo(type).getPropertyDescriptors()).stream()
                        .filter(descriptor -> !"class".equals(descriptor.getName()))
                        .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                PropertyDescriptor::getName, descriptor -> descriptor));
            } catch (IntrospectionException exception) {
                throw new IllegalStateException("无法分析实体属性: " + type.getName(), exception);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> C criteriaWithEntityFields(
            ILocalCrudService<E, C, PK> service,
            C source,
            E entity,
            List<String> fields
    ) {
        C criteria = source == null ? service.newCriteria() : source.copy();
        IWhereConditionCriteria whereCriteria = (IWhereConditionCriteria) criteria;
        for (String field : fields) {
            Object value = propertyValue(entity, field, "matchFields");
            if (value == null) whereCriteria.isNull(field);
            else whereCriteria.equals(field, value);
        }
        return criteria;
    }

    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> C effectiveUpdateCriteria(
            ILocalCrudService<E, C, PK> service,
            C source,
            E entity
    ) {
        if (source == null) {
            return criteriaWithId(service, null, idOf(entity));
        }
        C criteria = criteriaWithEntityFields(service, source, entity, source.resolveMatchFields());
        if (!((IWhereConditionCriteria<?, ?>) criteria).hasConditions()) {
            ((IIdCriteria<PK, E, C>) criteria).setId((PK) idOf(entity));
        }
        return criteria;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> C criteriaWithId(
            ILocalCrudService<E, C, PK> service,
            C source,
            Serializable id
    ) {
        C copy = source == null ? service.newCriteria() : source.copy();
        ((IIdCriteria) copy).setId(id);
        return copy;
    }
}
