// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.jwt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.web.security.exception.SecurityCoreStatusEnum;
import vip.isass.framework.common.security.jwt.JwtInfo;
import vip.isass.framework.common.security.jwt.JwtUtil;
import vip.isass.framework.web.security.IsassGrantedAuthority;
import vip.isass.framework.web.security.metadata.SecurityMetadataSourceProviderManager;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
@Slf4j
@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    @Value("${isass.security.jwt.secret:" + JwtUtil.DEFAULT_SECRET + "}")
    private String secret;

    @Resource
    private SecurityMetadataSourceProviderManager securityMetadataSourceProviderManager;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();

        JwtInfo jwtInfo;
        try {
            jwtInfo = JwtUtil.parse(token, secret);
        } catch (UnifiedException e) {
            if (SecurityCoreStatusEnum.TOKEN_EXPIRED.getStatus().equals(e.getStatus())) {
                throw new CredentialsExpiredException(e.getMsg());
            }
            throw new BadCredentialsException(e.getMsg());
        } catch (Exception e) {
            throw new BadCredentialsException(e.getMessage());
        }

        // 获取用户拥有的角色
        Collection<GrantedAuthority> configAttributes = Collections.emptyList();
        Collection<String> roleCodes = securityMetadataSourceProviderManager.findRoleCodesByUserId(String.valueOf(jwtInfo.getUid()));
        if (!CollUtil.isEmpty(roleCodes)) {
            configAttributes = roleCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(IsassGrantedAuthority::new)
                .collect(Collectors.toList());
        }

        return new JwtAuthenticationToken(token, jwtInfo, configAttributes);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(JwtAuthenticationToken.class);
    }
}
