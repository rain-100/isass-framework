package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.common.security.apikey.ApiKeyAuthenticationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyAuthenticationProviderTest {

    @Test
    void authenticatesApplicationPrincipalAndRoles() {
        ObjectProvider<ApiKeyAuthenticationService> provider = mock(ObjectProvider.class);
        ApiKeyAuthenticationService service = apiKey -> new ApiKeyAuthenticationService.ApiKeyAuthenticationResult(
                new DefaultAuthenticatedPrincipal().setPrincipalType(PrincipalType.APPLICATION).setPrincipalId(10L),
                List.of("ROLE_SERVICE"));
        when(provider.getIfAvailable()).thenReturn(service);

        ApiKeyAuthenticationToken result = (ApiKeyAuthenticationToken) new ApiKeyAuthenticationProvider(provider)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret"));

        assertEquals(10L, result.getPrincipal().getPrincipalId());
        assertEquals("ROLE_SERVICE", result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void rejectsWhenNoBusinessApiKeyAuthenticatorExists() {
        ObjectProvider<ApiKeyAuthenticationService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        assertThrows(BadCredentialsException.class, () -> new ApiKeyAuthenticationProvider(provider)
                .authenticate(new ApiKeyAuthenticationToken("isass_sk_public_secret")));
    }
}
