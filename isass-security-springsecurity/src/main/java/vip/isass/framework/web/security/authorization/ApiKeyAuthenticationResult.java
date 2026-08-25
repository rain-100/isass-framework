// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;

/**
 * API Key 认证成功后返回的应用主体和授权上下文。
 */
public record ApiKeyAuthenticationResult(
        DefaultAuthenticatedPrincipal principal,
        PrincipalAuthorizationContext authorizationContext) {
}
