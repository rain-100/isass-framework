// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.io.Serializable;
import java.util.Objects;

/** Canonical internal request used by every standard NoCode query facade. */
public record CrudQueryReq<C, PK extends Serializable>(
        CrudQueryType queryType,
        C criteria,
        PK cursorId,
        Long pageSize
) {

    public CrudQueryReq {
        Objects.requireNonNull(queryType, "queryType");
    }

    public static <C, PK extends Serializable> CrudQueryReq<C, PK> page(C criteria) {
        return new CrudQueryReq<>(CrudQueryType.PAGE, criteria, null, null);
    }

    public static <C, PK extends Serializable> CrudQueryReq<C, PK> cursorPage(
            C criteria, PK cursorId, Long pageSize) {
        return new CrudQueryReq<>(CrudQueryType.CURSOR_PAGE, criteria, cursorId, pageSize);
    }

    public static <C, PK extends Serializable> CrudQueryReq<C, PK> count(C criteria) {
        return new CrudQueryReq<>(CrudQueryType.COUNT, criteria, null, null);
    }

    public static <C, PK extends Serializable> CrudQueryReq<C, PK> exists(C criteria) {
        return new CrudQueryReq<>(CrudQueryType.EXISTS, criteria, null, null);
    }
}
