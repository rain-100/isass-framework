// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import vip.isass.framework.web.security.authentication.apikey.ApiKeyAuthenticationFilter;
import vip.isass.framework.web.security.authentication.jwt.JwtAuthenticationFilter;
import vip.isass.framework.web.security.authentication.multilogin.ShouldOfflineChecker;
import vip.isass.framework.web.security.authorization.DynamicPermissionAuthorizationManager;
import vip.isass.framework.web.security.authorization.BusinessOrInternalAuthenticatedAuthorizationManager;
import vip.isass.framework.web.security.authentication.internal.InternalHmacAuthenticationFilter;
import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;
import vip.isass.framework.web.security.EntrypointAuthenticatedUrlProvider;

import java.util.Collection;
import java.util.List;

/**
 * @author Rain
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Resource
    private PermitUrlConfiguration permitUrlConfiguration;

    @Resource
    private ShouldOfflineChecker shouldOfflineChecker;

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private DynamicPermissionAuthorizationManager dynamicPermissionAuthorizationManager;

    @Resource
    private BusinessOrInternalAuthenticatedAuthorizationManager authenticatedAuthorizationManager;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager,
            ObjectProvider<EntrypointAuthenticatedUrlProvider> authenticatedUrlProvider) throws Exception {
        UrlAccessSecurityStrategy urlAccessSecurityStrategy = securityProperties.getUrlAccessSecurityStrategy();
        log.info("urlAccessSecurityStrategy: {}", urlAccessSecurityStrategy);

        Collection<String> permitUrls = permitUrlConfiguration.getPermitUrls();
        EntrypointAuthenticatedUrlProvider entrypointAuthenticatedUrls = authenticatedUrlProvider.getIfAvailable();
        Collection<String> authenticatedUrls = entrypointAuthenticatedUrls == null
                ? List.of()
                : entrypointAuthenticatedUrls.getUrls();
        http
                // 允许跨域
                .cors(cors -> {
                })

                // 基于jwt，无需预防CSRF攻击
                .csrf(AbstractHttpConfigurer::disable)

                // 基于jwt，无需session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 禁用缓存
                .headers(headers -> headers.cacheControl(HeadersConfigurer.CacheControlConfig::disable)
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                // HSTS is now handled via ServerHttpSecurity or configuration properties in SB4

                // jwt 校验过滤器
                .addFilter(new JwtAuthenticationFilter(authenticationManager, shouldOfflineChecker))

                // API Key 应用主体认证过滤器
                .addFilter(new ApiKeyAuthenticationFilter(authenticationManager))

                // 内部 HMAC 服务主体认证过滤器；保留已经解析的 JWT/API Key 业务主体
                .addFilterBefore(new InternalHmacAuthenticationFilter(securityProperties), AuthorizationFilter.class)

                // 允许匿名机制
                .anonymous(_ -> {
                });

        if (urlAccessSecurityStrategy == UrlAccessSecurityStrategy.NONE) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else if (urlAccessSecurityStrategy == UrlAccessSecurityStrategy.ROLE) {
            http.authorizeHttpRequests(auth -> {
                auth.requestMatchers(permitUrls.toArray(new String[0])).permitAll();
                if (!authenticatedUrls.isEmpty()) {
                    auth.requestMatchers(authenticatedUrls.toArray(new String[0]))
                            .access(authenticatedAuthorizationManager);
                }
                auth.anyRequest().access(dynamicPermissionAuthorizationManager);
            });
        } else {
            http.authorizeHttpRequests(auth -> {
                auth.requestMatchers(permitUrls.toArray(new String[0])).permitAll();
                auth.anyRequest().access(authenticatedAuthorizationManager);
            });
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
        return new ProviderManager(authenticationProviders);
    }

}
