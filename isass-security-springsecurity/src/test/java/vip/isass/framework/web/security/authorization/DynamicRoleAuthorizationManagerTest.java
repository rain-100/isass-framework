// SPDX-License-Identifier: LGPL-3.0-only

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
        when(metadata.findRoleCodesByUri("POST /bsp-service/nocode/system/initialization/importData")).thenReturn(List.of());
        DynamicRoleAuthorizationManager manager = new DynamicRoleAuthorizationManager(metadata);

        assertFalse(manager.authorize(this::authenticated,
                new RequestAuthorizationContext(request("POST", "/bsp-service/nocode/system/initialization/importData"))).isGranted());
    }

    @Test
    void allowsSuperDeveloperWithoutRegisteredResourceRoles() {
        SecurityMetadataSourceProviderManager metadata = mock(SecurityMetadataSourceProviderManager.class);
        DynamicRoleAuthorizationManager manager = new DynamicRoleAuthorizationManager(metadata);

        assertTrue(manager.authorize(() -> new UsernamePasswordAuthenticationToken("admin", "", List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_DEV"))),
                new RequestAuthorizationContext(request("GET", "/bsp-service/auth/app/getResourceTree"))).isGranted());
    }

    @Test
    void allowsMatchingRoleForActualRequestUri() {
        SecurityMetadataSourceProviderManager metadata = mock(SecurityMetadataSourceProviderManager.class);
        when(metadata.findRoleCodesByUri("POST /bsp-service/nocode/system/initialization/importData"))
                .thenReturn(List.of("ROLE_IIMAGE_ASSET_SERVICE_APP_ADMIN"));
        DynamicRoleAuthorizationManager manager = new DynamicRoleAuthorizationManager(metadata);

        assertTrue(manager.authorize(() -> new UsernamePasswordAuthenticationToken("asset", "", List.of(
                        new SimpleGrantedAuthority("ROLE_IIMAGE_ASSET_SERVICE_APP_ADMIN"))),
                new RequestAuthorizationContext(request("POST", "/bsp-service/nocode/system/initialization/importData"))).isGranted());
    }

    private UsernamePasswordAuthenticationToken authenticated() {
        return new UsernamePasswordAuthenticationToken("user", "", List.of(new SimpleGrantedAuthority("ROLE_LOGIN")));
    }

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }
}
