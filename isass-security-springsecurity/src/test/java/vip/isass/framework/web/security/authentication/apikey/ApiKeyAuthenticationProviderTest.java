// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticationRejectedException;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticationRequest;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticationResult;
import vip.isass.framework.security.authentication.apikey.ApiKeyAuthenticator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyAuthenticationProviderTest {

    @Test
    void authenticatesApplicationPrincipalAndRoles() {
        ApiKeyAuthenticator service = mock(ApiKeyAuthenticator.class);
        when(service.authenticate(new ApiKeyAuthenticationRequest("isass_sk_public_secret")))
                .thenReturn(new ApiKeyAuthenticationResult(
                        new DefaultAuthenticatedPrincipal().setPrincipalType(PrincipalType.APPLICATION).setPrincipalId(10L),
                        new PrincipalAuthorizationContext(List.of("ROLE_SERVICE"), List.of("demo.view"), 1L, null)));

        ApiKeyAuthenticationToken result = (ApiKeyAuthenticationToken) new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret"));

        assertEquals(10L, result.getPrincipal().getPrincipalId());
        assertEquals("ROLE_SERVICE", result.getAuthorities().iterator().next().getAuthority());
        assertInstanceOf(SimpleGrantedAuthority.class, result.getAuthorities().iterator().next());
        assertEquals(List.of("demo.view"), result.getAuthorizationContext().permissionCodes());
    }

    @Test
    void rejectsWhenAuthorizationServiceReturnsNoPrincipal() {
        ApiKeyAuthenticator service = mock(ApiKeyAuthenticator.class);

        assertThrows(BadCredentialsException.class, () -> new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret")));
    }

    @Test
    void mapsRemoteAuthenticationBusinessFailureToBadCredentials() {
        ApiKeyAuthenticator service = mock(ApiKeyAuthenticator.class);
        ApiKeyAuthenticationRequest request = new ApiKeyAuthenticationRequest("isass_sk_public_secret");
        when(service.authenticate(request)).thenThrow(
                new ApiKeyAuthenticationRejectedException(new IllegalArgumentException("invalid")));

        assertThrows(BadCredentialsException.class, () -> new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret")));
    }

    @Test
    void missingContextAndInfrastructureFailureCannotBecomeAnAuthenticatedApplication() {
        ApiKeyAuthenticationRequest request = new ApiKeyAuthenticationRequest("isass_sk_public_secret");
        ApiKeyAuthenticator service = mock(ApiKeyAuthenticator.class);
        when(service.authenticate(request)).thenReturn(new ApiKeyAuthenticationResult(
                new DefaultAuthenticatedPrincipal().setPrincipalType(PrincipalType.APPLICATION).setPrincipalId(10L),
                null));
        assertThrows(AuthenticationServiceException.class, () -> new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken(request.apiKey())));

        when(service.authenticate(request)).thenThrow(new IllegalStateException("transport unavailable"));
        assertThrows(AuthenticationServiceException.class, () -> new ApiKeyAuthenticationProvider(service)
                .authenticate(new ApiKeyAuthenticationToken(request.apiKey())));
    }
}
