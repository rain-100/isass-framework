// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security.data;

import vip.isass.framework.common.criteria.WhereCondition;

import java.util.List;

/**
 * 开发者声明的固定租户读取边界，不属于角色数据授权或字段策略。
 */
@FunctionalInterface
public interface TenantReadPolicy {
    List<WhereCondition> conditions(Class<?> entityType);
}
