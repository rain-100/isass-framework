// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter;

import cn.hutool.core.util.StrUtil;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.JsonUtil;

import java.util.Map;

/**
 * 把 json 字符串转成 map
 *
 * @author Rain
 */
public class StringToMapConverter implements Converter<String, Map<String, Object>> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof String;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    @Override
    public Map<String, Object> convert(String source) {
        return StrUtil.isBlank(source) ? null : JsonUtil.readMap(source);
    }

}
