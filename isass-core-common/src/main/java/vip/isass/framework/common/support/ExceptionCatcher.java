// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Rain
 */
@Slf4j
public class ExceptionCatcher {

    /**
     * 泛型参数应该排在前面，调用方先传入泛型对象，后编写 consumer 时，ide 才能提示 consumer 的泛型类型
     *
     * @param consumer 消费逻辑
     * @param t        消费对象
     * @param <T>      消费对象泛型
     */
    @Deprecated
    public static <T> void consume(Consumer<T> consumer, T t) {
        try {
            consumer.accept(t);
        } catch (Throwable e) {
            log.trace("ignore exception: ", e);
        }
    }

    public static <T> void consume(T t, Consumer<T> consumer) {
        try {
            consumer.accept(t);
        } catch (Throwable e) {
            log.trace("ignore exception: ", e);
        }
    }

    public static <T, U> void biConsume(BiConsumer<T, U> consumer, T t, U u) {
        try {
            consumer.accept(t, u);
        } catch (Throwable e) {
            log.trace("ignore exception: ", e);
        }
    }

    public static <T, R> R functionOrDefault(Function<T, R> function, T t, R defaultValue) {
        try {
            return function.apply(t);
        } catch (Throwable e) {
            log.trace("ignore exception: ", e);
        }
        return defaultValue;
    }

    public static <T, R> R function(Function<T, R> function, T t) {
        return functionOrDefault(function, t, null);
    }

    public static <T> T supplier(Supplier<T> supplier) {
        return supplierOrDefault(supplier, null);
    }

    public static <T> T supplierOrDefault(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            log.trace("ignore exception: ", e);
        }
        return defaultValue;
    }

    public static <T> boolean isNull(Supplier<T> supplier) {
        try {
            return supplier.get() == null;
        } catch (Throwable e) {
            return true;
        }
    }

    public static <T> boolean isNotNull(Supplier<T> supplier) {
        return !isNull(supplier);
    }

    public static boolean isNotBlank(Supplier<String> supplier) {
        return !isBlank(supplier);
    }

    public static boolean isBlank(Supplier<String> supplier) {
        try {
            return StrUtil.isBlank(supplier.get());
        } catch (Throwable e) {
            return true;
        }
    }

}
