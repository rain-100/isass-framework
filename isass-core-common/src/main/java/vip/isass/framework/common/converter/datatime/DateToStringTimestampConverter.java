// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import vip.isass.framework.common.support.Converter;

import java.util.Date;

/**
 * 把 Date 类型，转换成 String 类型的时间戳
 *
 * @author Rain
 */
public class DateToStringTimestampConverter implements Converter<Date, String> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof Date;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    public String convert(Date source) {
        return source.getTime() + "";
    }

}
