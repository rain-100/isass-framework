// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.common.criteria.IUpdateCriteria;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.NullValueMode;
import vip.isass.framework.common.criteria.UpdateMode;
import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.entity.IParentIdEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;

import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.BiConsumer;

/**
 * Saves declared entity graphs, including fixed predicates and directional cascade deletion.
 */
public final class AssociationWriteCoordinator {

    private final Map<Class<?>, ILocalCrudService<?, ?, ?>> services;
    private final ThreadLocal<Integer> nesting = ThreadLocal.withInitial(() -> 0);
    private final BiConsumer<ILocalCrudService<?, ?, ?>, SuperCudReq<?, ?, ?>> authorizer;

    public AssociationWriteCoordinator(Collection<ILocalCrudService<?, ?, ?>> localServices) {
        this(localServices, (service, request) -> { });
    }

    public AssociationWriteCoordinator(Collection<ILocalCrudService<?, ?, ?>> localServices,
                                      BiConsumer<ILocalCrudService<?, ?, ?>, SuperCudReq<?, ?, ?>> authorizer) {
        Map<Class<?>, ILocalCrudService<?, ?, ?>> indexed = new LinkedHashMap<>();
        for (ILocalCrudService<?, ?, ?> service : localServices) {
            indexed.put(CrudServiceTypeResolver.resolveEntityClass(service.getClass()), service);
        }
        services = Map.copyOf(indexed);
        this.authorizer = authorizer;
    }

    public boolean active() {
        return true;
    }

    public boolean nestedInvocation() {
        return nesting.get() > 0;
    }

