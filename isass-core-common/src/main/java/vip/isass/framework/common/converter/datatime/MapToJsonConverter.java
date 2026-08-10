// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter.datatime;

import lombok.SneakyThrows;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.JsonUtil;

import java.util.Map;

/**
 * 把 Map 类型，转换成 json 字符串
 *
 * @author Rain
 */
public class MapToJsonConverter implements Converter<Map, String> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof Map;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    @SneakyThrows
    public String convert(Map source) {
        return JsonUtil.NOT_NULL_INSTANCE.writeValueAsString(source);
    }

}
