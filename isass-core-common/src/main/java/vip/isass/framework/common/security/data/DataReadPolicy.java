// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.security.data;

import vip.isass.framework.common.criteria.WhereCondition;
import java.util.List;
import java.util.Set;

/**
 * ORM 无关的读取授权 SPI，每个 SQL 表实例都必须消费范围条件。
 */
public interface DataReadPolicy {
    List<WhereCondition> conditions(Class<?> entityType);

    default Set<String> requiredProperties(Class<?> entityType) {
        return Set.of();
    }

    /**
     * 查询输入与输出分离：隐藏/脱敏字段不应通过筛选、排序或标量子查询暴露原值。
     */
    default void requireQueryable(Class<?> entityType, String property) {
    }
}
