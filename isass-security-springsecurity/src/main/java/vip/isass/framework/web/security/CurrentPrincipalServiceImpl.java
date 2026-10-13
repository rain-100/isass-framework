// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vip.isass.framework.security.AuthenticatedPrincipal;
import vip.isass.framework.security.CurrentPrincipalService;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.authorization.PrincipalAuthorizationContextResolver;

/** Reads the authenticated principal from Spring Security's request context. */
@Service
public class CurrentPrincipalServiceImpl implements CurrentPrincipalService {
    private final PrincipalAuthorizationLoader authorizationLoader;

    @Autowired
    public CurrentPrincipalServiceImpl(PrincipalAuthorizationLoader authorizationLoader) {
        this.authorizationLoader = authorizationLoader;
    }

    public CurrentPrincipalServiceImpl() {
        this.authorizationLoader = null;
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof PrincipalAuthenticationToken token ? token.getPrincipal() : null;
    }

    @Override
    public InternalServicePrincipal getInternalServicePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof PrincipalAuthenticationToken token
                ? token.getInternalServicePrincipal() : null;
    }

    @Override
    public PrincipalAuthorizationContext getAuthorizationContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof PrincipalAuthenticationToken token
                ? token.getAuthorizationContext() : null;
    }

    @Override
    public PrincipalAuthorizationContext loadRoleContext() {
        return PrincipalAuthorizationContextResolver.roles(businessToken(), authorizationLoader);
    }

    @Override
    public PrincipalAuthorizationContext loadAuthorizationContext() {
        return PrincipalAuthorizationContextResolver.permissions(businessToken(), authorizationLoader);
    }

    @Override
    public void cacheAuthorizationContext(PrincipalAuthorizationContext context) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof PrincipalAuthenticationToken token && token.hasBusinessPrincipal()) {
            synchronized (token) {
                PrincipalAuthorizationContext current = token.getAuthorizationContext();
                if (current == null || !current.permissionsLoaded() || context == null || context.permissionsLoaded()) {
                    token.setAuthorizationContext(context);
                }
            }
        }
    }

    private PrincipalAuthenticationToken businessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof PrincipalAuthenticationToken token && token.hasBusinessPrincipal()
                && token.isAuthenticated()) {
            return token;
        }
        throw new AuthenticationCredentialsNotFoundException("当前请求没有已认证业务主体");
    }
}
