// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.annotation;

import vip.isass.framework.entrypoint.metadata.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines one operation exposed by HTTP, gRPC and remote entrypoint proxies. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntrypointOperation {

    String operationName();

    String displayName();

    String description() default "";

    int displayOrder() default 0;

    HttpMethod httpMethod();

    /**
     * Whether the current operation allows requests without an authenticated principal.
     * Business credentials such as HMAC signatures or API keys must still be validated by
     * the operation implementation when required.
     */
    boolean allowAnonymous() default false;
}
