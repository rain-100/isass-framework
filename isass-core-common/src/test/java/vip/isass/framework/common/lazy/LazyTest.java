// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.lazy;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Lazy 测试类
 *
 * @author Rain
 */
public class LazyTest implements Supplier<Integer> {

    private transient Supplier<Integer> supplier;

    private volatile Integer value;

    public LazyTest(Supplier<Integer> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public Integer get() {
        if (value == null) {
            synchronized (this) {
                if (value == null) {
                    value = Objects.requireNonNull(supplier.get());
                    supplier = null;
                }
            }
        }
        return value;
    }

    public LazyTest map(Function<Integer, Integer> mapper) {
        return new LazyTest(() -> mapper.apply(this.get()));
    }

    public LazyTest flatMap(Function<Integer, LazyTest> mapper) {
        return new LazyTest(() -> mapper.apply(this.get()).get());
    }

    public LazyTest filter(Predicate<Integer> predicate) {
        return new LazyTest(() -> Optional.of(get()).filter(predicate).get());
    }

    public static <T> LazyTest of(Supplier<T> supplier) {
        return new LazyTest(() -> (Integer) supplier.get());
    }

    public static void main(String[] args) {
        LazyTest.of(() -> compute(42))
                .map(s -> compute(13))
                .flatMap(s -> lazyCompute(15))
                .filter(v -> v > 0);
        //.get();
    }

    private static int compute(int val) {
        int result = ThreadLocalRandom.current().nextInt() % val;
        System.out.println("Computed: " + result);
        return result;
    }

    private static LazyTest lazyCompute(int val) {
        return LazyTest.of(() -> {
            int result = ThreadLocalRandom.current().nextInt() % val;
            System.out.println("Computed: " + result);
            return result;
        });
    }

}