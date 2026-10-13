// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import vip.isass.framework.security.AuthenticatedPrincipal;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import vip.isass.framework.web.security.authorization.PrincipalAuthorizationContextResolver;

import java.util.Objects;

/**
 * @author Rain
 */
@Slf4j
public abstract class AbstractAuthenticationFilter extends BasicAuthenticationFilter {

    private final PrincipalAuthorizationLoader authorizationLoader;
    private final boolean loadRolesForLogging;

    public AbstractAuthenticationFilter(AuthenticationManager authenticationManager) {
        this(authenticationManager, null, false);
    }

    public AbstractAuthenticationFilter(AuthenticationManager authenticationManager,
                                        PrincipalAuthorizationLoader authorizationLoader,
                                        boolean loadRolesForLogging) {
        super(authenticationManager);
        this.authorizationLoader = loadRolesForLogging
                ? Objects.requireNonNull(authorizationLoader, "角色调试日志需要授权加载器") : authorizationLoader;
        this.loadRolesForLogging = loadRolesForLogging;
    }

    protected PrincipalAuthenticationToken saveAuthentication(
            AuthenticatedPrincipal principal, PrincipalAuthorizationContext principalAuthorizationContext) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current instanceof PrincipalAuthenticationToken token && token.hasBusinessPrincipal()) {
            throw new IllegalStateException("不允许同时使用多种 ISASS 认证凭证");
        }
        InternalServicePrincipal internalPrincipal =
                current instanceof PrincipalAuthenticationToken token
                        ? token.getInternalServicePrincipal() : null;
        PrincipalAuthenticationToken authentication = new PrincipalAuthenticationToken(
                principal, internalPrincipal, principalAuthorizationContext);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof PrincipalAuthenticationToken principalAuthenticationToken) {
            AuthenticatedPrincipal principal = principalAuthenticationToken.getPrincipal();
            if (principal == null) return;
            if (loadRolesForLogging) {
                PrincipalAuthorizationContext context = PrincipalAuthorizationContextResolver.roles(
                        principalAuthenticationToken, authorizationLoader);
                log.debug("认证成功[{}], 正在访问[{} {}], userId[{}], name[{}], 拥有角色{}, 源ip[{}]",
                        this.getClass().getSimpleName(),
                        request.getMethod(),
                        request.getRequestURL(),
                        principal.getPrincipalId(),
                        principal.getPrincipalName(),
                        context.roleCodes(),
                        JakartaServletUtil.getClientIP(request));
                return;
            }
            log.debug("认证成功[{}], 正在访问[{} {}], userId[{}], name[{}], 源ip[{}]",
                    this.getClass().getSimpleName(),
                    request.getMethod(),
                    request.getRequestURL(),
                    principal.getPrincipalId(),
                    principal.getPrincipalName(),
                    JakartaServletUtil.getClientIP(request));
        }
    }

    @Override
    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        log.debug("认证失败[{}]，正在访问 {} {}, 源ip[{}]",
                this.getClass().getSimpleName(),
                request.getMethod(),
                request.getRequestURL(),
                JakartaServletUtil.getClientIP(request));
    }

}
