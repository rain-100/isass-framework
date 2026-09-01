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

/** Loads explicitly requested association paths in batches, one query per path level. */
public final class AssociationQueryCoordinator {

    private static final int MAX_ASSOCIATION_DEPTH = 16;

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
                || !(criteria instanceof IAssociationCriteria<?> associationCriteria)) {
            return records;
        }
        LinkedHashSet<String> requestedPaths = requestedPaths(associationCriteria);
        if (requestedPaths.isEmpty()) return records;

        Map<String, List<?>> loadedByPath = new LinkedHashMap<>();
        loadedByPath.put("", records);
        Map<String, Map<String, Object>> associationCriteriaByPath =
                associationCriteria.getAssociationCriteria() == null
                        ? Map.of() : associationCriteria.getAssociationCriteria();
        for (String requestedPath : requestedPaths) {
            String parentPath = parentPath(requestedPath);
            String property = propertyName(requestedPath);
            List<?> sources = loadedByPath.getOrDefault(parentPath, List.of());
            if (sources.isEmpty()) {
                loadedByPath.put(requestedPath, List.of());
                continue;
            }
            EntityAssociation association = association(sources.getFirst(), property);
            if (association == null) {
                throw new IllegalArgumentException("未声明的关联属性: " + requestedPath);
            }
            loadedByPath.put(requestedPath, populate(sources, association,
                    associationCriteriaByPath.getOrDefault(requestedPath, Map.of())));
        }
        return records;
    }

    private LinkedHashSet<String> requestedPaths(IAssociationCriteria<?> criteria) {
        LinkedHashSet<String> requested = new LinkedHashSet<>();
        if (criteria.getAssociationQueries() != null) {
            criteria.getAssociationQueries().forEach(path -> addPathAndParents(requested, path));
        }
        if (criteria.getAssociationCriteria() != null) {
            criteria.getAssociationCriteria().keySet().forEach(path -> addPathAndParents(requested, path));
        }
        return requested;
    }

    private void addPathAndParents(Set<String> requested, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("关联查询路径不能为空");
        }
        String[] properties = rawPath.trim().split("\\.", -1);
        if (properties.length > MAX_ASSOCIATION_DEPTH) {
            throw new IllegalArgumentException("关联查询路径超过最大深度 " + MAX_ASSOCIATION_DEPTH + ": " + rawPath);
        }
        StringBuilder path = new StringBuilder();
        for (String property : properties) {
            if (property.isBlank()) {
                throw new IllegalArgumentException("关联查询路径格式错误: " + rawPath);
            }
            if (!path.isEmpty()) path.append('.');
            path.append(property);
            requested.add(path.toString());
        }
    }

    private String parentPath(String path) {
        int separator = path.lastIndexOf('.');
        return separator < 0 ? "" : path.substring(0, separator);
    }

    private String propertyName(String path) {
        int separator = path.lastIndexOf('.');
        return separator < 0 ? path : path.substring(separator + 1);
    }

    private EntityAssociation association(Object source, String property) {
        if (!(source instanceof IEntity<?> entity)) {
            throw new IllegalStateException("关联源对象未实现 IEntity: " + source.getClass().getName());
        }
        return entity.associations().stream()
                .filter(candidate -> candidate.property().equals(property))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<?> populate(List<?> sources, EntityAssociation association, Map<String, Object> filters) {
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
            return List.of();
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
        return targets;
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
