// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.security.data;

import java.util.function.Supplier;

/**
 * 一次业务查询及所有关联 SQL 共用的读取策略；嵌套作用域退出时恢复原策略。
 */
public final class DataReadContext {
    private static final ThreadLocal<DataReadPolicy> CURRENT = new ThreadLocal<>();

    private DataReadContext() {
    }

    public static DataReadPolicy current() {
        return CURRENT.get();
    }

    public static Scope open(DataReadPolicy policy) {
        DataReadPolicy previous = CURRENT.get();
        CURRENT.set(policy);
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    /**
     * 仅供授权元数据解析，不能包裹业务查询以跳过权限。
     */
    public static <T> T metadata(Supplier<T> loader) {
        try (Scope ignored = open(null)) {
            return loader.get();
        }
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
