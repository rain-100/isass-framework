package vip.isass.framework.web.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import vip.isass.framework.common.security.AuthenticatedPrincipal;

import java.util.Collection;

/**
 * Spring Security 中唯一的已认证主体包装。
 */
public class PrincipalAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedPrincipal principal;

    public PrincipalAuthenticationToken(AuthenticatedPrincipal principal,
                                        Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
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
    public void setAuthenticated(boolean isAuthenticated) {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Use the authenticated constructor");
        }
        super.setAuthenticated(false);
    }
}
