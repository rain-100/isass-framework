// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.nocode.criteria.IAssociationCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.EntityAssociation;
import vip.isass.framework.nocode.entity.IEntity;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads explicitly requested direct associations in batches. */
public final class AssociationQueryCoordinator {

    private final Map<Class<?>, ILocalCrudService<?, ?, ?>> services;

    public AssociationQueryCoordinator(Collection<ILocalCrudService<?, ?, ?>> localServices) {
        Map<Class<?>, ILocalCrudService<?, ?, ?>> indexed = new LinkedHashMap<>();
        for (ILocalCrudService<?, ?, ?> service : localServices) {
            Class<?> entityType = CrudServiceTypeResolver.resolveEntityClass(service.getClass());
            if (indexed.putIfAbsent(entityType, service) != null) {
                throw new IllegalStateException("实体存在多个 ILocalCrudService: " + entityType.getName());
            }
        }
        services = Map.copyOf(indexed);
    }

    public <E extends IEntity<E>> List<E> populate(List<E> records, Object criteria) {
        if (records == null || records.isEmpty()
                || !(criteria instanceof IAssociationCriteria<?> associationCriteria)
                || associationCriteria.getAssociationQueries() == null
                || associationCriteria.getAssociationQueries().isEmpty()) {
            return records;
        }
        Map<String, EntityAssociation> definitions = new LinkedHashMap<>();
        for (EntityAssociation association : records.getFirst().associations()) {
            definitions.put(association.property(), association);
        }
        for (String requested : new LinkedHashSet<>(associationCriteria.getAssociationQueries())) {
            EntityAssociation association = definitions.get(requested);
            if (association == null) {
                throw new IllegalArgumentException("未声明的关联属性: " + requested);
            }
            populate(records, association, associationCriteria.getAssociationCriteria() == null
                    ? Map.of() : associationCriteria.getAssociationCriteria().getOrDefault(requested, Map.of()));
        }
        return records;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void populate(List<?> sources, EntityAssociation association, Map<String, Object> filters) {
        ILocalCrudService targetService = services.get(association.targetType());
        if (targetService == null) {
            throw new IllegalStateException("关联目标没有本地 ILocalCrudService: "
                    + association.targetType().getName());
        }
        Set<Object> keys = new LinkedHashSet<>();
        sources.forEach(source -> {
            Object value = read(source, association.localField());
            if (value != null) keys.add(value);
        });
        if (keys.isEmpty()) {
            sources.forEach(source -> write(source, association.property(),
                    association.kind() == EntityAssociation.Kind.MANY ? List.of() : null));
            return;
        }
        Object criteria = targetService.newCriteria();
        if (!(criteria instanceof IWhereConditionCriteria where)) {
            throw new IllegalStateException("关联目标 Criteria 不支持条件查询: " + criteria.getClass().getName());
        }
        filters.forEach((name, value) -> {
            if (value instanceof Collection<?> collection) where.in(name, collection);
            else if (value != null) where.equals(name, value);
        });
        where.in(association.targetField(), keys);
        List<?> targets = targetService.getRepository().findByCriteria((vip.isass.framework.nocode.criteria.ICriteria) criteria);
        Map<Object, List<Object>> grouped = new LinkedHashMap<>();
        for (Object target : targets) {
            grouped.computeIfAbsent(read(target, association.targetField()), ignored -> new ArrayList<>()).add(target);
        }
        for (Object source : sources) {
            List<Object> matches = grouped.getOrDefault(read(source, association.localField()), List.of());
            if (association.kind() == EntityAssociation.Kind.ONE && matches.size() > 1) {
                throw new IllegalStateException("单体关联返回多条记录: " + association.property());
            }
            write(source, association.property(), association.kind() == EntityAssociation.Kind.MANY
                    ? List.copyOf(matches) : matches.isEmpty() ? null : matches.getFirst());
        }
    }

    private Object read(Object bean, String property) {
        try {
            PropertyDescriptor descriptor = descriptor(bean.getClass(), property);
            Method method = descriptor.getReadMethod();
            if (method == null) throw new IllegalArgumentException("属性不可读: " + property);
            if (!method.canAccess(bean)) method.setAccessible(true);
            return method.invoke(bean);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取关联属性失败: " + property, exception);
        }
    }

    private void write(Object bean, String property, Object value) {
        try {
            PropertyDescriptor descriptor = descriptor(bean.getClass(), property);
            Method method = descriptor.getWriteMethod();
            if (method == null) throw new IllegalArgumentException("属性不可写: " + property);
            if (!method.canAccess(bean)) method.setAccessible(true);
            method.invoke(bean, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("写入关联属性失败: " + property, exception);
        }
    }

    private PropertyDescriptor descriptor(Class<?> type, String property) {
        try {
            return List.of(Introspector.getBeanInfo(type).getPropertyDescriptors()).stream()
                    .filter(candidate -> candidate.getName().equals(property)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "实体不存在关联属性: " + type.getName() + "." + property));
        } catch (java.beans.IntrospectionException exception) {
            throw new IllegalStateException("无法分析实体属性: " + type.getName(), exception);
        }
    }
}
