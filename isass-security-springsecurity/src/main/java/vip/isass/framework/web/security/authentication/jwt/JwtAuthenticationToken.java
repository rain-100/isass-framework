// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.jwt;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import vip.isass.framework.security.jwt.JwtInfo;

import java.util.List;

/**
 * JWT 校验的临时凭证；请求主体及授权结果统一保存在 PrincipalAuthenticationToken。
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private String token;

    @Getter
    private JwtInfo jwtClaim;

    public JwtAuthenticationToken(String token) {
        super(List.of());
        this.token = token;
        super.setAuthenticated(false);
    }

    public JwtAuthenticationToken(String token, JwtInfo jwtClaim) {
        super(List.of());
        this.token = token;
        this.jwtClaim = jwtClaim;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return jwtClaim == null ? null : jwtClaim.getUid();
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use the verified JWT constructor instead");
        }

        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        token = null;
        // The authenticated filter reads the claims before credentials are erased.
    }

}
