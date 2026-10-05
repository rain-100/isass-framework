// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import vip.isass.framework.common.entity.IEntity;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 每个实体复用一个不可变空条件；不保存请求状态，不提供条件修改入口。 */
public final class EmptyCriteria<E extends IEntity<E>> implements ICriteria<E, EmptyCriteria<E>> {
    private static final ConcurrentMap<Class<?>, EmptyCriteria<?>> CACHE = new ConcurrentHashMap<>();
    private final Class<E> entityClass;

    private EmptyCriteria(Class<E> entityClass) { this.entityClass = entityClass; }

    @SuppressWarnings("unchecked")
    public static <E extends IEntity<E>> EmptyCriteria<E> of(Class<E> entityClass) {
        Objects.requireNonNull(entityClass, "entityClass");
        if (!IEntity.class.isAssignableFrom(entityClass)) {
            throw new IllegalArgumentException("不是 NoCode 实体: " + entityClass.getName());
        }
        return (EmptyCriteria<E>) CACHE.computeIfAbsent(entityClass, ignored -> new EmptyCriteria<>(entityClass));
    }

    Class<E> entityClass() { return entityClass; }

    @Override
    public EmptyCriteria<E> copy() { return this; }

    @Override
    public String getEntityType() { return CriteriaEntityTypes.entityType(entityClass); }
}
