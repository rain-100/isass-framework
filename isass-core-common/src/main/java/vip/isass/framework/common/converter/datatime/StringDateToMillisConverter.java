// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.format.FastDateFormat;
import cn.hutool.core.util.StrUtil;
import vip.isass.framework.common.support.Converter;

/**
 * 把任何表示形式的 string 类型的日期时间，转换成 long 类型的时间戳
 *
 * @author Rain
 */
public class StringDateToMillisConverter implements Converter<String, Long> {

    private static final String FORMAT = "yyyy/M/dd HH:mm";

    private static final FastDateFormat SDF = FastDateFormat.getInstance(FORMAT);

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof String;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return Long.class.isAssignableFrom(clazz);
    }

    @Override
    public Long convert(String source) {
        return convert0(source);
    }

    public static Long convert0(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }

        try {
            return Long.parseLong(source);
        } catch (NumberFormatException e) {
            // do nothing
        }

        try {
            return DateUtil.parse(source).getTime();
        } catch (Exception e) {
            // do nothing
        }

        if (source.length() == FORMAT.length() || source.length() == FORMAT.length() + 1) {
            return DateUtil.parse(source, SDF).getTime();
        }

        throw new IllegalArgumentException("StringDateToLong 转换失败：" + source);
    }

}
