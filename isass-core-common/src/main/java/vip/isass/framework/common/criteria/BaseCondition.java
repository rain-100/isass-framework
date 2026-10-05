// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.property.PropertyGetter;
import vip.isass.framework.common.property.PropertyNameResolver;

import java.util.List;

/**
 * ORM 无关的比较、分组及目标查询；构造时只记录，消费时校验。
 */
@Getter
public abstract class BaseCondition<B extends BaseCondition<B>> {

    private String sourceProperty;

    private Condition condition;

    private String targetProperty;

    private Object value;

    private List<WhereCondition> children;

    private ICriteria<?, ?> targetCriteria;

    @SuppressWarnings("unchecked")
    protected final B self() {
        return (B) this;
    }

    public B setSourceProperty(String property) {
        sourceProperty = property;
        return self();
    }

    public <E, V> B setSourceProperty(PropertyGetter<E, V> getter) {
        return setSourceProperty(PropertyNameResolver.resolve(getter));
    }

    public B setTargetProperty(String property) {
        targetProperty = property;
        return self();
    }

    public <E, V> B setTargetProperty(PropertyGetter<E, V> getter) {
        return setTargetProperty(PropertyNameResolver.resolve(getter));
    }

    public B setCondition(Condition value) {
        condition = value;
        return self();
    }

    public B setValue(Object value) {
        this.value = value;
        return self();
    }

    public B setChildren(List<WhereCondition> value) {
        children = value;
        return self();
    }

    public B setTargetCriteria(ICriteria<?, ?> value) {
        targetCriteria = value;
        return self();
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("未知条件字段: " + property);
    }
}
