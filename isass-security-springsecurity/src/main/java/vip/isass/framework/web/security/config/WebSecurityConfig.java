// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import vip.isass.framework.web.security.authentication.jwt.JwtAuthenticationFilter;
import vip.isass.framework.web.security.authentication.apikey.ApiKeyAuthenticationFilter;
import vip.isass.framework.web.security.authentication.multilogin.ShouldOfflineChecker;
import vip.isass.framework.web.security.authorization.DynamicRoleAuthorizationManager;
import vip.isass.framework.web.security.processor.AffirmativeBasedPostProcessor;
import vip.isass.framework.web.security.processor.FilterSecurityInterceptorSourcePostProcessor;

import jakarta.annotation.Resource;
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
    private DynamicRoleAuthorizationManager dynamicRoleAuthorizationManager;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        UrlAccessSecurityStrategy urlAccessSecurityStrategy = securityProperties.getUrlAccessSecurityStrategy();
        log.info("urlAccessSecurityStrategy: {}", urlAccessSecurityStrategy);

        Collection<String> permitUrls = permitUrlConfiguration.getPermitUrls();

        http
                // 允许跨域
                .cors(cors -> {})

                // 基于jwt，无需预防CSRF攻击
                .csrf(csrf -> csrf.disable())

                // 基于jwt，无需session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 禁用缓存
                .headers(headers -> headers.cacheControl(cache -> cache.disable()).frameOptions(frame -> frame.disable()))

                // HSTS is now handled via ServerHttpSecurity or configuration properties in SB4

                // jwt 校验过滤器
                .addFilter(new JwtAuthenticationFilter(authenticationManager, shouldOfflineChecker))

                // API Key 应用主体认证过滤器
                .addFilter(new ApiKeyAuthenticationFilter(authenticationManager))

                // 允许匿名机制
                .anonymous(anonymous -> {});

        if (urlAccessSecurityStrategy == UrlAccessSecurityStrategy.NONE) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else if (urlAccessSecurityStrategy == UrlAccessSecurityStrategy.ROLE) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(permitUrls.toArray(new String[0])).permitAll()
                    .anyRequest().access(dynamicRoleAuthorizationManager)
            );
        } else {
            http.authorizeHttpRequests(auth -> auth
                    // 允许自定义配置的 url 的请求
                    .requestMatchers(permitUrls.toArray(new String[0])).permitAll()
                    .anyRequest().authenticated()
            );
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
        return new ProviderManager(authenticationProviders);
    }

}
