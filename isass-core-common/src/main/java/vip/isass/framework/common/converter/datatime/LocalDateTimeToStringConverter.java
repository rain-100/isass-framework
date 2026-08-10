// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import vip.isass.framework.common.support.Converter;

import java.time.LocalDateTime;

/**
 * LocalDateTime 类型转换成 yyyy-MM-dd HH:mm:ss
 *
 * @author Rain
 */
public class LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof LocalDateTime;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    public String convert(LocalDateTime source) {
        return convert0(source);
    }

    public static String convert0(LocalDateTime source) {
        return DateUtil.format(source, DatePattern.NORM_DATETIME_PATTERN);
    }

}
