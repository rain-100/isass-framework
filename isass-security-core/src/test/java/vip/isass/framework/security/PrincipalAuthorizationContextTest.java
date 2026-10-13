// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.support.JsonUtil;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrincipalAuthorizationContextTest {

    @Test
    void copiesAuthorizationCollectionsAndKeepsThemImmutable() {
        List<String> roleCodes = new ArrayList<>(List.of("ROLE_LOGIN"));
        List<String> permissions = new ArrayList<>(List.of("bsp.auth.user.view"));
        PrincipalAuthorizationContext context = new PrincipalAuthorizationContext(
                roleCodes, permissions, 1L, null);

        roleCodes.clear();
        permissions.clear();

        assertEquals(List.of("ROLE_LOGIN"), context.roleCodes());
        assertEquals(List.of("bsp.auth.user.view"), context.permissionCodes());
        assertThrows(UnsupportedOperationException.class, () -> context.roleCodes().clear());
        assertThrows(UnsupportedOperationException.class, () -> context.permissionCodes().clear());
    }

    @Test
    void nullPermissionsDistinguishUnloadedFromLoadedEmptyPermissions() {
        PrincipalAuthorizationContext context = new PrincipalAuthorizationContext(null, null, 1L, null);

        assertTrue(context.roleCodes().isEmpty());
        assertNull(context.permissionCodes());
        assertFalse(context.permissionsLoaded());
        assertTrue(PrincipalAuthorizationContext.empty().permissionsLoaded());
        assertTrue(PrincipalAuthorizationContext.empty().permissionCodes().isEmpty());
        assertEquals(context, JsonUtil.readValue(JsonUtil.writeValue(context), PrincipalAuthorizationContext.class));
    }

    @Test
    void preservesRemoteAuthorizationPayloadThroughJsonRoundTrip() {
        PrincipalAuthorizationContext context = new PrincipalAuthorizationContext(
                List.of("ROLE_LOGIN"), List.of("bsp.auth.user.view"), 7L, 123456789L);

        String json = JsonUtil.writeValue(context);
        PrincipalAuthorizationContext restored = JsonUtil.readValue(
                json, PrincipalAuthorizationContext.class);

        assertEquals(context, restored);
        assertFalse(json.contains("\"roles\""));
        assertFalse(json.contains("\"name\""));
        assertFalse(json.contains("\"permissionsLoaded\""));
    }
}
