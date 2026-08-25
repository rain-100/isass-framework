// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiKeyAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
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
}
