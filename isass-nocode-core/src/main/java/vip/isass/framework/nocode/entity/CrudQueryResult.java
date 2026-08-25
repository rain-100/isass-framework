// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import vip.isass.framework.common.page.Page;

import java.io.Serializable;
import java.util.Objects;

/** Canonical internal result used by every standard NoCode query facade. */
public record CrudQueryResult<E, PK extends Serializable>(
        CrudQueryType queryType,
        Page<E> page,
        CursorPage<E, PK> cursorPage,
        Long count,
        Boolean exists
) {

    public CrudQueryResult {
        Objects.requireNonNull(queryType, "queryType");
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> page(Page<E> value) {
        return new CrudQueryResult<>(CrudQueryType.PAGE, value, null, null, null);
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> cursorPage(CursorPage<E, PK> value) {
        return new CrudQueryResult<>(CrudQueryType.CURSOR_PAGE, null, value, null, null);
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> count(Long value) {
        return new CrudQueryResult<>(CrudQueryType.COUNT, null, null, value, null);
    }

    public static <E, PK extends Serializable> CrudQueryResult<E, PK> exists(Boolean value) {
        return new CrudQueryResult<>(CrudQueryType.EXISTS, null, null, null, value);
    }
}
