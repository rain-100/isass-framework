// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.security.data;

import vip.isass.framework.common.criteria.CriteriaMetadata;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.IRelatedQueryCriteria;
import vip.isass.framework.common.criteria.JoinResultPropertyResolver;
import vip.isass.framework.common.criteria.type.IReturnFieldCriteria;
import vip.isass.framework.common.entity.IEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 按查询路径保存调用方投影，隐藏仅供授权判断补查的属性，不提前修改关联键或条件值。
 */
public final class DataReadProjection {
    private final Set<String> internalProperties;
    private final Map<String, DataReadProjection> children;

    private DataReadProjection(Set<String> internalProperties, Map<String, DataReadProjection> children) {
        this.internalProperties = Set.copyOf(internalProperties);
        this.children = Map.copyOf(children);
    }

    public static DataReadProjection capture(Class<?> entityType, Object criteria, DataReadPolicy policy) {
        return capture(entityType, criteria, policy, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static DataReadProjection capture(Class<?> entityType, Object criteria, DataReadPolicy policy,
                                               Set<Object> active) {
        if (criteria != null && !active.add(criteria)) {
            throw new IllegalArgumentException("查询投影包含循环 Criteria");
        }
        try {
            Set<String> internal = new HashSet<>();
            if (policy != null && criteria instanceof IReturnFieldCriteria<?, ?> fields
                    && fields.getReturnFields() != null && !fields.getReturnFields().isEmpty()) {
                internal.addAll(policy.requiredProperties(entityType));
                internal.removeAll(fields.getReturnFields());
            }
            Map<String, DataReadProjection> children = new LinkedHashMap<>();
            if (criteria instanceof IRelatedQueryCriteria<?, ?> related) {
                for (var join : related.getJoinConditions()) {
                    var association = JoinResultPropertyResolver.resolve(entityType, join);
                    children.put(association.property(), capture(association.targetType(),
                            join.getTargetCriteria(), policy, active));
                }
                for (var load : related.getLoadRelated()) {
                    addRelated(children, entityType, load.getProperty(), load.getCriteria(), policy, active);
                }
            }
            return new DataReadProjection(internal, children);
        } finally {
            if (criteria != null) {
                active.remove(criteria);
            }
        }
    }

    private static void addRelated(Map<String, DataReadProjection> children, Class<?> sourceType, String path,
                                   ICriteria<?, ?> target, DataReadPolicy policy, Set<Object> active) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("关联路径不能为空");
        }
        int separator = path.indexOf('.');
        String property = separator < 0 ? path : path.substring(0, separator);
        var association = CriteriaMetadata.associations(sourceType).stream()
                .filter(value -> value.property().equals(property))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("未知关联属性: " + property));
        DataReadProjection existing = children.get(property);
        if (separator < 0) {
            DataReadProjection projection = capture(association.targetType(), target, policy, active);
            Map<String, DataReadProjection> nested = new LinkedHashMap<>();
            if (existing != null) {
                nested.putAll(existing.children);
            }
            nested.putAll(projection.children);
            children.put(property, new DataReadProjection(projection.internalProperties, nested));
        } else {
            Map<String, DataReadProjection> nested = new LinkedHashMap<>(existing == null ? Map.of() : existing.children);
            addRelated(nested, association.targetType(), path.substring(separator + 1), target, policy, active);
            children.put(property, new DataReadProjection(existing == null ? Set.of() : existing.internalProperties, nested));
        }
    }

    public void hideInternalProperties(Object result) {
        hide(result, new IdentityHashMap<>());
    }

    private void hide(Object value, Map<DataReadProjection, Set<Object>> paths) {
        Set<Object> visited = paths.computeIfAbsent(this,
                ignored -> Collections.newSetFromMap(new IdentityHashMap<>()));
        if (value == null || !visited.add(value)) {
            return;
        }
        if (value instanceof Collection<?> values) {
            values.forEach(child -> hide(child, paths));
        } else if (value instanceof IEntity<?> entity) {
            internalProperties.forEach(property -> HiddenDataFields.mark(entity, property));
            children.forEach((property, projection) -> projection.hide(property(entity, property), paths));
            entity.associations().stream()
                    .filter(association -> association.targetType() == entity.getClass()
                            && !children.containsKey(association.property()))
                    .forEach(association -> hide(property(entity, association.property()), paths));
        }
    }

    private static Object property(IEntity<?> entity, String property) {
        var field = CriteriaMetadata.field(entity.getClass(), property);
        if (field == null) {
            throw new IllegalArgumentException("关联属性不存在: " + property);
        }
        try {
            return field.get(entity);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("无法读取关联属性: " + property, error);
        }
    }
}
