// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.web.security.authorization;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

/**
 * 显式分级装载并复用请求授权；null 上下文未加载，null 权限集合仅加载角色。
 */
public final class PrincipalAuthorizationContextResolver {
    private PrincipalAuthorizationContextResolver() {
    }

    public static PrincipalAuthorizationContext roles(PrincipalAuthenticationToken token,
                                                       PrincipalAuthorizationLoader loader) {
        return resolve(token, loader, false);
    }

    public static PrincipalAuthorizationContext permissions(PrincipalAuthenticationToken token,
                                                             PrincipalAuthorizationLoader loader) {
        return resolve(token, loader, true);
    }

    private static PrincipalAuthorizationContext resolve(PrincipalAuthenticationToken token,
                                                          PrincipalAuthorizationLoader loader,
                                                          boolean permissions) {
        synchronized (token) {
            if (!token.isAuthenticated() || !token.hasBusinessPrincipal()) {
                throw new AuthenticationCredentialsNotFoundException("需要有效业务主体才能加载授权");
            }
            PrincipalAuthorizationContext context = token.getAuthorizationContext();
            if (context != null && (!permissions || context.permissionsLoaded())) {
                return context;
            }
            try {
                context = permissions ? loader.load(token.getPrincipal()) : loader.loadRoles(token.getPrincipal());
                if (context == null || permissions && !context.permissionsLoaded()) {
                    throw new AuthenticationServiceException("主体授权上下文缺失或未完整装载");
                }
                token.setAuthorizationContext(context);
                return context;
            } catch (RuntimeException failure) {
                token.setAuthorizationContext(null);
                token.setAuthenticated(false);
                SecurityContextHolder.clearContext();
                throw new AuthenticationServiceException("主体授权上下文加载失败", failure);
            }
        }
    }
}
