// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApiKeyAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesTheAuthenticatedContextAsTheOnlyBusinessRoleSource() throws Exception {
        PrincipalAuthorizationContext context = new PrincipalAuthorizationContext(
                List.of("ROLE_SERVICE"), List.of("demo.view"), 1L, null);
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.APPLICATION).setPrincipalId(10L);
        AuthenticationManager manager = authentication -> new ApiKeyAuthenticationToken(principal, context);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/application");
        request.addHeader(ApiKeyAuthenticationFilter.HEADER_NAME, "isass_sk_public_secret");
        MockFilterChain chain = new MockFilterChain();

        new ApiKeyAuthenticationFilter(manager).doFilter(request, new MockHttpServletResponse(), chain);

        PrincipalAuthenticationToken token = (PrincipalAuthenticationToken)
                SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(chain.getRequest());
        assertSame(context, token.getAuthorizationContext());
        assertEquals("ROLE_SERVICE", token.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void recognizedInvalidApiKeyStopsTheFilterChainWithUnauthorized() throws Exception {
        AuthenticationManager authenticationManager = authentication -> {
            throw new BadCredentialsException("invalid");
        };
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(authenticationManager);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/asset-service/sample/task/page");
        request.addHeader(ApiKeyAuthenticationFilter.HEADER_NAME, "isass_sk_identifier_secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void duplicateApiKeyRepresentationsAreRejectedBeforeAuthentication() throws Exception {
        AuthenticationManager authenticationManager = authentication -> {
            throw new AssertionError("重复凭证不应进入认证器");
        };
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(authenticationManager);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/asset-service/sample/task/page");
        request.addHeader(ApiKeyAuthenticationFilter.HEADER_NAME, "isass_sk_identifier_secret");
        request.addHeader("Authorization", "Bearer isass_sk_identifier_secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void infrastructureFailureClearsContextAndStopsTheRequestWithServiceUnavailable() throws Exception {
        AuthenticationManager manager = authentication -> {
            throw new AuthenticationServiceException("transport unavailable");
        };
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/application");
        request.addHeader(ApiKeyAuthenticationFilter.HEADER_NAME, "isass_sk_public_secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new ApiKeyAuthenticationFilter(manager).doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
