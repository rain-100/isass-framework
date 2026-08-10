// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.converter;

import lombok.SneakyThrows;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.nocode.criteria.WhereCondition;

/**
 * 把 json 字符串类型的 nocode 查询条件转换成 WhereCondition。
 *
 * @author Rain
 */
public class StringToWhereConditionConverter implements Converter<String, WhereCondition> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof String;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return WhereCondition.class.isAssignableFrom(clazz);
    }

    @Override
    @SneakyThrows
    public WhereCondition convert(String source) {
        return JsonUtil.DEFAULT_INSTANCE.readValue(source, WhereCondition.class);
    }

}
