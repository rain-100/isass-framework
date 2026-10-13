// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticationRejectedException;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticationRequest;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticationResult;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticator;

/**
 * 将权限服务校验出的 API Key 应用主体转换为 Spring Security 认证结果。
 */
@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyAuthenticator authenticator;

    public ApiKeyAuthenticationProvider(ApiKeyAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        ApiKeyAuthenticationResult result;
        try {
            result = authenticator.authenticate(
                    new ApiKeyAuthenticationRequest((String) authentication.getCredentials()));
        } catch (ApiKeyAuthenticationRejectedException exception) {
            throw new BadCredentialsException("API Key 无效或已失效", exception);
        } catch (RuntimeException exception) {
            throw new AuthenticationServiceException("API Key 认证服务暂时不可用", exception);
        }
        if (result == null || result.principal() == null) {
            throw new BadCredentialsException("API Key 无效");
        }
        PrincipalAuthorizationContext context = result.authorizationContext();
        if (context == null) {
            throw new AuthenticationServiceException("API Key 主体授权上下文为空");
        }
        return new ApiKeyAuthenticationToken(result.principal(), context);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
