// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.common.security.InternalServicePrincipal;
import vip.isass.framework.web.security.authorization.PrincipalAuthorizationContext;

import java.util.Collection;

/**
 * Spring Security 中唯一的已认证主体包装。
 */
public class PrincipalAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedPrincipal principal;
    private final InternalServicePrincipal internalServicePrincipal;
    private volatile PrincipalAuthorizationContext authorizationContext;

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        Collection<? extends GrantedAuthority> authorities) {
        this(principal, null, authorities, null);
    }

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        Collection<? extends GrantedAuthority> authorities,
                                        PrincipalAuthorizationContext authorizationContext) {
        this(principal, null, authorities, authorizationContext);
    }

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        InternalServicePrincipal internalServicePrincipal,
                                        Collection<? extends GrantedAuthority> authorities,
                                        PrincipalAuthorizationContext authorizationContext) {
        super(authorities);
        if (principal == null && internalServicePrincipal == null) {
            throw new IllegalArgumentException("业务主体和内部服务主体不能同时为空");
        }
        this.principal = principal;
        this.internalServicePrincipal = internalServicePrincipal;
        this.authorizationContext = authorizationContext;
        super.setAuthenticated(true);
    }

    public InternalServicePrincipal getInternalServicePrincipal() {
        return internalServicePrincipal;
    }

    public boolean hasBusinessPrincipal() {
        return principal != null;
    }

    public boolean hasInternalServicePrincipal() {
        return internalServicePrincipal != null;
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
    public String getName() {
        if (principal != null) return String.valueOf(principal.getPrincipalId());
        return internalServicePrincipal == null ? "" : internalServicePrincipal.serviceName();
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Use the authenticated constructor");
        }
        super.setAuthenticated(false);
    }
}
