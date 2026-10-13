// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vip.isass.framework.security.AuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;

import java.util.List;

/**
 * API Key 认证请求和认证结果。
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private String apiKey;
    private final AuthenticatedPrincipal principal;
    private final PrincipalAuthorizationContext authorizationContext;

    public ApiKeyAuthenticationToken(String apiKey) {
        super(List.of());
        this.apiKey = apiKey;
        this.principal = null;
        this.authorizationContext = null;
        super.setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(AuthenticatedPrincipal principal,
                                     PrincipalAuthorizationContext authorizationContext) {
        super(authorizationContext == null ? List.of() : authorizationContext.roleCodes().stream()
                .distinct()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList());
        this.principal = principal;
        this.authorizationContext = authorizationContext;
        this.apiKey = null;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public PrincipalAuthorizationContext getAuthorizationContext() {
        return authorizationContext;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        apiKey = null;
    }
}
