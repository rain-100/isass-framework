// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringApiKeyHeaderProviderTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsInboundApiKeyOnlyForApplicationPrincipalAndInternalEndpoint() {
        SpringApiKeyHeaderProvider provider = provider();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ApiKeyAuthenticationFilter.HEADER_NAME, "isass_sk_identifier_secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(applicationToken());

        assertTrue(provider.support("POST", "https://bsp.internal:31010/bsp-service/auth/bootstrap/register"));
        assertEquals("isass_sk_identifier_secret", provider.getValue());
        assertFalse(provider.support("POST", "https://storage.example/object"));
    }

    @Test
    void doesNotInventApiKeyForBackgroundInternalCall() {
        SpringApiKeyHeaderProvider provider = provider();
        assertFalse(provider.support("GET",
                "https://bsp.internal:31010/bsp-service/config/parameter/getCodeValuesByKey"));
    }

    private SpringApiKeyHeaderProvider provider() {
        return new SpringApiKeyHeaderProvider(new MockEnvironment().withProperty(
                "isass.entrypoint.http.services.bsp-service.url", "https://bsp.internal:31010"),
                new StaticListableBeanFactory());
    }

    private PrincipalAuthenticationToken applicationToken() {
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.APPLICATION).setPrincipalId(1L);
        return new PrincipalAuthenticationToken(principal, List.of());
    }
}
