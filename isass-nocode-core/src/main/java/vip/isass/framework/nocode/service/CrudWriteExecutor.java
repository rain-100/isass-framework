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
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.CriteriaMetadata;
import vip.isass.framework.common.criteria.IUpdateCriteria;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.criteria.type.IOrderByCriteria;
import vip.isass.framework.common.criteria.type.IPageCriteria;
import vip.isass.framework.common.criteria.type.IReturnFieldCriteria;
import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.SuperCudResult;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleListener;
import vip.isass.framework.database.core.repository.IRepository;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    public <PK extends Serializable, E extends IEntity<E>,
            C extends ICriteria<E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> SuperCudResult superCud(ILocalCrudService<E, C, PK> service,
                                                                                            SuperCudReq<E, C, PK> request
    ) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(request, "request");
        Set<Object> active = ACTIVE_SERVICES.get();
        boolean firstInvocation = active.add(service);
        if (!firstInvocation && (associations == null || !associations.nestedInvocation())) {
            validate(request);
            validateCapabilities(service, request);
            return execute(service, request);
        }
        try {
            CrudWriteLifecycleContext<E, C, PK> context = new CrudWriteLifecycleContext<>(service, request);
            List<CrudWriteLifecycleListener> supported = listeners.stream()
                    .filter(listener -> listener.supports(context)).toList();
            boolean transactionCallbacks = registerTransactionCallbacks(supported, context);
            try {
                validate(request);
                validateCapabilities(service, request);
                if (associations != null) {
                    request.addEntities().forEach(entity -> associations.authorizeGraph(entity, null, true, null));
                    for (E entity : request.updateEntities()) {
                        C criteria = effectiveUpdateCriteria(service, request.updateCriteria(), entity);
                        PK rootId = validateAndResolveRelatedUpdateRootId(entity, criteria);
                        if (rootId != null && repositoryLock(service, rootId) == null) {
                            throw new AbsentException("关联更新的主实体不存在: " + rootId);
                        }
                        associations.authorizeGraph(entity, criteria, false, rootId);
                    }
                    // 外键指向的新增对象先在当前事务内保存、回填 ID，再执行依赖其存在性的业务校验。
                    if (request.addByFields().isEmpty()) {
                        request.addEntities().forEach(entity -> associations.beforeSave(entity, true));
                    }
                    for (E entity : request.updateEntities()) {
                        C criteria = effectiveUpdateCriteria(service, request.updateCriteria(), entity);
                        associations.beforeSave(entity, false, validateAndResolveRelatedUpdateRootId(entity, criteria), criteria);
                    }
                }
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
            if (firstInvocation) active.remove(service);
            if (active.isEmpty()) ACTIVE_SERVICES.remove();
        }
    }

    private boolean registerTransactionCallbacks(List<CrudWriteLifecycleListener> supported,
                                                 CrudWriteLifecycleContext<?, ?, ?> context) {
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
                                   CrudWriteLifecycleContext<?, ?, ?> context) {
        for (CrudWriteLifecycleListener listener : supported) {
            try {
                listener.afterCommit(context);
            } catch (RuntimeException | Error error) {
                LOGGER.error("NoCode 写生命周期 afterCommit 执行失败: {}", listener.getClass().getName(), error);
            }
        }
    }

    private void notifyAfterRollback(List<CrudWriteLifecycleListener> supported,
                                     CrudWriteLifecycleContext<?, ?, ?> context,
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

    private <PK extends Serializable, E extends IEntity<E>,
            C extends ICriteria<E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> SuperCudResult execute(
            ILocalCrudService<E, C, PK> service,
            SuperCudReq<E, C, PK> request
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
            C effectiveCriteria = effectiveUpdateCriteria(service, request.updateCriteria(), entity);
            PK updateRootId = validateAndResolveRelatedUpdateRootId(entity, effectiveCriteria);
            if (updateRootId != null && !repository.isPresentByCriteria(effectiveCriteria)) {
                throw new AbsentException("关联更新的主实体未命中最终 Criteria: " + updateRootId);
            }
            if (associations != null && associations.active()) {
                associations.beforeSave(entity, false, updateRootId, effectiveCriteria);
            }
            int current = onlySubmittedAssociation(entity)
                    ? (repository.isPresentByCriteria(effectiveCriteria) ? 1 : 0)
                    : repository.updateCountByCriteria(entity, effectiveCriteria);
            if (current == 0 && updateRootId != null && !repository.isPresentByCriteria(effectiveCriteria)) {
                throw new AbsentException("关联更新后主实体不再命中最终 Criteria: " + updateRootId);
            }
            if (current == 0 && request.updateCriteria() == null) {
                throw new AbsentException("按 ID 更新失败，记录不存在: " + idOf(entity));
            }
            updatedCount += current;
            if (associations != null && associations.active()) {
                associations.afterSave(entity, effectiveCriteria, false, updateRootId);
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
            if (!CrudIdSupport.supportsId(service)) {
                deletedCount += repository.deleteCountByCriteria(criteria);
                continue;
            }
            LinkedHashSet<Serializable> rootIds = new LinkedHashSet<>();
            C selection = criteria.copy();
            if (selection instanceof IReturnFieldCriteria<?, ?> columns) {
                columns.setReturnField("id");
            }
            for (E deleting : repository.findByCriteria(selection)) {
                Serializable rootId = CrudIdSupport.requireIdEntity(deleting, "按条件固定 ID 删除").getId();
                if (rootId != null) rootIds.add(rootId);
            }
            if (rootIds.isEmpty()) continue;
            List<Serializable> fixedIds = List.copyOf(rootIds);
            for (int offset = 0; offset < fixedIds.size(); offset += 500) {
                List<Serializable> batch = fixedIds.subList(offset, Math.min(offset + 500, fixedIds.size()));
                if (associations != null && associations.active()) associations.beforeDelete(service, batch);
                deletedCount += repository.deleteCountByIds(batch);
            }
        }

        return new SuperCudResult(addedCount, updatedCount, deletedCount);
    }

    private <E extends IEntity<E>, C extends ICriteria<E, C>>
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

    private void validate(SuperCudReq<?, ?, ?> request) {
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
            request.updateEntities().forEach(entity -> CrudIdSupport.requireId(entity, "updateEntities"));
        }
        if (!request.updateEntities().isEmpty() && request.updateCriteria() != null) {
            validateWriteOptions(request.updateCriteria());
            IWhereConditionCriteria<?, ?> whereCriteria = requireWhereCriteria(
                    request.updateCriteria(), "updateCriteria");
            List<String> matchFields = ((IUpdateCriteria<?>) request.updateCriteria()).resolveMatchFields();
            if (!whereCriteria.hasConditions() && matchFields.isEmpty()) {
                request.updateEntities().forEach(entity -> CrudIdSupport.requireId(entity, "updateEntities"));
            }
        }
        request.deleteCriteria().forEach(criteria -> {
            validateWriteOptions(criteria);
            requireCriteria(criteria, "deleteCriteria");
        });
    }

    private <PK extends Serializable, E extends IEntity<E>,
            C extends ICriteria<E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> void validateCapabilities(
            ILocalCrudService<E, C, PK> service, SuperCudReq<E, C, PK> request) {
        if (!request.deleteIds().isEmpty()) {
            CrudIdSupport.requireIdEntity(service, "deleteIds");
        }
        for (E entity : request.updateEntities()) {
            C effective = effectiveUpdateCriteria(service, request.updateCriteria(), entity);
            validateAndResolveRelatedUpdateRootId(entity, effective);
        }
        if (associations != null && associations.active()) {
            request.addEntities().forEach(entity -> associations.validateSaveCapabilities(entity, true));
            request.updateEntities().forEach(entity -> associations.validateSaveCapabilities(entity, false));
        }
        if (!request.deleteCriteria().isEmpty() && !CrudIdSupport.supportsId(service)
                && CriteriaMetadata.associations(CrudServiceTypeResolver.resolveEntityClass(service.getClass()))
                .stream().anyMatch(EntityAssociation::cascadeDelete)) {
            throw new UnsupportedOperationException("级联删除要求主实体实现 IIdEntity");
        }
    }

    private void validateWriteOptions(Object criteria) {
        if (criteria instanceof IOrderByCriteria<?, ?> order && order.getOrderBy() != null
                && !order.getOrderBy().isBlank()) {
            throw new IllegalArgumentException("更新/删除不支持 orderBy");
        }
        if (criteria instanceof IPageCriteria<?, ?> page
                && (!Objects.equals(page.getPageNum(), IPageCriteria.DEFAULT_PAGE_NUM)
                || !Objects.equals(page.getPageSize(), IPageCriteria.DEFAULT_PAGE_SIZE)
                || !Objects.equals(page.getSearchCountFlag(), IPageCriteria.DEFAULT_SEARCH_COUNT_FLAG))) {
            throw new IllegalArgumentException("更新/删除不支持分页和 count 选项");
        }
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
        return CrudIdSupport.requireId(entity, "entity");
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
                        .collect(Collectors.toUnmodifiableMap(
                                PropertyDescriptor::getName, descriptor -> descriptor));
            } catch (IntrospectionException exception) {
                throw new IllegalStateException("无法分析实体属性: " + type.getName(), exception);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <PK extends Serializable, E extends IEntity<E>,
            C extends ICriteria<E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> C criteriaWithEntityFields(
            ILocalCrudService<E, C, PK> service,
            C source,
            E entity,
            List<String> fields
    ) {
        C criteria = source == null ? service.newCriteria() : source.copy();
        IWhereConditionCriteria<?, ?> whereCriteria = requireWhereCriteria(criteria, "写入 Criteria");
        for (String field : fields) {
            Object value = propertyValue(entity, field, "matchFields");
            if (value == null) whereCriteria.isNull(field);
            else whereCriteria.equals(field, value);
        }
        return criteria;
    }

    private <PK extends Serializable, E extends IEntity<E>,
            C extends ICriteria<E, C> & IUpdateCriteria<C>
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
            CrudIdSupport.equalsId(service, criteria, idOf(entity));
        }
        return criteria;
    }

    @SuppressWarnings("unchecked")
    private <PK extends Serializable, E extends IEntity<E>,
            C extends ICriteria<E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> PK validateAndResolveRelatedUpdateRootId(
            E entity, C criteria) {
        boolean submittedRelation = false;
        for (EntityAssociation association : entity.associations()) {
            if (entity.isPropertyPresent(association.property())
                    && propertyValue(entity, association.property(), "关联属性") != null) {
                submittedRelation = true;
                break;
            }
        }
        if (!submittedRelation) return null;
        CrudIdSupport.requireIdEntity(entity, "更新关联对象");
        if (!(criteria instanceof IWhereConditionCriteria<?, ?> where)) {
            throw new IllegalArgumentException("更新关联对象必须以 Criteria 明确指定主实体 ID");
        }
        Serializable rootId = null;
        for (WhereCondition node : where.getWhereConditions()) {
            if (node.getCondition() == Condition.Logical.OR && node.getChildren() == null) {
                throw new IllegalArgumentException("更新关联对象的 OR 条件可能扩大主实体 ID 范围");
            }
            if ("id".equals(node.getSourceProperty()) && node.getCondition() == Condition.Compare.EQUAL
                    && node.getTargetProperty() == null && node.getTargetCriteria() == null
                    && node.getValue() instanceof Serializable value) {
                if (rootId != null && !String.valueOf(rootId).equals(String.valueOf(value))) {
                    throw new IllegalArgumentException("更新关联对象的 Criteria 包含冲突的主实体 ID");
                }
                rootId = value;
            }
        }
        requireUsableId(rootId, "更新关联对象的 Criteria.id");
        Serializable entityId = CrudIdSupport.requireIdEntity(entity, "更新关联对象").getId();
        if (entityId != null && !String.valueOf(entityId).equals(String.valueOf(rootId))) {
            throw new IllegalArgumentException("更新实体 ID 与 Criteria 主实体 ID 不一致");
        }
        return (PK) rootId;
    }

    public static boolean onlySubmittedAssociation(IEntity<?> entity) {
        Set<String> relations = entity.associations().stream().map(EntityAssociation::property)
                .collect(Collectors.toSet());
        if (relations.isEmpty() || entity.presentProperties().stream().noneMatch(relations::contains)) return false;
        return entity.presentProperties().stream().allMatch(property -> "id".equals(property) || relations.contains(property));
    }

    private Object repositoryLock(ILocalCrudService<?, ?, ?> service, Serializable id) {
        return service.getRepository().getEntityByIdForUpdate(id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <PK extends Serializable, E extends IEntity<E>,
            C extends ICriteria<E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> C criteriaWithId(
            ILocalCrudService<E, C, PK> service,
            C source,
            Serializable id
    ) {
        C copy = source == null ? service.newCriteria() : source.copy();
        return CrudIdSupport.equalsId(service, copy, id);
    }
}
