// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 把 LocalTime 类型的日期时间，转换成 HH:mm:ss
 *
 * @author Rain
 */
public class LocalTimeToStringConverter implements Converter<LocalTime, String> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof LocalTime;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    public String convert(LocalTime source) {
        return convert0(source);
    }

    public static String convert0(LocalTime source) {
        LocalDateTime localDateTime = LocalDateTimeUtil.localTimeToLocalDateTime(source);
        return DateUtil.format(localDateTime, DatePattern.NORM_TIME_PATTERN);
    }

}
