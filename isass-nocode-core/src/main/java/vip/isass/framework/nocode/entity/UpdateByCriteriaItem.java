// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import java.util.List;

/** One criteria-scoped update group in a {@link SuperCudReq}. */
public record UpdateByCriteriaItem<E, C>(List<E> entities, C criteria) {

    public UpdateByCriteriaItem {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }
}
