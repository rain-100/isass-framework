// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares service order without depending on Spring's {@code @Order}.
 *
 * @author isass
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface IsassOrder {

    int value();
}
