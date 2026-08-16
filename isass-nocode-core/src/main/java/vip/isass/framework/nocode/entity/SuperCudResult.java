// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.io.Serializable;
import java.util.List;

/** Per-group result of a {@link SuperCudReq}. */
public record SuperCudResult<E>(
        List<E> addEntities,
        List<CreateIfAbsentResult<E>> addIfAbsentResults,
        List<E> updateEntities,
        List<Integer> updateByCriteriaCounts,
        List<Serializable> deleteIds,
        List<Integer> deleteByCriteriaCounts
) {

    public SuperCudResult {
        addEntities = immutableList(addEntities);
        addIfAbsentResults = immutableList(addIfAbsentResults);
        updateEntities = immutableList(updateEntities);
        updateByCriteriaCounts = immutableList(updateByCriteriaCounts);
        deleteIds = immutableList(deleteIds);
        deleteByCriteriaCounts = immutableList(deleteByCriteriaCounts);
    }

    public static <E> SuperCudResult<E> empty() {
        return new SuperCudResult<>(null, null, null, null, null, null);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
