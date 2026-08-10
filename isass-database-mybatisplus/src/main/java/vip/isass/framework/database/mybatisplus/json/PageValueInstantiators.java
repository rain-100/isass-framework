// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.json;

import com.baomidou.mybatisplus.core.metadata.IPage;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.ValueInstantiators;
import tools.jackson.databind.deser.std.StdValueInstantiator;

public class PageValueInstantiators implements ValueInstantiators {

    @Override
    public ValueInstantiator findValueInstantiator(DeserializationConfig config, BeanDescription.Supplier beanDescSupplier) {
        return null;
    }

    @Override
    public ValueInstantiator modifyValueInstantiator(DeserializationConfig config, BeanDescription.Supplier beanDescSupplier, ValueInstantiator defaultInstantiator) {
        BeanDescription beanDesc = beanDescSupplier.get();
        if (beanDesc.getBeanClass() == IPage.class) {
            return toPageValueInstantiator(beanDesc, defaultInstantiator);
        }
        return defaultInstantiator;
    }

    private ValueInstantiator toPageValueInstantiator(BeanDescription beanDesc, ValueInstantiator defaultInstantiator) {
        if (beanDesc.getType().containedTypeCount() == 1) {
            // 如果泛型是 <?> 则得到的 JavaType 是 object
            JavaType entityJavaType = beanDesc.getType().containedType(0);
            return new PageValueInstantiator((StdValueInstantiator) defaultInstantiator, entityJavaType);
        } else {
            throw new IllegalStateException("No generic type specified for Page!");
        }
    }
}
