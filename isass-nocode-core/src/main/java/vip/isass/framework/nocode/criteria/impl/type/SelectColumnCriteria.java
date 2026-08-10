// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.impl.type;

import lombok.ToString;
import vip.isass.framework.nocode.criteria.type.ISelectColumnCriteria;
import vip.isass.framework.nocode.entity.IEntity;

import java.util.ArrayList;
import java.util.Collection;

/**
 * select 字段查询条件
 */
@ToString
public class SelectColumnCriteria<
    E extends IEntity<E>,
    C extends SelectColumnCriteria<E, C>
    > implements ISelectColumnCriteria<E, C> {

    private Collection<String> selectColumns;

    @Override
    public Collection<String> getSelectColumns() {
        if (selectColumns == null) {
            selectColumns = new ArrayList<>(16);
        }
        return selectColumns;
    }

    public C setSelectColumns(Collection<String> selectColumns) {
        return ISelectColumnCriteria.super.setSelectColumns(selectColumns);
    }

}
