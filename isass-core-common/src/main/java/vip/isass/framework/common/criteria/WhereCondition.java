// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria;

import lombok.NoArgsConstructor;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.property.PropertyGetter;

import java.util.Arrays;

/**
 * @author Rain
 */
@NoArgsConstructor
public class WhereCondition extends BaseCondition<WhereCondition> {

    public WhereCondition(String sourceProperty, Condition condition, Object value) {
        setSourceProperty(sourceProperty);
        setCondition(condition);
        setValue(value);
    }

    /** 常量比较；字段字符串仍是 Java 属性名，不是数据库列名。 */
    public static WhereCondition eq(String property, Object value) {
        return new WhereCondition(property, Condition.Compare.EQUAL, value);
    }

    public static <E> WhereCondition eq(PropertyGetter<E, ?> property, Object value) {
        return new WhereCondition().setSourceProperty(property).setCondition(Condition.Compare.EQUAL)
                .setValue(value);
    }

    /** 双字段比较，可用于 JOIN ON 或相关子查询；两侧所属表由消费位置决定。 */
    public static <S, T, V> WhereCondition eq(PropertyGetter<S, V> source, PropertyGetter<T, V> target) {
        return new WhereCondition().setSourceProperty(source).setCondition(Condition.Compare.EQUAL).setTargetProperty(target);
    }

    public static WhereCondition and(WhereCondition... children) { return group(Condition.Logical.AND, children); }

    public static WhereCondition or(WhereCondition... children) { return group(Condition.Logical.OR, children); }

    public static WhereCondition not(WhereCondition child) { return group(Condition.Logical.NOT, child); }

    private static WhereCondition group(Condition connector, WhereCondition... children) {
        // 不提前校验布尔语法；保留 null/空组交给原消费入口报告。
        return new WhereCondition().setCondition(connector)
                .setChildren(children == null ? null : Arrays.asList(children));
    }
}
