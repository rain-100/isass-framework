// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria;

import vip.isass.framework.common.entity.IEntity;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * ORM 无关的实体查询条件。
 *
 * @author Rain
 */
@JsonDeserialize(using = CriteriaDeserializer.class)
public interface ICriteria<E extends IEntity<E>, C extends ICriteria<E, C>> {

    default String getEntityType() {
        return CriteriaEntityTypes.entityType(this);
    }

    @SuppressWarnings("unchecked")
    default C copy() {
        return (C) CriteriaCopies.copy(this);
    }

}
