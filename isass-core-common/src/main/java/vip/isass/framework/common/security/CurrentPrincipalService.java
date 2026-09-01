// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security;

/**
 * 提供当前请求已认证主体。
 */
public interface CurrentPrincipalService {

    /** 返回 JWT 或 API Key 认证得到的业务主体。 */
    AuthenticatedPrincipal getPrincipal();

    /** 返回 HMAC 认证得到的内部调用服务主体。 */
    default InternalServicePrincipal getInternalServicePrincipal() {
        return null;
    }
}
