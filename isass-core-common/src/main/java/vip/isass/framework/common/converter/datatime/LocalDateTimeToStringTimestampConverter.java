// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalDateTime;

/**
 * 把 Date 类型，转换成 String 类型的时间戳
 *
 * @author Rain
 */
public class LocalDateTimeToStringTimestampConverter implements Converter<LocalDateTime, String> {

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
        return LocalDateTimeUtil.localDateTimeToEpochMilli(source) + "";
    }

}
