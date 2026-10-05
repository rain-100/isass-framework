// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria.impl.type;

import lombok.ToString;
import vip.isass.framework.common.criteria.type.IReturnFieldCriteria;
import vip.isass.framework.common.entity.IEntity;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 查询结果返回字段条件。
 */
@ToString
public class ReturnFieldCriteria<
    E extends IEntity<E>,
    C extends ReturnFieldCriteria<E, C>
    > implements IReturnFieldCriteria<E, C> {

    private Collection<String> returnFields;

    @Override
    public Collection<String> getReturnFields() {
        if (returnFields == null) {
            returnFields = new ArrayList<>(16);
        }
        return returnFields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setReturnFields(Collection<String> returnFields) {
        this.returnFields = returnFields;
        return (C) this;
    }

}
