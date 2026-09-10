// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import vip.isass.framework.common.page.Page;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Canonical internal result used by every standard NoCode query facade. */
public record CrudQueryResult<E, PK extends Serializable>(
        CrudQueryType queryType,
        Page<E> page,
        CursorPage<E, PK> cursorPage,
        Long count,
        Boolean exists,
        List<E> tree
) {

    public CrudQueryResult {
        Objects.requireNonNull(queryType, "queryType");
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> page(Page<E> value) {
        return new CrudQueryResult<>(CrudQueryType.PAGE, value, null, null, null, null);
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> cursorPage(CursorPage<E, PK> value) {
        return new CrudQueryResult<>(CrudQueryType.CURSOR_PAGE, null, value, null, null, null);
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> count(Long value) {
        return new CrudQueryResult<>(CrudQueryType.COUNT, null, null, value, null, null);
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> exists(Boolean value) {
        return new CrudQueryResult<>(CrudQueryType.EXISTS, null, null, null, value, null);
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> tree(List<E> value) {
        return new CrudQueryResult<>(CrudQueryType.TREE, null, null, null, null, List.copyOf(value));
    }

    /** Returns every entity in this result, flattening tree results in pre-order. */
    public List<E> records() {
        if (page != null) return page.getRecords();
        if (cursorPage != null) return cursorPage.records();
        if (tree == null || tree.isEmpty()) return List.of();

        List<E> records = new ArrayList<>();
        ArrayDeque<E> pending = new ArrayDeque<>(tree.reversed());
        Set<E> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (!pending.isEmpty()) {
            E entity = pending.removeLast();
            if (!visited.add(entity)) continue;
            records.add(entity);
            if (entity instanceof IParentIdEntity<?, ?> parentIdEntity
                    && parentIdEntity.getChildren() != null) {
                for (Object child : parentIdEntity.getChildren().reversed()) {
                    @SuppressWarnings("unchecked") E typedChild = (E) child;
                    pending.addLast(typedChild);
                }
            }
        }
        return records;
    }
}
