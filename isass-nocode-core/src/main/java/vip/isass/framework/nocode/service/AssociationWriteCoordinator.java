// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.criteria.UpdateMode;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.EntityAssociation;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.IParentIdEntity;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Saves direct aggregate members and applies fixed directional cascade-delete metadata.
 */
public final class AssociationWriteCoordinator {

    private final Map<Class<?>, ILocalCrudService<?, ?, ?>> services;
    private final ThreadLocal<Integer> nesting = ThreadLocal.withInitial(() -> 0);

    public AssociationWriteCoordinator(Collection<ILocalCrudService<?, ?, ?>> localServices) {
        Map<Class<?>, ILocalCrudService<?, ?, ?>> indexed = new LinkedHashMap<>();
        for (ILocalCrudService<?, ?, ?> service : localServices) {
            indexed.put(CrudServiceTypeResolver.resolveEntityClass(service.getClass()), service);
        }
        services = Map.copyOf(indexed);
    }

    public boolean active() {
        return nesting.get() == 0;
    }

    public void beforeSave(IEntity<?> source, boolean creating) {
        if (!active()) return;
        validateTreeParent(source);
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
                Object created = nested(() -> targetService.create((IIdEntity) submitted));
                targetId = id(created);
            } else {
                Object existing = targetService.getRepository().getEntityById((Serializable) targetId);
                if (existing == null) {
                    throw new IllegalArgumentException("关联对象不存在: " + targetId);
                }
                nested(() -> targetService.update((IIdEntity) submitted));
            }
            write(source, association.localField(), targetId);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void validateTreeParent(IEntity<?> source) {
        if (!(source instanceof IParentIdEntity parentEntity) || id(source) == null) return;
        Serializable sourceId = id(source);
        Object parentId = parentEntity.getParentId();
        if (parentId == null || "0".equals(String.valueOf(parentId))) return;
        if (sourceId.equals(parentId)) throw new IllegalArgumentException("节点不能把自己设为父节点");
        ILocalCrudService service = services.get(source.getClass());
        if (service == null) return;
        Set<Object> visited = new LinkedHashSet<>();
        Object cursor = parentId;
        for (int depth = 0; cursor != null && !"0".equals(String.valueOf(cursor)); depth++) {
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
        if (!active()) return;
        UpdateMode mode = criteria == null ? UpdateMode.MERGE : criteria.resolveUpdateMode();
        for (EntityAssociation association : source.associations()) {
            Object submitted = read(source, association.property());
            if (!submitted(source, association, submitted, creating)
                    || !"id".equals(association.localField())) continue;
            Object sourceKey = read(source, association.localField());
            if (sourceKey == null) throw new IllegalStateException("保存关联前当前实体 ID 为空");
            List<Object> targets = association.kind() == EntityAssociation.Kind.MANY
                    ? collection(submitted) : List.of(submitted);
            Set<Serializable> submittedIds = new LinkedHashSet<>();
            ILocalCrudService targetService = targetService(association);
            for (Object target : targets) {
                write(target, association.targetField(), sourceKey);
                Serializable targetId = id(target);
                if (targetId == null) {
                    nested(() -> targetService.create((IIdEntity) target));
                    targetId = id(target);
                    if (targetId == null) {
                        throw new IllegalStateException("新增关联对象后 ID 仍为空");
                    }
                    submittedIds.add(targetId);
                } else {
                    Object existing = targetService.getRepository().getEntityById(targetId);
                    if (existing == null || !java.util.Objects.equals(
                            read(existing, association.targetField()), sourceKey)) {
                        throw new IllegalArgumentException("关联对象不存在或不属于当前实体: " + targetId);
                    }
                    nested(() -> targetService.update((IIdEntity) target));
                    submittedIds.add(targetId);
                }
            }
            if (mode == UpdateMode.REPLACE) {
                Object targetCriteria = targetService.newCriteria();
                IWhereConditionCriteria where = (IWhereConditionCriteria) targetCriteria;
                where.equals(association.targetField(), sourceKey);
                if (!submittedIds.isEmpty()) where.notIn("id", submittedIds);
                nested(() -> targetService.deleteBatch((vip.isass.framework.nocode.criteria.ICriteria) targetCriteria));
            }
        }
    }

    private boolean submitted(IEntity<?> source, EntityAssociation association,
                              Object value, boolean creating) {
        return value != null && (creating || source.isPropertyPresent(association.property()));
    }

    public void beforeDelete(ILocalCrudService<?, ?, ?> sourceService, Collection<? extends Serializable> ids) {
        if (!active() || ids.isEmpty()) return;
        for (Serializable id : ids) {
            Object source = sourceService.getRepository().getEntityById(id);
            if (source instanceof IEntity<?> entity) cascade(entity);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void cascade(IEntity<?> source) {
        cascade(source, new LinkedHashSet<>(), 0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void cascade(IEntity<?> source, Set<String> visited, int depth) {
        if (depth > 1000) throw new IllegalStateException("级联删除超过最大深度 1000");
        String identity = source.getClass().getName() + ":" + id(source);
        if (!visited.add(identity)) throw new IllegalStateException("级联删除检测到循环: " + identity);
        for (EntityAssociation association : source.associations()) {
            if (!association.cascadeDelete()) continue;
            Object key = read(source, association.localField());
            if (key == null) continue;
            ILocalCrudService targetService = targetService(association);
            Object criteria = targetService.newCriteria();
            ((IWhereConditionCriteria) criteria).equals(association.targetField(), key);
            List<?> targets = targetService.getRepository().findByCriteria(
                    (vip.isass.framework.nocode.criteria.ICriteria) criteria);
            for (Object target : targets) {
                if (target instanceof IEntity<?> entity) cascade(entity, visited, depth + 1);
            }
            nested(() -> targetService.deleteBatch((vip.isass.framework.nocode.criteria.ICriteria) criteria));
        }
        visited.remove(identity);
    }

    @SuppressWarnings("rawtypes")
    private ILocalCrudService targetService(EntityAssociation association) {
        ILocalCrudService service = services.get(association.targetType());
        if (service == null) throw new IllegalStateException(
                "关联目标没有本地 ILocalCrudService: " + association.targetType().getName());
        return service;
    }

    private <T> T nested(Supplier<T> action) {
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
        if (values.stream().anyMatch(java.util.Objects::isNull)) {
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
        } catch (java.beans.IntrospectionException exception) {
            throw new IllegalStateException("无法分析实体属性: " + type.getName(), exception);
        }
    }
}
