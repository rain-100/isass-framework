// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalTime;

/**
 * 把任何表示形式的 string 类型的日期时间，转换成 LocalTime
 *
 * @author Rain
 */
public class StringToLocalTimeConverter implements Converter<String, LocalTime> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof String;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return LocalTime.class.isAssignableFrom(clazz);
    }

    @Override
    public LocalTime convert(String source) {
        return convert0(source);
    }

    public static LocalTime convert0(String source) {
        Long timestamp = StringDateToMillisConverter.convert0(source);
        return timestamp == null ? null : LocalDateTimeUtil.epochMilliToLocalTime(timestamp);
    }

}
