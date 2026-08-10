// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.log;

import org.slf4j.event.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志记录注解
 *
 * @author rain
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InvokeLog {

    /**
     * 日志级别
     *
     * @return 日志级别
     */
    Level level() default Level.DEBUG;

    /**
     * 是否记录方法参数
     *
     * @return 是否记录方法参数
     */
    boolean parameter() default true;

}
