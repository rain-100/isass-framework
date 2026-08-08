package vip.isass.framework.web.security.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import vip.isass.framework.web.security.metadata.SecurityMetadataSourceProviderManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicRoleAuthorizationManagerTest {

    @Test
    void deniesAuthenticatedRequestWithoutRegisteredResourceRoles() {
        SecurityMetadataSourceProviderManager metadata = mock(SecurityMetadataSourceProviderManager.class);
        when(metadata.findRoleCodesByUri("POST /bsp-service/init-data/import")).thenReturn(List.of());
        DynamicRoleAuthorizationManager manager = new DynamicRoleAuthorizationManager(metadata);

        assertFalse(manager.authorize(this::authenticated,
                new RequestAuthorizationContext(request("POST", "/bsp-service/init-data/import"))).isGranted());
    }

    @Test
    void allowsMatchingRoleForActualRequestUri() {
        SecurityMetadataSourceProviderManager metadata = mock(SecurityMetadataSourceProviderManager.class);
        when(metadata.findRoleCodesByUri("POST /bsp-service/init-data/import"))
                .thenReturn(List.of("ROLE_IIMAGE_ASSET_SERVICE_APP_ADMIN"));
        DynamicRoleAuthorizationManager manager = new DynamicRoleAuthorizationManager(metadata);

        assertTrue(manager.authorize(() -> new UsernamePasswordAuthenticationToken("asset", "", List.of(
                        new SimpleGrantedAuthority("ROLE_IIMAGE_ASSET_SERVICE_APP_ADMIN"))),
                new RequestAuthorizationContext(request("POST", "/bsp-service/init-data/import"))).isGranted());
    }

    private UsernamePasswordAuthenticationToken authenticated() {
        return new UsernamePasswordAuthenticationToken("user", "", List.of(new SimpleGrantedAuthority("ROLE_LOGIN")));
    }

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }
}
