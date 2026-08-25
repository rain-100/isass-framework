// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.web.security.authorization.ApiKeyAuthenticationRequest;
import vip.isass.framework.web.security.authorization.ApiKeyAuthenticationResult;
import vip.isass.framework.web.security.authorization.IAuthorizationService;
import vip.isass.framework.web.security.authorization.PrincipalAuthorizationContext;
import vip.isass.framework.entrypoint.transport.EntrypointRemoteBusinessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyAuthenticationProviderTest {

    @Test
    void authenticatesApplicationPrincipalAndRoles() {
        IAuthorizationService service = mock(IAuthorizationService.class);
        when(service.apiKeyContext(new ApiKeyAuthenticationRequest("isass_sk_public_secret")))
                .thenReturn(new ApiKeyAuthenticationResult(
                        new DefaultAuthenticatedPrincipal().setPrincipalType(PrincipalType.APPLICATION).setPrincipalId(10L),
                        new PrincipalAuthorizationContext(List.of("ROLE_SERVICE"), List.of("demo.view"), 1L, null)));

        ApiKeyAuthenticationToken result = (ApiKeyAuthenticationToken) new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret"));

        assertEquals(10L, result.getPrincipal().getPrincipalId());
        assertEquals("ROLE_SERVICE", result.getAuthorities().iterator().next().getAuthority());
        assertEquals(List.of("demo.view"), result.getAuthorizationContext().permissionCodes());
    }

    @Test
    void rejectsWhenAuthorizationServiceReturnsNoPrincipal() {
        IAuthorizationService service = mock(IAuthorizationService.class);

        assertThrows(BadCredentialsException.class, () -> new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret")));
    }

    @Test
    void mapsRemoteAuthenticationBusinessFailureToBadCredentials() {
        IAuthorizationService service = mock(IAuthorizationService.class);
        ApiKeyAuthenticationRequest request = new ApiKeyAuthenticationRequest("isass_sk_public_secret");
        when(service.apiKeyContext(request)).thenThrow(
                new EntrypointRemoteBusinessException("API Key 无效或已失效"));

        assertThrows(BadCredentialsException.class, () -> new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret")));
    }
}
