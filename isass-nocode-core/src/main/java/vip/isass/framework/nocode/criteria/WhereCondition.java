// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vip.isass.framework.nocode.criteria.impl.type.Condition;

/**
 * @author Rain
 */
@Getter
@Setter
@NoArgsConstructor
public class WhereCondition {

    /**
     * java 对象属性名
     */
    private String propertyName;

    /**
     * 条件，例如：大于，小于，等于
     */
    private Condition condition;

    /**
     * 比较的值
     */
    private Object value;

    public WhereCondition(String propertyName, Condition condition, Object value) {
        this.propertyName = propertyName;
        this.condition = condition;
        this.value = value;
    }

}
