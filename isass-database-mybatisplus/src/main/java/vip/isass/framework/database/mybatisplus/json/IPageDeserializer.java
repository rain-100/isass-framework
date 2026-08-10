// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.json;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import lombok.SneakyThrows;
import vip.isass.framework.common.support.JsonUtil;

/**
 * 使用 ValueDeserializer 的方式解决 Ipage 泛型接口的反序列化问题
 */
public class IPageDeserializer extends ValueDeserializer<IPage<?>> {

    private JavaType entityType;

    @Override
    @SneakyThrows
    public IPage<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JavaType pageType = JsonUtil.DEFAULT_INSTANCE.getTypeFactory().constructParametricType(Page.class, entityType);
        return deserializationContext.readValue(jsonParser, pageType);
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            IPageDeserializer parser = new IPageDeserializer();
            parser.entityType = ctxt.getContextualType().containedType(0);
            return parser;
        } else {
            JavaType wrapperType = property.getType();
            JavaType valueType = wrapperType.containedType(0);
            IPageDeserializer parser = new IPageDeserializer();
            parser.entityType = valueType;
            return parser;
        }
    }
}
