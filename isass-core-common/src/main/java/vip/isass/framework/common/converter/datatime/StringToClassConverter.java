// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import cn.hutool.core.util.StrUtil;
import vip.isass.framework.common.support.Converter;

/**
 * 把全限定类名字符串转换成 class 对象
 *
 * @author Rain
 */
public class StringToClassConverter implements Converter<String, Class> {

    private static final String PREFIX = "class ";

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof String;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return Class.class.isAssignableFrom(clazz);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class convert(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }
        try {
            return Class.forName(StrUtil.removePrefix(source, PREFIX));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
