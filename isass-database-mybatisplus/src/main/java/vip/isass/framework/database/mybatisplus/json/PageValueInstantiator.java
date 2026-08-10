// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.json;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyMetadata;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.deser.CreatorProperty;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.std.StdValueInstantiator;

import java.util.List;

public class PageValueInstantiator extends StdValueInstantiator {
    private final JavaType entityJavaType;

    public PageValueInstantiator(StdValueInstantiator valueInstantiator, JavaType entityJavaType) {
        super(valueInstantiator);
        this.entityJavaType = entityJavaType;
    }

    @Override
    public boolean canCreateFromObjectWith() {
        return true;
    }

    @Override
    public SettableBeanProperty[] getFromObjectArguments(DeserializationConfig config) {
        JavaType longType = config.constructType(long.class);
        JavaType contentElementType = config.getTypeFactory().constructCollectionType(List.class, entityJavaType);
        JavaType orderItemType = config.getTypeFactory().constructCollectionType(List.class, OrderItem.class);
        return new SettableBeanProperty[]{
                creatorProperty("current", longType, 0, PropertyMetadata.STD_OPTIONAL),
                creatorProperty("size", longType, 1, PropertyMetadata.STD_OPTIONAL),
                creatorProperty("total", longType, 2, PropertyMetadata.STD_OPTIONAL),
                creatorProperty("orders", orderItemType, 3, PropertyMetadata.STD_REQUIRED),
                creatorProperty("records", contentElementType, 4, PropertyMetadata.STD_OPTIONAL)
        };
    }

    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) {
        Page<Object> page = new Page<>();
        if (args[0] != null) {
            page.setCurrent((long) args[0]);
        }
        if (args[1] != null) {
            page.setSize((long) args[1]);
        }
        if (args[2] != null) {
            page.setTotal((long) args[2]);
        }
        if (args[3] != null) {
            page.setOrders((List<OrderItem>) args[3]);
        }
        if (args[4] != null) {
            page.setRecords((List<Object>) args[4]);
        }
        return page;
    }

    private CreatorProperty creatorProperty(String name, JavaType type, int index, PropertyMetadata metadata) {
        return CreatorProperty.construct(
                PropertyName.construct(name),
                type,
                null,
                null,
                null,
                null,
                index,
                null,
                metadata);
    }
}

