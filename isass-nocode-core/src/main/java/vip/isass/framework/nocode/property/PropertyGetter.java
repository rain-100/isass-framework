package vip.isass.framework.nocode.property;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 可解析属性名的 getter 方法引用。
 *
 * @param <T> 实体类型
 * @param <R> 属性类型
 */
@FunctionalInterface
public interface PropertyGetter<T, R> extends Function<T, R>, Serializable {
}
