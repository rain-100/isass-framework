// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import vip.isass.framework.entrypoint.transport.EntrypointRemoteBusinessException;
import vip.isass.framework.web.security.IsassGrantedAuthority;
import vip.isass.framework.web.security.authorization.ApiKeyAuthenticationRequest;
import vip.isass.framework.web.security.authorization.ApiKeyAuthenticationResult;
import vip.isass.framework.web.security.authorization.IAuthorizationService;
import vip.isass.framework.web.security.authorization.PrincipalAuthorizationContext;

import java.util.Collection;
import java.util.List;

/**
 * 将权限服务校验出的 API Key 应用主体转换为 Spring Security 认证结果。
 */
@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final IAuthorizationService authorizationService;

    public ApiKeyAuthenticationProvider(IAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        ApiKeyAuthenticationResult result;
        try {
            result = authorizationService.apiKeyContext(
                    new ApiKeyAuthenticationRequest((String) authentication.getCredentials()));
        } catch (EntrypointRemoteBusinessException exception) {
            throw new BadCredentialsException("API Key 无效或已失效", exception);
        }
        if (result == null || result.principal() == null) {
            throw new BadCredentialsException("API Key 无效");
        }
        PrincipalAuthorizationContext context = result.authorizationContext() == null
                ? PrincipalAuthorizationContext.empty()
                : result.authorizationContext();
        Collection<String> roleCodes = context.roleCodes() == null ? List.of() : context.roleCodes();
        return new ApiKeyAuthenticationToken(result.principal(), roleCodes.stream()
                .map(IsassGrantedAuthority::new)
                .toList(), context);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
