package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import vip.isass.framework.common.security.AuthenticatedPrincipal;

import java.util.Collection;

/**
 * API Key 认证请求和认证结果。
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private String apiKey;
    private final AuthenticatedPrincipal principal;

    public ApiKeyAuthenticationToken(String apiKey) {
        super(java.util.Collections.emptyList());
        this.apiKey = apiKey;
        this.principal = null;
        super.setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(AuthenticatedPrincipal principal,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.apiKey = null;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        apiKey = null;
    }
}
