// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security.data;

import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.WhereCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 服务端读取约束按 Criteria 对象身份绑定，不属于客户端参数或输出字段授权。
 * 嵌套作用域只叠加约束，不替换固定租户边界或客户端条件校验。
 */
public final class TrustedReadContext {
    private static final ThreadLocal<Restriction> CURRENT = new ThreadLocal<>();

    private TrustedReadContext() {
    }

    public static List<WhereCondition> conditions(ICriteria<?, ?> criteria) {
        List<WhereCondition> result = new ArrayList<>();
        for (Restriction restriction = CURRENT.get(); restriction != null; restriction = restriction.previous()) {
            if (restriction.criteria() == criteria) {
                result.addAll(restriction.conditions());
            }
        }
        return List.copyOf(result);
    }

    public static Scope open(ICriteria<?, ?> criteria, List<WhereCondition> conditions) {
        Restriction previous = CURRENT.get();
        Restriction current = new Restriction(Objects.requireNonNull(criteria, "查询条件不能为空"),
                List.copyOf(conditions), previous);
        CURRENT.set(current);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    private record Restriction(ICriteria<?, ?> criteria, List<WhereCondition> conditions, Restriction previous) {
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
