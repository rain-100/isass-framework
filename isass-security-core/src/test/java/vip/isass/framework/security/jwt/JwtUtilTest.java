// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.security.jwt;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {
    private static final String SECRET = "test-signing-key-with-at-least-64-bytes-of-entropy-for-security-tests";

    @Test
    void preservesIdentityScopeAndExpiryWithoutAnAuthorizationPayload() {
        long expiry = System.currentTimeMillis() + 60_000;
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER).setPrincipalId(1L).setPrincipalName("tester")
                .setTenantId(2L).setAppId(3L).setLoginLogId(4L).setAuthenticationExpireAt(expiry);

        JwtInfo restored = JwtUtil.parse(JwtUtil.generateToken(principal, SECRET), SECRET);

        assertEquals(1L, restored.getUid());
        assertEquals(2L, restored.getTid());
        assertEquals(3L, restored.getAid());
        assertEquals(4L, restored.getLid());
        assertEquals("tester", restored.getName());
        assertEquals(expiry, restored.getEat());
    }

    @Test
    void rejectsExpiredOrIncorrectlySignedCredentials() {
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER).setPrincipalId(1L);
        String token = JwtUtil.generateToken(principal, SECRET);
        assertThrows(UnifiedException.class, () -> JwtUtil.parse(token, SECRET + "different"));

        principal.setAuthenticationExpireAt(System.currentTimeMillis() - 60_000);
        String expired = JwtUtil.generateToken(principal, SECRET);
        assertThrows(UnifiedException.class, () -> JwtUtil.parse(expired, SECRET));
    }
}
