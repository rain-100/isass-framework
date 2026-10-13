// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.jwt;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.security.jwt.JwtInfo;
import vip.isass.framework.web.security.authentication.AbstractAuthenticationFilter;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.authentication.multilogin.ShouldOfflineChecker;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Rain
 */
@Slf4j
public class JwtAuthenticationFilter extends AbstractAuthenticationFilter {

    private final ShouldOfflineChecker shouldOfflineChecker;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                   ShouldOfflineChecker shouldOfflineChecker) {
        this(authenticationManager, shouldOfflineChecker, null, false);
    }

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                   ShouldOfflineChecker shouldOfflineChecker,
                                   PrincipalAuthorizationLoader authorizationLoader,
                                   boolean loadRolesForLogging) {
        super(authenticationManager, authorizationLoader, loadRolesForLogging);
        this.shouldOfflineChecker = shouldOfflineChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(JwtConst.HEADER_NAME);

        if (StrUtil.isEmpty(header)) {
            chain.doFilter(request, response);
            return;
        }

        // API Key 的 Bearer 兼容格式由后续 ApiKeyAuthenticationFilter 处理，不能先按 JWT 解析。
        if (StrUtil.startWithIgnoreCase(header, "Bearer isass_sk_")) {
            chain.doFilter(request, response);
            return;
        }

        String token;
        if (header.startsWith(JwtConst.PREFIX)) {
            token = header.replace(JwtConst.PREFIX, "");
        } else if (header.startsWith(JwtConst.PREFIX_URL_ENCODED)) {
            token = header.replace(JwtConst.PREFIX_URL_ENCODED, "");
        } else {
            chain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof PrincipalAuthenticationToken existing
                && existing.hasBusinessPrincipal()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "同一请求不能同时携带多个 ISASS 认证凭证");
            return;
        }

        try {
            JwtAuthenticationToken authResult = (JwtAuthenticationToken) getAuthenticationManager()
                    .authenticate(new JwtAuthenticationToken(token));

            JwtInfo jwtClaim = authResult.getJwtClaim();
            DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                    .setPrincipalType(PrincipalType.USER)
                    .setPrincipalId(jwtClaim.getUid())
                    .setPrincipalName(jwtClaim.getName())
                    .setTenantId(jwtClaim.getTid())
                    .setAppId(jwtClaim.getAid())
                    .setLoginLogId(jwtClaim.getLid())
                    .setAuthenticationExpireAt(jwtClaim.getEat())
                    .setTerminalType(jwtClaim.getTt());

            // todo 判断账号是否禁用

            // 处理多端登录
            shouldOfflineChecker.checkShouldOffline(principal);

            // 只保存已认证身份；角色和功能权限由授权或业务执行入口按需装载。
            PrincipalAuthenticationToken authentication = saveAuthentication(principal, null);

            // 权限认证成功方法
            onSuccessfulAuthentication(request, response, authentication);
        } catch (AuthenticationServiceException failed) {
            SecurityContextHolder.clearContext();
            onUnsuccessfulAuthentication(request, response, failed);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "授权服务暂时不可用");
            return;
        } catch (AuthenticationException failed) {
            SecurityContextHolder.clearContext();
            onUnsuccessfulAuthentication(request, response, failed);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "token错误或过期");
            return;
        }

        chain.doFilter(request, response);
    }

}
