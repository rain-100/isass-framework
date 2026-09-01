// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization.internal;

/** 使用 Java DSL 声明当前服务允许内部 HMAC 身份访问的 Entrypoint 操作。 */
@FunctionalInterface
public interface InternalAccessProvider {

    void defineInternalAccess(InternalAccessBuilder builder);
}
