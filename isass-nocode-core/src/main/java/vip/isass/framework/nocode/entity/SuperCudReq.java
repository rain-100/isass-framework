// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * A composable create/update/delete change set executed by {@code superCud} in one transaction.
 * Every group is optional and multiple groups may be submitted together.
 */
public record SuperCudReq<E, C>(
        List<E> addEntities,
        List<AddIfAbsentItem<E, C>> addIfAbsentItems,
        List<E> updateEntities,
        List<UpdateByCriteriaItem<E, C>> updateByCriteriaItems,
        List<Serializable> deleteIds,
        List<C> deleteCriteria
) {

    public SuperCudReq {
        addEntities = immutableList(addEntities);
        addIfAbsentItems = immutableList(addIfAbsentItems);
        updateEntities = immutableList(updateEntities);
        updateByCriteriaItems = immutableList(updateByCriteriaItems);
        deleteIds = immutableList(deleteIds);
        deleteCriteria = immutableList(deleteCriteria);
    }

    public static <E, C> SuperCudReq<E, C> empty() {
        return new SuperCudReq<>(null, null, null, null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> add(E entity) {
        return new SuperCudReq<>(List.of(entity), null, null, null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> addAll(Collection<E> entities) {
        return new SuperCudReq<>(copyOf(entities), null, null, null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> addIfAbsent(E entity, C criteria) {
        return new SuperCudReq<>(null, List.of(new AddIfAbsentItem<>(entity, criteria)),
                null, null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> update(E entity) {
        return new SuperCudReq<>(null, null, List.of(entity), null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> updateAll(Collection<E> entities) {
        return new SuperCudReq<>(null, null, copyOf(entities), null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> updateByCriteria(Collection<E> entities, C criteria) {
        return new SuperCudReq<>(null, null, null,
                List.of(new UpdateByCriteriaItem<>(copyOf(entities), criteria)), null, null);
    }

    public static <E, C> SuperCudReq<E, C> delete(Serializable id) {
        return new SuperCudReq<>(null, null, null, null, List.of(id), null);
    }

    public static <E, C> SuperCudReq<E, C> deleteAll(Collection<? extends Serializable> ids) {
        return new SuperCudReq<>(null, null, null, null, copyOf(ids), null);
    }

    public static <E, C> SuperCudReq<E, C> deleteByCriteria(C criteria) {
        return new SuperCudReq<>(null, null, null, null, null, List.of(criteria));
    }

    public boolean isEmpty() {
        return addEntities.isEmpty()
                && addIfAbsentItems.isEmpty()
                && updateEntities.isEmpty()
                && updateByCriteriaItems.isEmpty()
                && deleteIds.isEmpty()
                && deleteCriteria.isEmpty();
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static <T> List<T> copyOf(Collection<? extends T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
