// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Rain
 */
@Slf4j
public class NullableUtil {

    public static <T, R> R supplier(T t, Supplier<R> supplier) {
        if (t == null) {
            return null;
        }
        return supplier.get();
    }

    public static <T, R> R supplierOrDefault(T t, Supplier<R> supplier, R defaultValue) {
        R r = supplier(t, supplier);
        return r == null ? defaultValue : r;
    }

    public static <T, R> R functionOrDefault(T t, Function<T, R> function, R defaultValue) {
        R r = function(t, function);
        return r == null ? defaultValue : r;
    }

    public static <T, R> R function(T t, Function<T, R> function) {
        if (t == null) {
            return null;
        }
        return function.apply(t);
    }

}
