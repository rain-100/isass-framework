// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 转换器接口
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 * @author Rain
 */
public interface Converter<S, T> extends cn.hutool.core.convert.Converter<T> {

    Logger log = LoggerFactory.getLogger(Converter.class);

    /**
     * source 源对象是否支持被本转换器转换
     *
     * @param source the object if support or not
     * @return support if true
     */
    boolean supportSourceType(Object source);

    boolean supportTargetClass(Class clazz);

    T convert(S source);

    @SuppressWarnings("unchecked")
    default T convert(Object value, T defaultValue) {
        return defaultIfException((S) value, defaultValue);
    }

    /**
     * 将 S 转换为 T ,若转换过程抛出异常，则返回默认值
     *
     * @param source       source object
     * @param defaultValue the default value to be return when convert source object throw an exception
     * @return converted object
     */
    default T defaultIfException(S source, T defaultValue) {
        try {
            return convert(source);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 将 S 转换为 T。
     * 出现以下任一情况，则返回默认值
     * 1: 转换过程抛出异常
     * 2: 入参 source 为空
     * 3: 转换逻辑结果为空
     *
     * @param source       source
     * @param defaultValue default value
     * @return return converted object
     */
    default T defaultIfNull(S source, T defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        T convert = convert(source);
        return convert == null ? defaultValue : convert;
    }

}
