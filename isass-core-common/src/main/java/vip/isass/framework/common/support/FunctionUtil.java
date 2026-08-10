// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Rain
 */
@Slf4j
public class FunctionUtil {

    public static <T, R> R applyIfNotNull(T t, Function<T, R> function) {
        if (t == null) {
            return null;
        }
        return function.apply(t);
    }

    public static <T> void consumeIfNotNull(T t, Consumer<T> consumer) {
        if (t == null) {
            return;
        }
        consumer.accept(t);
    }

    public static <T> void consumeIfNotBlank(T t, Consumer<T> consumer) {
        if (StrUtil.isBlankIfStr(t)) {
            return;
        }
        consumer.accept(t);
    }

    public static <T, R> R applyIfTrue(boolean flag, T t, Function<T, R> function) {
        if (flag) {
            return function.apply(t);
        }
        return null;
    }

    public static <T> void consumeIfTrue(boolean flag, T t, Consumer<T> consumer) {
        if (flag) {
            consumer.accept(t);
        }
    }

    public static <T> T applyIfTrue(boolean flag, Supplier<T> supplier) {
        if (flag) {
            return supplier.get();
        }
        return null;
    }

    public static <T extends CharSequence, R> R applyIfNotBlank(T t, Function<T, R> function) {
        if (StrUtil.isBlank(t)) {
            return null;
        }
        return function.apply(t);
    }

    public static <T> boolean isNotNull(T t, Function<T, ?> function) {
        return !isNull(t, function);
    }

    public static <T> boolean isNull(T t, Function<T, ?> function) {
        try {
            return function.apply(t) == null;
        } catch (Exception e) {
            return true;
        }
    }

    public static <S, V> V getFirstNotNullValueFromCollection(Collection<S> services, Function<S, V> function) {
        for (S service : services) {
            V value = function.apply(service);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

}
