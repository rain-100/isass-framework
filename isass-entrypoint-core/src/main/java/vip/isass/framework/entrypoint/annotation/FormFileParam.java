// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface FormFileParam {
    String value();
}
