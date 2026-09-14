// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 简单类型读取指定表单字段；对象类型按 Java 属性名绑定平铺表单字段，不需要参数名前缀。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface FormFieldParam {
    String value();
}
