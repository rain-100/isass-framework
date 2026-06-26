package vip.isass.framework.nocode.v3.converter;

import lombok.SneakyThrows;
import vip.isass.framework.common.support.Converter;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.nocode.v3.criteria.V3WhereCondition;

/**
 * 把 json 字符串类型的 v3 查询条件转换成 V3WhereCondition。
 *
 * @author Rain
 */
public class StringToV3WhereConditionConverter implements Converter<String, V3WhereCondition> {

    @Override
    public boolean supportSourceType(Object source) {
        return source instanceof String;
    }

    @Override
    public boolean supportTargetClass(Class clazz) {
        return V3WhereCondition.class.isAssignableFrom(clazz);
    }

    @Override
    @SneakyThrows
    public V3WhereCondition convert(String source) {
        return JsonUtil.DEFAULT_INSTANCE.readValue(source, V3WhereCondition.class);
    }

}
