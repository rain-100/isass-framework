// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vip.isass.framework.security.AuthenticatedPrincipal;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 中唯一的已认证主体包装。
 */
public class PrincipalAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedPrincipal principal;
    private final InternalServicePrincipal internalServicePrincipal;
    private volatile PrincipalAuthorizationContext principalAuthorizationContext;

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal) {
        this(principal, null, null);
    }

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        PrincipalAuthorizationContext principalAuthorizationContext) {
        this(principal, null, principalAuthorizationContext);
    }

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        InternalServicePrincipal internalServicePrincipal,
                                        PrincipalAuthorizationContext principalAuthorizationContext) {
        super(List.of());
        if (principal == null && internalServicePrincipal == null) {
            throw new IllegalArgumentException("业务主体和内部服务主体不能同时为空");
        }
        this.principal = principal;
        this.internalServicePrincipal = internalServicePrincipal;
        this.principalAuthorizationContext = principalAuthorizationContext;
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
        return principalAuthorizationContext;
    }

    public void setAuthorizationContext(PrincipalAuthorizationContext principalAuthorizationContext) {
        this.principalAuthorizationContext = principalAuthorizationContext;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        PrincipalAuthorizationContext context = principalAuthorizationContext;
        if (context == null) {
            return List.of();
        }
        return context.roleCodes().stream()
                .distinct()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
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
