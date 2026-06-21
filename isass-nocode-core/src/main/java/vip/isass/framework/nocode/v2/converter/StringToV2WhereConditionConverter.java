package vip.isass.framework.nocode.v2.converter;

import lombok.SneakyThrows;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.nocode.v2.criteria.V2WhereCondition;

/**
 * 把 json 字符串类型的 v2 查询条件转换成 V2WhereCondition。
 *
 * @author Rain
 */
public class StringToV2WhereConditionConverter implements Converter<String, V2WhereCondition> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof String;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return V2WhereCondition.class.isAssignableFrom(clazz);
    }

    @Override
    @SneakyThrows
    public V2WhereCondition convert(String source) {
        return JsonUtil.DEFAULT_INSTANCE.readValue(source, V2WhereCondition.class);
    }

}
