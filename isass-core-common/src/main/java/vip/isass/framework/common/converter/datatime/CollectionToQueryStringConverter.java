// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import cn.hutool.core.collection.CollUtil;
import vip.isass.framework.common.support.Converter;

import java.util.Collection;

/**
 * 把 Collection 类型，转换成 http query string，用逗号拼接
 *
 * @author Rain
 */
@SuppressWarnings("rawtypes")
public class CollectionToQueryStringConverter implements Converter<Collection, String> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof Collection;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    public String convert(Collection source) {
        return convert0(source);
    }

    @SuppressWarnings("unchecked")
    public static String convert0(Collection source) {
        return CollUtil.join(source, ",");
    }

}
