// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria.type;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.property.PropertyGetter;
import vip.isass.framework.common.property.PropertyNameResolver;

import java.util.Collection;
import java.util.Collections;

/**
 * 定义查询结果需要返回的 Java 属性。
 *
 * @author Rain
 */
public interface IReturnFieldCriteria<E extends IEntity<E>, C extends IReturnFieldCriteria<E, C>>
        extends ICriteria<E, C> {

    String DISTINCT = "DISTINCT ";

    /**
     * 获取要返回的 Java 属性名。
     *
     * @return Java 属性名集合
     */
    Collection<String> getReturnFields();

    default C setReturnField(String returnField) {
        getReturnFields().clear();
        return addReturnField(returnField);
    }

    default C setReturnField(PropertyGetter<E, ?> getter) {
        getReturnFields().clear();
        return addReturnField(getter);
    }

    /** 保存返回字段集合引用；继续增删字段时，调用方应传入可变集合。 */
    C setReturnFields(Collection<String> returnFields);

    default C setReturnFields(String... returnFields) {
        getReturnFields().clear();
        return addReturnFields(returnFields);
    }

    default C setReturnFields(PropertyGetter<E, ?> first) {
        getReturnFields().clear();
        return addReturnField(first);
    }

    @SuppressWarnings("unchecked")
    default C setReturnFields(PropertyGetter<E, ?> first, PropertyGetter<E, ?>... getters) {
        getReturnFields().clear();
        addReturnField(first);
        for (PropertyGetter<E, ?> getter : getters) {
            addReturnField(PropertyNameResolver.resolve(getter));
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C addReturnField(String returnField) {
        if (StrUtil.isNotBlank(returnField)) {
            if (!getReturnFields().contains(returnField)) {
                addReturnFields(Collections.singleton(returnField));
            }
        }
        return (C) this;
    }

    default C addReturnField(PropertyGetter<E, ?> getter) {
        return addReturnField(PropertyNameResolver.resolve(getter));
    }

    @SuppressWarnings("unchecked")
    default C addReturnFields(Collection<String> returnFields) {
        if (CollUtil.isNotEmpty(returnFields)) {
            Collection<String> targetFields = getReturnFields();
            for (String returnField : returnFields) {
                if (StrUtil.containsAnyIgnoreCase(returnField, "select", "insert", "update")) {
                    throw new IllegalArgumentException("returnFields 只能包含 Java 属性名，不能包含 SQL 语句");
                }
                targetFields.add(returnField);
            }
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C addReturnFields(String... returnFields) {
        if (ArrayUtil.isNotEmpty(returnFields)) {
            addReturnFields(CollUtil.toList(returnFields));
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C addReturnFields(PropertyGetter<E, ?>... getters) {
        if (ArrayUtil.isNotEmpty(getters)) {
            for (PropertyGetter<E, ?> getter : getters) {
                addReturnField(getter);
            }
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C removeReturnField(String returnField) {
        if (StrUtil.isNotBlank(returnField)) {
            getReturnFields().remove(returnField);
        }
        return (C) this;
    }

    default C removeReturnField(PropertyGetter<E, ?> getter) {
        return removeReturnField(PropertyNameResolver.resolve(getter));
    }

    @SuppressWarnings("unchecked")
    default C removeReturnFields(Collection<String> returnFields) {
        if (CollUtil.isNotEmpty(returnFields)) {
            getReturnFields().removeAll(returnFields);
        }
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    default C removeReturnFields(String... returnFields) {
        if (ArrayUtil.isNotEmpty(returnFields)) {
            getReturnFields().removeAll(CollUtil.toList(returnFields));
        }
        return (C) this;
    }

}
