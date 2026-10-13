// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.web.security.authentication;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.security.jwt.JwtInfo;
import vip.isass.framework.web.security.authentication.apikey.ApiKeyAuthenticationFilter;
import vip.isass.framework.web.security.authentication.apikey.ApiKeyAuthenticationToken;
import vip.isass.framework.web.security.authentication.jwt.JwtAuthenticationFilter;
import vip.isass.framework.web.security.authentication.jwt.JwtAuthenticationToken;
import vip.isass.framework.web.security.authentication.jwt.JwtConst;
import vip.isass.framework.web.security.authentication.multilogin.ShouldOfflineChecker;
import vip.isass.framework.web.security.authorization.PrincipalAuthorizationContextResolver;
import vip.isass.framework.web.security.config.SecurityProperties;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthenticationRoleLoggingTest {
    private final PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(AbstractAuthenticationFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void captureLogs() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void clear() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
        SecurityContextHolder.clearContext();
    }

    @Test
    void configurationDefaultsOffAndBindsBothBooleanValues() {
        assertFalse(new SecurityProperties().getAuthentication().isLoadRolesForLogging());
        for (boolean enabled : List.of(true, false)) {
            MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                    "isass.security.authentication.load-roles-for-logging", String.valueOf(enabled)));
            SecurityProperties properties = new Binder(source)
                    .bind("isass.security", Bindable.of(SecurityProperties.class)).get();
            assertEquals(enabled, properties.getAuthentication().isLoadRolesForLogging());
        }
    }

    @Test
    void disabledLoggingDoesNotLoadOrPrintRoles() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter(false).doFilter(request(), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        assertNull(token().getAuthorizationContext());
        assertTrue(successLog().contains("userId[1], name[test]"));
        assertFalse(successLog().contains("拥有角色"));
        verifyNoInteractions(loader);
    }

    @Test
    void enabledLoggingLoadsOnlyRolesAndReusesTheResultForAuthorization() throws Exception {
        PrincipalAuthorizationContext roles = PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null);
        when(loader.loadRoles(any())).thenReturn(roles);
        MockFilterChain chain = new MockFilterChain();
        filter(true).doFilter(request(), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
        assertSame(roles, token().getAuthorizationContext());
        assertNull(roles.permissionCodes());
        assertTrue(successLog().contains("拥有角色[ROLE_LOGIN]"));
        assertSame(roles, PrincipalAuthorizationContextResolver.roles(token(), loader));
        verify(loader).loadRoles(any());
        verify(loader, never()).load(any());
    }

    @Test
    void loadedEmptyRolesArePrintedAsEmptyRatherThanUnloaded() throws Exception {
        when(loader.loadRoles(any())).thenReturn(PrincipalAuthorizationContext.roles(List.of(), 1L, null));
        filter(true).doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(successLog().contains("拥有角色[]"));
        verify(loader, never()).load(any());
    }

    @Test
    void enablingTheFlagLoadsRolesEvenWhenDebugOutputIsDisabled() throws Exception {
        logger.setLevel(Level.INFO);
        when(loader.loadRoles(any())).thenReturn(PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null));
        filter(true).doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());

        verify(loader).loadRoles(any());
        verify(loader, never()).load(any());
        assertTrue(appender.list.isEmpty());
    }

    @Test
    void failedRoleLoadStopsEvenPublicRequestsAndClearsAuthentication() throws Exception {
        when(loader.loadRoles(any())).thenThrow(new IllegalStateException("unavailable"));
        MockHttpServletRequest request = request();
        request.setRequestURI("/public");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter(true).doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertFalse(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("认证成功")));
        verify(loader, never()).load(any());
    }

    @Test
    void anonymousAndInvalidJwtRequestsDoNotLoadRoles() throws Exception {
        filter(true).doFilter(new MockHttpServletRequest("GET", "/public"),
                new MockHttpServletResponse(), new MockFilterChain());
        AuthenticationManager manager = authentication -> {
            throw new BadCredentialsException("invalid");
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(manager, mock(ShouldOfflineChecker.class), loader, true)
                .doFilter(request(), response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        verifyNoInteractions(loader);
    }

    @Test
    void apiKeyLoggingReusesItsExistingContextWithoutCallingJwtAuthorization() throws Exception {
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.APPLICATION).setPrincipalId(1L).setPrincipalName("test");
        PrincipalAuthorizationContext context = new PrincipalAuthorizationContext(
                List.of("ROLE_SERVICE"), List.of("sample.view"), 1L, null);
        AuthenticationManager manager = authentication -> new ApiKeyAuthenticationToken(principal, context);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/application");
        request.addHeader(ApiKeyAuthenticationFilter.HEADER_NAME, "isass_sk_test_secret");
        new ApiKeyAuthenticationFilter(manager, loader, true)
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertSame(context, token().getAuthorizationContext());
        assertTrue(successLog().contains("拥有角色[ROLE_SERVICE]"));
        verifyNoInteractions(loader);
    }

    private JwtAuthenticationFilter filter(boolean enabled) {
        JwtInfo claims = mock(JwtInfo.class);
        when(claims.getUid()).thenReturn(1L);
        when(claims.getName()).thenReturn("test");
        AuthenticationManager manager = authentication -> new JwtAuthenticationToken("jwt", claims);
        return new JwtAuthenticationFilter(manager, mock(ShouldOfflineChecker.class), loader, enabled);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/authenticated");
        request.addHeader(JwtConst.HEADER_NAME, JwtConst.PREFIX + "jwt");
        return request;
    }

    private PrincipalAuthenticationToken token() {
        return (PrincipalAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    }

    private String successLog() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("认证成功")).findFirst().orElseThrow();
    }
}
