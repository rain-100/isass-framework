// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.web.security.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vip.isass.framework.entrypoint.authorization.EntrypointPermissionResolver;
import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import vip.isass.framework.security.jwt.JwtInfo;
import vip.isass.framework.web.security.EntrypointAuthenticatedUrlProvider;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.authentication.jwt.JwtConst;
import vip.isass.framework.web.security.authentication.jwt.JwtAuthenticationToken;
import vip.isass.framework.web.security.authentication.multilogin.ShouldOfflineChecker;
import vip.isass.framework.web.security.authorization.internal.InternalAccessRegistry;
import vip.isass.framework.web.security.config.PermitUrlConfiguration;
import vip.isass.framework.web.security.config.SecurityProperties;
import vip.isass.framework.web.security.config.WebSecurityConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LazyAuthorizationFilterChainTest {
    @Test
    void realChainSkipsPublicAndAuthenticatedAuthorizationAndFailsClosedOnProtectedEndpoints() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestConfiguration.class);
            context.refresh();
            FilterChainProxy chain = context.getBean(FilterChainProxy.class);
            PrincipalAuthorizationLoader loader = context.getBean(PrincipalAuthorizationLoader.class);
            assertEquals(200, request(chain, "/public", true, true));
            assertEquals(200, request(chain, "/authenticated", true, true));
            assertEquals(200, request(chain, "/public", false, true));
            assertEquals(401, request(chain, "/authenticated", false, false));
            verifyNoInteractions(loader);

            when(loader.loadRoles(any())).thenReturn(PrincipalAuthorizationContext.roles(List.of("ROLE_SUPER_DEV"), 1L, null));
            assertEquals(200, request(chain, "/protected", true, true));
            verify(loader, never()).load(any());
            reset(loader);

            when(loader.loadRoles(any())).thenReturn(PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null));
            when(loader.load(any())).thenReturn(new PrincipalAuthorizationContext(List.of("ROLE_LOGIN"), List.of("sample.view"), 2L, null));
            assertEquals(200, request(chain, "/protected", true, true));
            verify(loader).loadRoles(any());
            verify(loader).load(any());
            reset(loader);

            when(loader.loadRoles(any())).thenReturn(PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null));
            when(loader.load(any())).thenReturn(PrincipalAuthorizationContext.empty());
            assertEquals(403, request(chain, "/protected", true, false));
            reset(loader);

            when(loader.loadRoles(any())).thenThrow(new IllegalStateException("unavailable"));
            assertEquals(503, request(chain, "/protected", true, false));
            assertEquals(200, request(chain, "/authenticated", true, true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void enabledConfigurationLoadsRolesOnPublicUrlsAndReusesThemOnProtectedUrls() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("role-debug", Map.of(
                    "isass.security.authentication.load-roles-for-logging", "true")));
            context.register(TestConfiguration.class);
            context.refresh();
            FilterChainProxy chain = context.getBean(FilterChainProxy.class);
            PrincipalAuthorizationLoader loader = context.getBean(PrincipalAuthorizationLoader.class);
            PrincipalAuthorizationContext roles = PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null);
            when(loader.loadRoles(any())).thenReturn(roles);
            assertEquals(200, request(chain, "/public", true, true, roles));
            verify(loader).loadRoles(any());
            verify(loader, never()).load(any());

            reset(loader);
            when(loader.loadRoles(any())).thenReturn(roles);
            when(loader.load(any())).thenReturn(new PrincipalAuthorizationContext(
                    List.of("ROLE_LOGIN"), List.of("sample.view"), 2L, null));
            assertEquals(200, request(chain, "/protected", true, true));
            verify(loader).loadRoles(any());
            verify(loader).load(any());
            reset(loader);

            when(loader.loadRoles(any())).thenReturn(PrincipalAuthorizationContext.roles(
                    List.of("ROLE_SUPER_DEV"), 1L, null));
            assertEquals(200, request(chain, "/protected", true, true));
            verify(loader).loadRoles(any());
            verify(loader, never()).load(any());
            reset(loader);

            when(loader.loadRoles(any())).thenThrow(new IllegalStateException("unavailable"));
            assertEquals(503, request(chain, "/public", true, false));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private int request(FilterChainProxy chain, String path, boolean jwt, boolean expectedExecuted) throws Exception {
        return request(chain, path, jwt, expectedExecuted, null);
    }

    private int request(FilterChainProxy chain, String path, boolean jwt, boolean expectedExecuted,
                        PrincipalAuthorizationContext expectedContext) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (jwt) {
            request.addHeader(JwtConst.HEADER_NAME, JwtConst.PREFIX + "jwt");
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean executed = new AtomicBoolean();
        chain.doFilter(request, response, (req, res) -> {
            executed.set(true);
            if (("/public".equals(path) || "/authenticated".equals(path)) && jwt) {
                PrincipalAuthenticationToken token = (PrincipalAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
                assertSame(expectedContext, token.getAuthorizationContext());
            }
        });
        assertEquals(expectedExecuted, executed.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        return response.getStatus();
    }

    @Configuration(proxyBeanMethods = false)
    @Import({WebSecurityConfig.class, DynamicPermissionAuthorizationManager.class,
            BusinessOrInternalAuthenticatedAuthorizationManager.class})
    static class TestConfiguration {
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            return new UrlBasedCorsConfigurationSource();
        }

        @Bean
        SecurityProperties securityProperties(Environment environment) {
            SecurityProperties properties = Binder.get(environment).bind("isass.security", SecurityProperties.class)
                    .orElseGet(SecurityProperties::new);
            properties.setUrlAccessSecurityStrategy(UrlAccessSecurityStrategy.ROLE);
            return properties;
        }

        @Bean
        PermitUrlConfiguration permits() {
            return new PermitUrlConfiguration().setPermitUrls(List.of("/public"));
        }

        @Bean
        ShouldOfflineChecker offline() {
            return mock(ShouldOfflineChecker.class);
        }

        @Bean
        PrincipalAuthorizationLoader authorizationLoader() {
            return mock(PrincipalAuthorizationLoader.class);
        }

        @Bean
        EntrypointPermissionResolver permissions() {
            return (method, path) -> "/protected".equals(path) ? List.of("sample.view") : List.of();
        }

        @Bean
        EntrypointAuthenticatedUrlProvider authenticatedUrls() {
            ServiceDefinitionRegistry definitions = mock(ServiceDefinitionRegistry.class);
            ServiceDefinition service = mock(ServiceDefinition.class);
            OperationDefinition operation = mock(OperationDefinition.class);
            when(service.localImplementation()).thenReturn(true);
            when(service.operations()).thenReturn(List.of(operation));
            when(service.pathPrefix(operation)).thenReturn("");
            when(operation.operationName()).thenReturn("authenticated");
            when(operation.accessStrategy()).thenReturn(UrlAccessSecurityStrategy.AUTHENTICATED);
            when(definitions.all()).thenReturn(List.of(service));
            return new EntrypointAuthenticatedUrlProvider(definitions);
        }

        @Bean
        InternalAccessRegistry internalAccess() {
            return new InternalAccessRegistry(List.of(), mock(ServiceDefinitionRegistry.class));
        }

        @Bean
        AuthenticationProvider jwtProvider() {
            AuthenticationProvider provider = mock(AuthenticationProvider.class);
            when(provider.supports(any())).thenReturn(true);
            JwtInfo claims = mock(JwtInfo.class);
            when(claims.getUid()).thenReturn(1L);
            when(claims.getName()).thenReturn("test");
            when(provider.authenticate(any())).thenAnswer(call -> new JwtAuthenticationToken("jwt", claims));
            return provider;
        }
    }
}
