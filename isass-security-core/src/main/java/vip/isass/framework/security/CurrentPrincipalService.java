// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security;

/**
 * 提供当前请求已认证主体。
 */
public interface CurrentPrincipalService {

    /** 返回 JWT 或 API Key 认证得到的业务主体。 */
    AuthenticatedPrincipal getPrincipal();

    /** Returns authorization already loaded for this request; never performs I/O. */
    default PrincipalAuthorizationContext getAuthorizationContext() {
        return null;
    }

    /** Explicitly loads roles for this request; unlike getters, this method may perform I/O. */
    default PrincipalAuthorizationContext loadRoleContext() {
        PrincipalAuthorizationContext context = getAuthorizationContext();
        if (context == null) {
            throw new IllegalStateException("当前主体角色尚未装载，且未配置授权加载器");
        }
        return context;
    }

    /** Explicitly loads complete authorization for this request. */
    default PrincipalAuthorizationContext loadAuthorizationContext() {
        PrincipalAuthorizationContext context = loadRoleContext();
        if (!context.permissionsLoaded()) {
            throw new IllegalStateException("当前主体功能权限尚未装载，且未配置授权加载器");
        }
        return context;
    }

    /** Memoizes a calculated context for the current request. Non-request implementations may ignore it. */
    default void cacheAuthorizationContext(PrincipalAuthorizationContext context) {
    }

    /** 返回 HMAC 认证得到的内部调用服务主体。 */
    default InternalServicePrincipal getInternalServicePrincipal() {
        return null;
    }
}
