// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.web.security.authentication.AbstractAuthenticationFilter;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 使用 API Key 认证应用主体。 */
public class ApiKeyAuthenticationFilter extends AbstractAuthenticationFilter {

    public static final String HEADER_NAME = "X-ISASS-API-Key";
    private static final String AUTHORIZATION_PREFIX = "Bearer isass_sk_";

    public ApiKeyAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String headerApiKey = request.getHeader(HEADER_NAME);
        String authorization = request.getHeader("Authorization");
        boolean authorizationApiKey = StrUtil.startWithIgnoreCase(authorization, AUTHORIZATION_PREFIX);
        if (StrUtil.isNotBlank(headerApiKey) && authorizationApiKey) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "同一请求不能同时携带多个 ISASS 认证凭证");
            return;
        }
        String apiKey = StrUtil.isBlank(headerApiKey) && authorizationApiKey
                ? authorization.substring("Bearer ".length()).trim() : headerApiKey;
        if (StrUtil.isBlank(apiKey)) {
            chain.doFilter(request, response);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof PrincipalAuthenticationToken) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "同一请求不能同时携带多个 ISASS 认证凭证");
            return;
        }
        try {
            ApiKeyAuthenticationToken result = (ApiKeyAuthenticationToken) getAuthenticationManager()
                    .authenticate(new ApiKeyAuthenticationToken(apiKey));
            AuthenticatedPrincipal principal = (AuthenticatedPrincipal) result.getPrincipal();
            saveAuthentication(principal, result.getAuthorities());
            onSuccessfulAuthentication(request, response, result);
        } catch (AuthenticationException failed) {
            onUnsuccessfulAuthentication(request, response, failed);
        }
        chain.doFilter(request, response);
    }
}
