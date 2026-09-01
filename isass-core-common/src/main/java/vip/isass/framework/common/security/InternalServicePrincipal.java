// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security;

/**
 * 当前请求中经内部 HMAC 认证的调用服务主体。
 *
 * @param serviceName 调用方微服务名
 * @param keyId       HMAC 密钥标识
 * @param requestId   调用链中的请求标识
 */
public record InternalServicePrincipal(
        String serviceName,
        String keyId,
        String requestId
) {
}
