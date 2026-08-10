// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.jwt;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import vip.isass.framework.common.security.jwt.JwtInfo;

import java.util.Collection;

/**
 * @author Rain
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private String token;

    @Getter
    private JwtInfo jwtClaim;

    public JwtAuthenticationToken(String token) {
        super(java.util.Collections.emptyList());
        this.token = token;
        super.setAuthenticated(false);
    }

    public JwtAuthenticationToken(String token, JwtInfo jwtClaim, Collection<? extends GrantedAuthority> authorities) {
        super((Collection<GrantedAuthority>) authorities);
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
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
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
