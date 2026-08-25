// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.web.security.authorization.PrincipalAuthorizationContext;

import java.util.Collection;

/**
 * Spring Security 中唯一的已认证主体包装。
 */
public class PrincipalAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedPrincipal principal;
    private volatile PrincipalAuthorizationContext authorizationContext;

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        Collection<? extends GrantedAuthority> authorities) {
        this(principal, authorities, null);
    }

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        Collection<? extends GrantedAuthority> authorities,
                                        PrincipalAuthorizationContext authorizationContext) {
        super(authorities);
        this.principal = principal;
        this.authorizationContext = authorizationContext;
        super.setAuthenticated(true);
    }

    public PrincipalAuthorizationContext getAuthorizationContext() {
        return authorizationContext;
    }

    public void setAuthorizationContext(PrincipalAuthorizationContext authorizationContext) {
        this.authorizationContext = authorizationContext;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Use the authenticated constructor");
        }
        super.setAuthenticated(false);
    }
}
