// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines the stable address and documentation group of an entrypoint. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface EntrypointInfo {

    String serviceName();

    String contextName();

    String resourceName();

    /** OpenAPI/Knife4j 分组排序值；数值越小越靠前。 */
    int displayOrder() default 1000;

    /** OpenAPI/Knife4j 中文分组名；为空时回退为 contextName/resourceName。 */
    String tag() default "";
}
