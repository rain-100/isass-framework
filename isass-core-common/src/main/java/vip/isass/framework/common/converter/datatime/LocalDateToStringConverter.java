// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 把 LocalDate 类型的日期时间，转换成 yyyy-MM-dd
 *
 * @author Rain
 */
public class LocalDateToStringConverter implements Converter<LocalDate, String> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof LocalDate;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    public String convert(LocalDate source) {
        return convert0(source);
    }

    public static String convert0(LocalDate source) {
        LocalDateTime localDateTime = LocalDateTimeUtil.localDateToLocalDateTime(source);
        return DateUtil.format(localDateTime, DatePattern.NORM_DATE_PATTERN);
    }

}
