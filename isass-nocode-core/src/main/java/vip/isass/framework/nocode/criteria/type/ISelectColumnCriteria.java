// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.type;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.property.PropertyGetter;
import vip.isass.framework.nocode.property.PropertyNameResolver;

import java.util.Collection;
import java.util.Collections;

/**
 * sql 的 select 字段条件接口
 *
 * @author Rain
 */
public interface ISelectColumnCriteria<E extends IEntity<E>, C extends ISelectColumnCriteria<E, C>>
        extends ICriteria<E, C> {

    String DISTINCT = "DISTINCT ";

    /**
     * get select columns list
     *
     * @return select column list
     */
    Collection<String> getSelectColumns();

    default C setSelectColumn(String selectColumn) {
        getSelectColumns().clear();
        return addSelectColumn(selectColumn);
    }

    default C setSelectColumn(PropertyGetter<E, ?> getter) {
        getSelectColumns().clear();
        return addSelectColumn(getter);
    }

    default C setSelectColumns(Collection<String> selectColumns) {
        getSelectColumns().clear();
        return addSelectColumns(selectColumns);
    }

    default C setSelectColumns(String... selectColumns) {
        getSelectColumns().clear();
        return addSelectColumns(selectColumns);
    }

    @SuppressWarnings("unchecked")
    default C setSelectColumns(PropertyGetter<E, ?>... getters) {
        getSelectColumns().clear();
        if (ArrayUtil.isNotEmpty(getters)) {
            for (PropertyGetter<E, ?> getter : getters) {
                addSelectColumn(PropertyNameResolver.resolve(getter));
            }
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C addSelectColumn(String selectColumn) {
        if (StrUtil.isNotBlank(selectColumn)) {
            if (!getSelectColumns().contains(selectColumn)) {
                addSelectColumns(Collections.singleton(selectColumn));
            }
        }
        return (C) this;
    }

    default C addSelectColumn(PropertyGetter<E, ?> getter) {
        return addSelectColumn(PropertyNameResolver.resolve(getter));
    }

    @SuppressWarnings("unchecked")
    default C addSelectColumns(Collection<String> selectColumns) {
        if (CollUtil.isNotEmpty(selectColumns)) {
            Collection<String> targetColumns = getSelectColumns();
            for (String selectColumn : selectColumns) {
                if (StrUtil.containsAnyIgnoreCase(selectColumn, "select", "insert", "update")) {
                    throw new IllegalArgumentException("selectColumns can not contains insert, update, select");
                }
                targetColumns.add(selectColumn);
            }
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C addSelectColumns(String... selectColumns) {
        if (ArrayUtil.isNotEmpty(selectColumns)) {
            addSelectColumns(CollUtil.toList(selectColumns));
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C addSelectColumns(PropertyGetter<E, ?>... getters) {
        if (ArrayUtil.isNotEmpty(getters)) {
            for (PropertyGetter<E, ?> getter : getters) {
                addSelectColumn(getter);
            }
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C unSelectColumn(String selectColumn) {
        if (StrUtil.isNotBlank(selectColumn)) {
            getSelectColumns().remove(selectColumn);
        }
        return (C) this;
    }

    default C unSelectColumn(PropertyGetter<E, ?> getter) {
        return unSelectColumn(PropertyNameResolver.resolve(getter));
    }

    @SuppressWarnings("unchecked")
    default C unSelectColumns(Collection<String> selectColumns) {
        if (CollUtil.isNotEmpty(selectColumns)) {
            getSelectColumns().removeAll(selectColumns);
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C unSelectColumns(String... selectColumns) {
        if (ArrayUtil.isNotEmpty(selectColumns)) {
            getSelectColumns().removeAll(CollUtil.toList(selectColumns));
        }
        return (C) this;
    }

}
