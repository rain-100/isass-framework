// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.security.apikey.ApiKeyAuthenticationService;
import vip.isass.framework.web.security.IsassGrantedAuthority;

import java.util.Collection;
import java.util.List;

/** 将权限服务校验出的 API Key 应用主体转换为 Spring Security 认证结果。 */
@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ObjectProvider<ApiKeyAuthenticationService> authenticationServiceProvider;

    public ApiKeyAuthenticationProvider(ObjectProvider<ApiKeyAuthenticationService> authenticationServiceProvider) {
        this.authenticationServiceProvider = authenticationServiceProvider;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        ApiKeyAuthenticationService service = authenticationServiceProvider.getIfAvailable();
        if (service == null) {
            throw new BadCredentialsException("当前服务未配置 API Key 认证能力");
        }
        ApiKeyAuthenticationService.ApiKeyAuthenticationResult result = service.authenticate(
                (String) authentication.getCredentials());
        if (result == null || result.principal() == null) {
            throw new BadCredentialsException("API Key 无效");
        }
        Collection<String> roleCodes = result.roleCodes() == null ? List.of() : result.roleCodes();
        return new ApiKeyAuthenticationToken(result.principal(), roleCodes.stream()
                .map(IsassGrantedAuthority::new)
                .toList());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