    /** Check ID-dependent nested writes before inserting or updating any aggregate row. */
    public void validateSaveCapabilities(IEntity<?> source, boolean creating) {
        validateSaveCapabilities(source, creating, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    private void validateSaveCapabilities(IEntity<?> source, boolean creating, Set<Object> path, int depth) {
        if (depth > 32 || !path.add(source)) {
            throw new IllegalArgumentException("关联写入存在循环或超过最大深度 32");
        }
        try {
            for (EntityAssociation association : source.associations()) {
                Object value = read(source, association.property());
                if (!submitted(source, association, value, creating)) {
                    continue;
                }
                CrudIdSupport.requireIdEntity(source, "关联写入");
                CrudIdSupport.requireIdEntityType(association.targetType(), "关联写入目标");
                Set<Serializable> ids = new LinkedHashSet<>();
                for (Object target : association.kind() == EntityAssociation.Kind.MANY
                        ? collection(value) : List.of(value)) {
                    if (!association.targetType().isInstance(target)) {
                        throw new IllegalArgumentException("关联对象类型不匹配: " + association.property());
                    }
                    CrudIdSupport.requireIdEntity(target, "关联写入目标");
                    if (id(target) != null && !ids.add(id(target))) {
                        throw new IllegalArgumentException("关联集合包含重复 ID: " + id(target));
                    }
                    AssociationSupport.fixedValues(association).forEach((field, required) -> {
                        Object actual = read(target, field);
                        if (actual != null && !Objects.equals(actual, required)) {
                            throw new IllegalArgumentException("关联固定条件不允许修改: " + field);
                        }
                    });
                    validateSaveCapabilities((IEntity<?>) target, id(target) == null, path, depth + 1);
                }
            }
        } finally {
            path.remove(source);
        }
    }

    public void beforeSave(IEntity<?> source, boolean creating) {
        beforeSave(source, creating, null);
    }

    public void beforeSave(IEntity<?> source, boolean creating, Serializable updateRootId) {
        beforeSave(source, creating, updateRootId, null);
    }

    public void beforeSave(IEntity<?> source, boolean creating, Serializable updateRootId,
                           IUpdateCriteria<?> criteria) {
        if (!active()) return;
        validateTreeParent(source, updateRootId);
        for (EntityAssociation association : source.associations()) {
            Object submitted = read(source, association.property());
            if (!submitted(source, association, submitted, creating)
                    || association.kind() != EntityAssociation.Kind.ONE
                    || "id".equals(association.localField()) || !"id".equals(association.targetField())) {
                continue;
            }
            Object targetId = id(submitted);
            ILocalCrudService targetService = targetService(association);
            if (targetId == null) {
                authorizer.accept(targetService, SuperCudReq.add((IEntity<?>) submitted));
                Object created = nested(() -> targetService.create((IIdEntity) submitted));
                targetId = id(created);
            } else {
                Object existing = targetService.getRepository().getEntityById((Serializable) targetId);
                if (existing == null) {
                    throw new IllegalArgumentException("关联对象不存在: " + targetId);
                }
                if (changed((IEntity<?>) submitted, existing)) {
                    updateExistingTarget(targetService, submitted, (Serializable) targetId, criteria);
                }
            }
            Object supplied = read(source, association.localField());
            if (supplied != null && !Objects.equals(supplied, targetId)) {
                throw new IllegalArgumentException("关联对象与关联 ID 不一致: " + association.property());
            }
            write(source, association.localField(), targetId);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void validateTreeParent(IEntity<?> source, Serializable updateRootId) {
        if (!(source instanceof IParentIdEntity parentEntity)) return;
        Serializable sourceId = updateRootId == null ? id(source) : updateRootId;
        if (sourceId == null) return;
        Object parentId = parentEntity.getParentId();
        if (parentId == null) return;
        if (sourceId.equals(parentId)) throw new IllegalArgumentException("节点不能把自己设为父节点");
        ILocalCrudService service = services.get(source.getClass());
        if (service == null) return;
        Set<Object> visited = new LinkedHashSet<>();
        Object cursor = parentId;
        for (int depth = 0; cursor != null; depth++) {
            if (depth > 1000 || !visited.add(cursor)) {
                throw new IllegalStateException("树形父链存在循环或超过最大深度");
            }
            if (sourceId.equals(cursor)) throw new IllegalArgumentException("节点不能移动到自己的子孙节点下");
            Object parent = service.getRepository().getEntityById((Serializable) cursor);
            if (!(parent instanceof IParentIdEntity next)) break;
            cursor = next.getParentId();
        }
    }

    public void afterSave(IEntity<?> source, IUpdateCriteria<?> criteria, boolean creating) {
        afterSave(source, criteria, creating, null);
    }

    public void afterSave(IEntity<?> source, IUpdateCriteria<?> criteria, boolean creating,
                          Serializable updateRootId) {
        if (!active()) return;
        UpdateMode mode = criteria == null ? UpdateMode.MERGE : criteria.resolveUpdateMode();
        for (EntityAssociation association : source.associations()) {
            Object submitted = read(source, association.property());
            if (!submitted(source, association, submitted, creating)
                    || !"id".equals(association.localField())) continue;
            Object sourceKey = !creating && updateRootId != null
                    ? updateRootId : read(source, association.localField());
            if (sourceKey == null) throw new IllegalStateException("保存关联前当前实体 ID 为空");
            List<Object> targets = association.kind() == EntityAssociation.Kind.MANY
                    ? collection(submitted) : List.of(submitted);
            Set<Serializable> submittedIds = new LinkedHashSet<>();
            ILocalCrudService targetService = targetService(association);
            for (Object target : targets) {
                AssociationSupport.constrain((IEntity<?>) target, association, sourceKey);
                Serializable targetId = resolveTargetId(target, association, targetService, sourceKey);
                if (targetId == null) {
                    authorizer.accept(targetService, SuperCudReq.add((IEntity<?>) target));
                    nested(() -> targetService.create((IIdEntity) target));
                    targetId = id(target);
                    if (targetId == null) {
                        throw new IllegalStateException("新增关联对象后 ID 仍为空");
                    }
                    submittedIds.add(targetId);
                } else {
                    Object existing = targetService.getRepository().getEntityById(targetId);
                    if (existing == null || !AssociationSupport.matches(existing, association, sourceKey)) {
                        throw new IllegalArgumentException("关联对象不存在或不属于当前实体: " + targetId);
                    }
                    if (changed((IEntity<?>) target, existing)) {
                        updateExistingTarget(targetService, target, targetId, null);
                    }
                    submittedIds.add(targetId);
                }
            }
            if (mode == UpdateMode.REPLACE) {
                Object targetCriteria = targetService.newCriteria();
                IWhereConditionCriteria where = (IWhereConditionCriteria) targetCriteria;
                where.equals(association.targetField(), sourceKey);
                AssociationSupport.fixedValues(association).forEach(where::equals);
                if (!submittedIds.isEmpty()) where.notIn("id", submittedIds);
                deleteSelected(targetService, (ICriteria) targetCriteria);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void updateExistingTarget(ILocalCrudService targetService, Object target,
                                      Serializable targetId, IUpdateCriteria<?> sourceCriteria) {
        authorizer.accept(targetService, SuperCudReq.update((IEntity<?>) target));
        if (sourceCriteria == null || sourceCriteria.resolveNullValueMode() == NullValueMode.IGNORE_NULL) {
            nested(() -> targetService.update((IIdEntity) target));
            return;
        }
        Object targetCriteria = targetService.newCriteria();
        ((IWhereConditionCriteria) targetCriteria).equals("id", targetId);
        ((IUpdateCriteria) targetCriteria).setNullValueMode(NullValueMode.WRITE_NULL);
        nested(() -> targetService.update((IIdEntity) target,
                (ICriteria) targetCriteria));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Serializable resolveTargetId(Object target, EntityAssociation association,
                                         ILocalCrudService targetService, Object sourceKey) {
        Serializable targetId = id(target);
        if (targetId != null || association.kind() != EntityAssociation.Kind.ONE) {
            return targetId;
        }
        Object targetCriteria = targetService.newCriteria();
        ((IWhereConditionCriteria) targetCriteria).equals(association.targetField(), sourceKey);
        AssociationSupport.fixedValues(association).forEach(((IWhereConditionCriteria) targetCriteria)::equals);
        List<?> existingTargets = targetService.getRepository().findByCriteria(
                (ICriteria) targetCriteria);
        if (existingTargets.size() > 1) {
            throw new IllegalStateException("单体关联存在多条目标记录: "
                    + association.targetType().getName() + "." + association.targetField() + "=" + sourceKey);
        }
        if (existingTargets.isEmpty()) {
            return null;
        }
        targetId = id(existingTargets.getFirst());
        if (targetId == null) {
            throw new IllegalStateException("单体关联目标记录 ID 为空: " + association.targetType().getName());
        }
        ((IIdEntity) target).setId(targetId);
        return targetId;
    }

    private boolean submitted(IEntity<?> source, EntityAssociation association,
                              Object value, boolean creating) {
        return value != null && (creating || source.isPropertyPresent(association.property()));
    }

    /**
     * Pre-authorizes all descendant changes before the first write in a submitted graph.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void authorizeGraph(IEntity<?> source, IUpdateCriteria<?> criteria,
                               boolean creating, Serializable updateRootId) {
        Object sourceKey = updateRootId == null ? id(source) : updateRootId;
        for (EntityAssociation association : source.associations()) {
            Object value = read(source, association.property());
            if (!submitted(source, association, value, creating)) {
                continue;
            }
            ILocalCrudService service = targetService(association);
            Set<Serializable> retained = new LinkedHashSet<>();
            for (Object raw : association.kind() == EntityAssociation.Kind.MANY ? collection(value) : List.of(value)) {
                IEntity<?> target = (IEntity<?>) raw;
                if ("id".equals(association.localField()) && sourceKey != null) {
                    AssociationSupport.constrain(target, association, sourceKey);
                    resolveTargetId(target, association, service, sourceKey);
                }
                Serializable targetId = (Serializable) id(target);
                Object stored = targetId == null ? null : service.getRepository().getEntityById(targetId);
                if (targetId != null) {
                    retained.add(targetId);
                    if (stored == null || "id".equals(association.localField()) && sourceKey != null
                            && !AssociationSupport.matches(stored, association, sourceKey)) {
                        throw new IllegalArgumentException("关联对象不存在或不属于当前实体: " + targetId);
                    }
                }
                if (targetId == null || changed(target, stored)) {
                    authorizer.accept(service, targetId == null ? SuperCudReq.add(target) : SuperCudReq.update(target));
                }
                authorizeGraph(target, null, targetId == null, null);
            }
            if (!creating && sourceKey != null && "id".equals(association.localField())
                    && criteria != null && criteria.resolveUpdateMode() == UpdateMode.REPLACE) {
                IWhereConditionCriteria selection = (IWhereConditionCriteria) service.newCriteria();
                selection.equals(association.targetField(), sourceKey);
                AssociationSupport.fixedValues(association).forEach(selection::equals);
                List<?> existing = service.getRepository().findByCriteria((ICriteria) selection);
                List<Serializable> removed = existing.stream()
                        .map(this::id)
                        .map(id -> (Serializable) id)
                        .filter(id -> !retained.contains(id))
                        .toList();
                if (!removed.isEmpty()) {
                    authorizer.accept(service, SuperCudReq.deleteAll(removed));
                }
            }
        }
    }

    private boolean changed(IEntity<?> target, Object stored) {
        return target.presentProperties().stream()
                .filter(property -> !"id".equals(property))
                .anyMatch(property -> target.associations().stream()
                        .anyMatch(association -> association.property().equals(property)
                                && read(target, property) != null)
                        || !Objects.equals(read(target, property), read(stored, property)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void deleteSelected(ILocalCrudService targetService, ICriteria criteria) {
        List<?> selected = targetService.getRepository().findByCriteria(criteria);
        List<Serializable> ids = selected.stream()
                .map(this::id)
                .filter(Objects::nonNull)
                .map(id -> (Serializable) id)
                .distinct()
                .toList();
        if (!ids.isEmpty()) {
            SuperCudReq request = SuperCudReq.deleteAll(ids);
            authorizer.accept(targetService, request);
            nested(() -> targetService.superCud(request));
        }
    }

    public void beforeDelete(ILocalCrudService<?, ?, ?> sourceService, Collection<? extends Serializable> ids) {
        if (!active() || ids.isEmpty()) return;
        List<Serializable> batch = new ArrayList<>(500);
        for (Serializable id : ids) {
            batch.add(id);
            if (batch.size() == 500) {
                cascadeRootBatch(sourceService, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) cascadeRootBatch(sourceService, batch);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void cascadeRootBatch(ILocalCrudService<?, ?, ?> sourceService, List<Serializable> ids) {
        Object sourceCriteria = sourceService.newCriteria();
        ((IWhereConditionCriteria) sourceCriteria).in("id", ids);
        List<?> sources = sourceService.getRepository().findByCriteria(
                (ICriteria) sourceCriteria);
        if (sources.isEmpty()) return;
        IEntity<?> first = (IEntity<?>) sources.getFirst();
        for (EntityAssociation association : first.associations()) {
            if (!association.cascadeDelete()) continue;
            Set<Object> keys = new LinkedHashSet<>();
            for (Object source : sources) {
                Object key = read(source, association.localField());
                if (key != null) keys.add(key);
            }
            if (keys.isEmpty()) continue;
            ILocalCrudService targetService = targetService(association);
            Object targetCriteria = targetService.newCriteria();
            ((IWhereConditionCriteria) targetCriteria).in(association.targetField(), keys);
            AssociationSupport.fixedValues(association).forEach(((IWhereConditionCriteria) targetCriteria)::equals);
            deleteSelected(targetService, (ICriteria) targetCriteria);
        }
    }

    @SuppressWarnings("rawtypes")
    private ILocalCrudService targetService(EntityAssociation association) {
        ILocalCrudService service = services.get(association.targetType());
        if (service == null) throw new IllegalStateException(
                "关联目标没有本地 ILocalCrudService: " + association.targetType().getName());
        return service;
    }

    private <T> T nested(Supplier<T> action) {
        if (nesting.get() >= 32) {
            throw new IllegalArgumentException("关联写入或删除超过最大深度 32");
        }
        nesting.set(nesting.get() + 1);
        try {
            return action.get();
        } finally {
            int value = nesting.get() - 1;
            if (value == 0) nesting.remove();
            else nesting.set(value);
        }
    }

    private List<Object> collection(Object value) {
        if (!(value instanceof Collection<?> values)) {
            throw new IllegalArgumentException("列表关联属性必须是 Collection");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("关联集合不能包含 null");
        }
        return new ArrayList<>(values);
    }

    private Serializable id(Object value) {
        return value instanceof IIdEntity<?, ?> entity ? (Serializable) entity.getId() : null;
    }

    private Object read(Object bean, String property) {
        try {
            Method method = descriptor(bean.getClass(), property).getReadMethod();
            if (method == null) throw new IllegalArgumentException("属性不可读: " + property);
            if (!method.canAccess(bean)) method.setAccessible(true);
            return method.invoke(bean);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取属性失败: " + property, exception);
        }
    }

    private void write(Object bean, String property, Object value) {
        try {
            Method method = descriptor(bean.getClass(), property).getWriteMethod();
            if (method == null) throw new IllegalArgumentException("属性不可写: " + property);
            if (!method.canAccess(bean)) method.setAccessible(true);
            method.invoke(bean, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("写入属性失败: " + property, exception);
        }
    }

    private PropertyDescriptor descriptor(Class<?> type, String property) {
        try {
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                if (descriptor.getName().equals(property)) return descriptor;
            }
            throw new IllegalArgumentException("实体不存在属性: " + type.getName() + "." + property);
        } catch (IntrospectionException exception) {
            throw new IllegalStateException("无法分析实体属性: " + type.getName(), exception);
        }
    }
}
