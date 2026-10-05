// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria.impl.type;

import lombok.ToString;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.entity.IEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * where 查询条件
 */
@ToString
public class WhereConditionCriteria<
    E extends IEntity<E>,
    C extends WhereConditionCriteria<E, C>
    > implements IWhereConditionCriteria<E, C> {

    private List<WhereCondition> whereConditions;

    public List<WhereCondition> getWhereConditions() {
        if (whereConditions == null) {
            whereConditions = new ArrayList<>();
        }
        return whereConditions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setWhereConditions(List<WhereCondition> whereConditions) {
        this.whereConditions = whereConditions;
        return (C) this;
    }

}
