// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.converter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.TypeUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JavaType;
import vip.isass.framework.common.support.JsonUtil;

import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * 类型转换工具
 *
 * @author rain
 */
@Slf4j
public class ConvertUtil {

    /**
     * 类型转换，支持任意目标类型，包括基础类型、简单对象、复杂泛型对象
     *
     * @param actualType 真实类型，如果有泛型，需要包含泛型信息
     * @param value      待转换的值
     * @param <T>        返回类型
     * @return 转换后的结构
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> T convert(Type actualType, Object value) {
        if (value == null) {
            return null;
        }
        Class<T> clazz = (Class<T>) TypeUtil.getClass(actualType);
        if (clazz == Object.class) {
            return (T) value;
        }

        if (clazz.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        if (ClassUtil.isBasicType(clazz)) {
            return Convert.convert(clazz, value);
        } else if (clazz == String.class) {
            return (T) value.toString();
        } else {
            JavaType javaType = JsonUtil.DEFAULT_INSTANCE.getTypeFactory().constructType(actualType);
            if (value instanceof String) {
                return JsonUtil.DEFAULT_INSTANCE.readValue(value.toString(), javaType);
            } else if (value instanceof byte[]) {
                return JsonUtil.DEFAULT_INSTANCE.readValue((byte[]) value, javaType);
            } else if (value instanceof InputStream) {
                return JsonUtil.DEFAULT_INSTANCE.readValue((InputStream) value, javaType);
            }
            return JsonUtil.DEFAULT_INSTANCE.convertValue(value, javaType);
        }
    }
}
