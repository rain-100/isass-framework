// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.AddIfAbsentItem;
import vip.isass.framework.nocode.entity.CreateIfAbsentResult;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.SuperCudResult;
import vip.isass.framework.nocode.entity.UpdateByCriteriaItem;
import vip.isass.framework.nocode.lifecycle.CrudLifecycleRegistry;
import vip.isass.framework.nocode.lifecycle.CrudOperation;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Executes every standard nocode write through one validation and transaction boundary. */
public class CrudChangeExecutor {

    private final AssociationWriteCoordinator associations;
    private final TransactionTemplate conditionalCreateTransaction;

    public CrudChangeExecutor() {
        this(null, null);
    }

    public CrudChangeExecutor(AssociationWriteCoordinator associations) {
        this(associations, null);
    }

    public CrudChangeExecutor(AssociationWriteCoordinator associations,
                              PlatformTransactionManager transactionManager) {
        this.associations = associations;
        conditionalCreateTransaction = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        if (conditionalCreateTransaction != null) {
            conditionalCreateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> SuperCudResult<E> superCud(
            ILocalCrudService<E, C, PK> service,
            SuperCudReq<E, C> request
    ) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(request, "request");
        validate(request);
        return CrudLifecycleRegistry.execute(
                service,
                CrudOperation.SUPER_CUD,
                "superCud",
                new Object[]{request},
                () -> execute(service, request));
    }

    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> SuperCudResult<E> execute(
            ILocalCrudService<E, C, PK> service,
            SuperCudReq<E, C> request
    ) {
        if (request.isEmpty()) {
            return SuperCudResult.empty();
        }

        IRepository<E, C> repository = service.getRepository();
        List<E> added = new ArrayList<>(request.addEntities().size());
        List<CreateIfAbsentResult<E>> addIfAbsentResults =
                new ArrayList<>(request.addIfAbsentItems().size());
        List<E> updated = new ArrayList<>(request.updateEntities().size());
        List<Integer> updateCounts = new ArrayList<>(request.updateByCriteriaItems().size());
        List<Serializable> deletedIds = new ArrayList<>(request.deleteIds().size());
        List<Integer> deleteCounts = new ArrayList<>(request.deleteCriteria().size());

        if (!request.addEntities().isEmpty()) {
            service.prepareForInsert(request.addEntities());
            if (associations != null && associations.active()) {
                request.addEntities().forEach(entity -> associations.beforeSave(entity, true));
            }
            if (request.addEntities().size() == 1) {
                repository.add(request.addEntities().getFirst());
            } else {
                repository.addBatch(request.addEntities());
            }
            added.addAll(request.addEntities());
            if (associations != null && associations.active()) {
                request.addEntities().forEach(entity -> associations.afterSave(entity, null, true));
            }
        }

        for (AddIfAbsentItem<E, C> item : request.addIfAbsentItems()) {
            service.prepareForInsert(item.entity());
            if (associations != null && associations.active()) associations.beforeSave(item.entity(), true);
            boolean created = insertAndResolveUniqueConflict(repository, item.entity());
            E result = created ? item.entity() : repository.getByCriteria(item.criteria());
            if (result == null) {
                throw new IllegalStateException("条件新增未返回新记录或已存在记录");
            }
            addIfAbsentResults.add(new CreateIfAbsentResult<>(created, result));
            if (created && associations != null && associations.active()) associations.afterSave(result, null, true);
        }

        for (E entity : request.updateEntities()) {
            if (associations != null && associations.active()) associations.beforeSave(entity, false);
            if (!repository.updateById(entity)) {
                throw new AbsentException("按 ID 更新失败，记录不存在: " + idOf(entity));
            }
            updated.add(entity);
            if (associations != null && associations.active()) associations.afterSave(entity, null, false);
        }

        for (UpdateByCriteriaItem<E, C> item : request.updateByCriteriaItems()) {
            int affected = 0;
            for (E entity : item.entities()) {
                if (associations != null && associations.active()) associations.beforeSave(entity, false);
                Serializable id = idOfOrNull(entity);
                C effectiveCriteria = id == null
                        ? item.criteria()
                        : criteriaWithId(service, item.criteria(), id);
                int current = repository.updateCountByCriteria(entity, effectiveCriteria);
                if (id != null && current == 0) {
                    throw new AbsentException("按 Criteria 更新失败，记录不存在或不符合条件: " + id);
                }
                affected += current;
                if (associations != null && associations.active()) {
                    associations.afterSave(entity, item.criteria(), false);
                }
            }
            updateCounts.add(affected);
        }

        if (!request.deleteIds().isEmpty()) {
            if (associations != null && associations.active()) associations.beforeDelete(service, request.deleteIds());
            int affected = repository.deleteCountByIds(request.deleteIds());
            if (affected != request.deleteIds().size()) {
                throw new AbsentException("按 ID 删除失败，请求 " + request.deleteIds().size()
                        + " 条，实际删除 " + affected + " 条");
            }
            deletedIds.addAll(request.deleteIds());
        }

        for (C criteria : request.deleteCriteria()) {
            if (associations != null && associations.active()) {
                List<E> deleting = repository.findByCriteria(criteria);
                associations.beforeDelete(service, deleting.stream().map(IIdEntity::getId).toList());
            }
            deleteCounts.add(repository.deleteCountByCriteria(criteria));
        }

        return new SuperCudResult<>(added, addIfAbsentResults, updated, updateCounts, deletedIds, deleteCounts);
    }

    private <E extends vip.isass.framework.nocode.entity.IEntity<E>,
            C extends ICriteria<E, C>> boolean insertAndResolveUniqueConflict(
            IRepository<E, C> repository, E entity) {
        try {
            if (conditionalCreateTransaction == null) {
                repository.add(entity);
            } else {
                conditionalCreateTransaction.executeWithoutResult(status -> repository.add(entity));
            }
            return true;
        } catch (DuplicateKeyException duplicate) {
            // NESTED propagation rolls only the failed INSERT back to its savepoint, so the
            // surrounding superCud transaction remains usable for the remaining change groups.
            return false;
        }
    }

    private <E, C> void validate(SuperCudReq<E, C> request) {
        requireNoNulls(request.addEntities(), "addEntities");
        requireNoNulls(request.addIfAbsentItems(), "addIfAbsentItems");
        requireNoNulls(request.updateEntities(), "updateEntities");
        requireNoNulls(request.updateByCriteriaItems(), "updateByCriteriaItems");
        requireNoNulls(request.deleteIds(), "deleteIds");
        requireNoNulls(request.deleteCriteria(), "deleteCriteria");

        Set<Serializable> addedIds = collectIds(request.addEntities(), false, "addEntities");
        Set<Serializable> updatedIds = collectIds(request.updateEntities(), true, "updateEntities");
        Set<Serializable> deletedIds = new LinkedHashSet<>();
        for (Serializable id : request.deleteIds()) {
            requireUsableId(id, "deleteIds");
            if (!deletedIds.add(id)) {
                throw new IllegalArgumentException("deleteIds 包含重复 ID: " + id);
            }
        }
        rejectOverlap(addedIds, updatedIds, "addEntities", "updateEntities");
        rejectOverlap(addedIds, deletedIds, "addEntities", "deleteIds");
        rejectOverlap(updatedIds, deletedIds, "updateEntities", "deleteIds");

        for (AddIfAbsentItem<E, C> item : request.addIfAbsentItems()) {
            if (item.entity() == null) {
                throw new IllegalArgumentException("addIfAbsentItems.entity 不能为空");
            }
            requireCriteria(item.criteria(), "addIfAbsentItems.criteria");
        }
        for (UpdateByCriteriaItem<E, C> item : request.updateByCriteriaItems()) {
            if (item.entities().isEmpty()) {
                throw new IllegalArgumentException("updateByCriteriaItems.entities 不能为空");
            }
            requireNoNulls(item.entities(), "updateByCriteriaItems.entities");
            long withoutId = item.entities().stream().filter(entity -> idOfOrNull(entity) == null).count();
            if (withoutId > 0 && item.entities().size() != 1) {
                throw new IllegalArgumentException("Criteria 批量更新包含无 ID 实体时只能提交一个实体");
            }
            if (withoutId > 0) {
                requireCriteria(item.criteria(), "updateByCriteriaItems.criteria");
            }
            collectIds(item.entities(), false, "updateByCriteriaItems.entities");
        }
        request.deleteCriteria().forEach(criteria -> requireCriteria(criteria, "deleteCriteria"));
    }

    private void requireCriteria(Object criteria, String field) {
        if (!(criteria instanceof IWhereConditionCriteria<?, ?> whereCriteria)) {
            throw new IllegalArgumentException(field + " 必须实现 IWhereConditionCriteria");
        }
        if (!whereCriteria.hasConditions()) {
            throw new IllegalArgumentException(field + " 必须至少包含一个有效条件");
        }
    }

    private <E> Set<Serializable> collectIds(List<E> entities, boolean required, String field) {
        Set<Serializable> ids = new LinkedHashSet<>();
        for (E entity : entities) {
            Serializable id = idOfOrNull(entity);
            if (required) {
                requireUsableId(id, field);
            }
            if (id != null && !ids.add(id)) {
                throw new IllegalArgumentException(field + " 包含重复 ID: " + id);
            }
        }
        return ids;
    }

    private void rejectOverlap(Set<Serializable> first, Set<Serializable> second,
                               String firstName, String secondName) {
        Set<Serializable> overlap = new HashSet<>(first);
        overlap.retainAll(second);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException(firstName + " 与 " + secondName + " 存在冲突 ID: " + overlap);
        }
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
