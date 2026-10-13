// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.web.security.authentication.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.jwt.JwtInfo;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.authentication.multilogin.ShouldOfflineChecker;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishesIdentityBeforeSuccessCallbackWithoutLoadingAuthorization() throws Exception {
        ShouldOfflineChecker offline = mock(ShouldOfflineChecker.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(manager(), offline) {
            @Override
            protected void onSuccessfulAuthentication(HttpServletRequest request,
                    HttpServletResponse response, Authentication authentication) {
                PrincipalAuthenticationToken token = assertInstanceOf(PrincipalAuthenticationToken.class,
                        SecurityContextHolder.getContext().getAuthentication());
                assertSame(token, authentication);
                assertEquals(1L, token.getPrincipal().getPrincipalId());
                assertEquals(2L, token.getPrincipal().getTenantId());
                assertEquals(3L, token.getPrincipal().getAppId());
                assertNull(token.getAuthorizationContext());
                assertEquals(List.of(), token.getAuthorities());
            }
        };
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(), response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        verify(offline).checkShouldOffline(any());
    }

    @Test
    void publicAndAuthenticatedRequestsDoNotNeedAnAuthorizationProvider() throws Exception {
        for (String path : List.of("/public", "/authenticated")) {
            SecurityContextHolder.clearContext();
            MockHttpServletRequest request = request();
            request.setRequestURI(path);
            MockFilterChain chain = new MockFilterChain();
            filter().doFilter(request, new MockHttpServletResponse(), chain);
            assertNotNull(chain.getRequest());
            assertNull(((PrincipalAuthenticationToken) SecurityContextHolder.getContext()
                    .getAuthentication()).getAuthorizationContext());
        }
    }

    @Test
    void forcedOfflineStillRejectsIdentityBeforeBusinessExecution() throws Exception {
        ShouldOfflineChecker offline = mock(ShouldOfflineChecker.class);
        doThrow(new BadCredentialsException("offline")).when(offline).checkShouldOffline(any());
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(manager(), offline).doFilter(request(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidJwtStopsRequest() throws Exception {
        AuthenticationManager manager = authentication -> {
            throw new BadCredentialsException("invalid");
        };
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthenticationFilter(manager, mock(ShouldOfflineChecker.class)).doFilter(request(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void skipsAnonymousRequestsAndBearerApiKeys() throws Exception {
        MockFilterChain anonymous = new MockFilterChain();
        filter().doFilter(new MockHttpServletRequest("GET", "/public"), new MockHttpServletResponse(), anonymous);
        MockHttpServletRequest apiKeyRequest = new MockHttpServletRequest("GET", "/application");
        apiKeyRequest.addHeader(JwtConst.HEADER_NAME, "Bearer isass_sk_public_secret");
        MockFilterChain apiKey = new MockFilterChain();
        filter().doFilter(apiKeyRequest, new MockHttpServletResponse(), apiKey);
        assertNotNull(anonymous.getRequest());
        assertNotNull(apiKey.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void preservesExistingInternalIdentityWithoutBusinessAuthorization() throws Exception {
        InternalServicePrincipal internal = new InternalServicePrincipal("asset-service", "key", "request");
        SecurityContextHolder.getContext().setAuthentication(new PrincipalAuthenticationToken(null, internal, null));
        filter().doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());
        PrincipalAuthenticationToken token = (PrincipalAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertSame(internal, token.getInternalServicePrincipal());
        assertNull(token.getAuthorizationContext());
    }

    @Test
    void downstreamFailuresAreNotRewrittenAsAuthenticationFailures() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(IllegalStateException.class, () -> filter().doFilter(request(), response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new IllegalStateException("downstream");
                }));
        assertEquals(200, response.getStatus());
    }

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(manager(), mock(ShouldOfflineChecker.class));
    }

    private AuthenticationManager manager() {
        JwtInfo claims = mock(JwtInfo.class);
        when(claims.getUid()).thenReturn(1L);
        when(claims.getTid()).thenReturn(2L);
        when(claims.getAid()).thenReturn(3L);
        return authentication -> new JwtAuthenticationToken("jwt", claims);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bsp-service/auth/user/page");
        request.addHeader(JwtConst.HEADER_NAME, JwtConst.PREFIX + "jwt");
        return request;
    }
}
