// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.config.SecurityProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalHmacAuthenticationFilterTest {

    private static final String KEY_ID = "internal-key";
    private static final String SECRET = "internal-secret";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesInternalServicePreservesBodyAndMergesBusinessPrincipal() throws Exception {
        DefaultAuthenticatedPrincipal businessPrincipal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER).setPrincipalId(1L).setTenantId(2L).setAppId(3L);
        SecurityContextHolder.getContext().setAuthentication(
                new PrincipalAuthenticationToken(businessPrincipal, List.of()));

        byte[] body = "{\"value\":1}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = request(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter().doFilter(request, response, (effectiveRequest, ignored) -> {
            invoked.set(true);
            assertEquals("{\"value\":1}", new String(
                    effectiveRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            PrincipalAuthenticationToken token = (PrincipalAuthenticationToken)
                    SecurityContextHolder.getContext().getAuthentication();
            assertEquals(businessPrincipal, token.getPrincipal());
            assertNotNull(token.getInternalServicePrincipal());
            assertEquals("asset-service", token.getInternalServicePrincipal().serviceName());
            assertEquals("request-1", token.getInternalServicePrincipal().requestId());
        });

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsBodyChangedAfterSigning() throws Exception {
        byte[] signedBody = "{\"value\":1}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = request(signedBody);
        request.setContent("{\"value\":2}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter().doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertEquals(401, response.getStatus());
        assertTrue(!invoked.get());
    }

    @Test
    void doesNotRewriteDownstreamAuthorizationFailureAsHmacAuthenticationFailure() {
        MockHttpServletRequest request = request(new byte[0]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> filter().doFilter(request, response,
                        (ignoredRequest, ignoredResponse) -> {
                            throw new IllegalStateException("downstream denied");
                        }));

        assertEquals("downstream denied", exception.getMessage());
        assertEquals(200, response.getStatus());
    }

    private InternalHmacAuthenticationFilter filter() {
        SecurityProperties properties = new SecurityProperties();
        properties.getInternal().setHmacKeyId(KEY_ID);
        properties.getInternal().setHmacSecret(SECRET);
        return new InternalHmacAuthenticationFilter(properties);
    }

    private MockHttpServletRequest request(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/bsp-service/auth/bootstrap/register");
        request.setQueryString("b=2&a=1");
        request.setContent(body);
        long timestamp = System.currentTimeMillis();
        String contentSha256 = InternalHmacUtil.contentSha256(body);
        String signature = InternalHmacUtil.signature(
                "asset-service", KEY_ID, timestamp, "request-1", request.getMethod(),
                request.getRequestURI(), request.getQueryString(), contentSha256, SECRET);
        request.addHeader(InternalHmacHeaders.SERVICE, "asset-service");
        request.addHeader(InternalHmacHeaders.KEY_ID, KEY_ID);
        request.addHeader(InternalHmacHeaders.TIMESTAMP, String.valueOf(timestamp));
        request.addHeader(InternalHmacHeaders.REQUEST_ID, "request-1");
        request.addHeader(InternalHmacHeaders.CONTENT_SHA256, contentSha256);
        request.addHeader(InternalHmacHeaders.SIGNATURE, signature);
        return request;
    }
}
