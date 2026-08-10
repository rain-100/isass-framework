// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication;

import cn.hutool.extra.servlet.JakartaServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vip.isass.framework.common.security.AuthenticatedPrincipal;

import java.util.Collection;

/**
 * @author Rain
 */
@Slf4j
public abstract class AbstractAuthenticationFilter extends BasicAuthenticationFilter {

    public AbstractAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    protected void saveAuthentication(AuthenticatedPrincipal principal, Collection<GrantedAuthority> authorities) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current instanceof PrincipalAuthenticationToken) {
            throw new IllegalStateException("不允许同时使用多种 ISASS 认证凭证");
        }
        SecurityContextHolder.getContext().setAuthentication(new PrincipalAuthenticationToken(principal, authorities));
    }

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof PrincipalAuthenticationToken principalAuthenticationToken) {
            AuthenticatedPrincipal principal = principalAuthenticationToken.getPrincipal();
            log.debug("认证成功[{}], 正在访问[{} {}], userId[{}], name[{}], 拥有角色{}, 源ip[{}]",
                this.getClass().getSimpleName(),
                request.getMethod(),
                request.getRequestURL(),
                principal.getPrincipalId(),
                principal.getPrincipalName(),
                authentication.getAuthorities(),
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
