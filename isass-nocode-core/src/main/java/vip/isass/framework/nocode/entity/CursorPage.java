// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.io.Serializable;
import java.util.List;

public record CursorPage<E, PK extends Serializable>(
        List<E> records,
        PK nextCursorId,
        boolean hasMore
) {
    public CursorPage {
        records = List.copyOf(records);
    }
}
