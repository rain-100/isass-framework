// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.impl.type;

import lombok.ToString;
import vip.isass.framework.nocode.criteria.WhereCondition;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.IEntity;

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

    public C setWhereConditions(List<WhereCondition> whereConditions) {
        return IWhereConditionCriteria.super.setWhereConditions(whereConditions);
    }

}
