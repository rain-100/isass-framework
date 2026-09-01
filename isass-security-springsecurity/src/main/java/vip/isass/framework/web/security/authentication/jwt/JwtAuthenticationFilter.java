// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.jwt;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.common.security.jwt.JwtInfo;
import vip.isass.framework.web.security.authentication.AbstractAuthenticationFilter;
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

    private ShouldOfflineChecker shouldOfflineChecker;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                   ShouldOfflineChecker shouldOfflineChecker) {
        super(authenticationManager);
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
                instanceof vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken existing
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

            // 保存已验证的权限信息
            saveAuthentication(principal, authResult.getAuthorities());

            // 权限认证成功方法
            onSuccessfulAuthentication(request, response, authResult);
        } catch (AuthenticationException failed) {
            onUnsuccessfulAuthentication(request, response, failed);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "token错误或过期");
            return;
        }

        chain.doFilter(request, response);
    }


}
