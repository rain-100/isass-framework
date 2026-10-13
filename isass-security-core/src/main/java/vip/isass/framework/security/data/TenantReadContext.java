// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security.data;

import java.util.Objects;

/**
 * 根查询及其关联 SQL 共用的固定租户边界；角色策略和元数据读取不会替换此上下文。
 */
public final class TenantReadContext {
    private static final ThreadLocal<TenantReadPolicy> CURRENT = new ThreadLocal<>();

    private TenantReadContext() {
    }

    public static TenantReadPolicy current() {
        return CURRENT.get();
    }

    public static Scope open(TenantReadPolicy policy) {
        TenantReadPolicy previous = CURRENT.get();
        CURRENT.set(Objects.requireNonNull(policy, "固定租户读取策略不能为空"));
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
