// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security.authentication.apikey;

import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;

/**
 * API Key 认证成功后返回的应用主体和授权上下文。
 */
public record ApiKeyAuthenticationResult(
        DefaultAuthenticatedPrincipal principal,
        PrincipalAuthorizationContext authorizationContext) {
}
