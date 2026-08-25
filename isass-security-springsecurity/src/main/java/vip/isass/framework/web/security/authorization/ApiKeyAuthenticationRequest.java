// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

/**
 * API Key 授权上下文查询请求。完整 API Key 只能通过请求体传输。
 */
public record ApiKeyAuthenticationRequest(String apiKey) {
}
