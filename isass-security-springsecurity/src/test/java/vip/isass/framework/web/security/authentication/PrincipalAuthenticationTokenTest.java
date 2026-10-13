// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication;

import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrincipalAuthenticationTokenTest {

    @Test
    void exposesOnlyContextRolesAsStandardSpringAuthorities() {
        PrincipalAuthorizationContext context = new PrincipalAuthorizationContext(
                List.of("ROLE_LOGIN", "ROLE_MANAGER", "ROLE_LOGIN"), List.of("bsp.auth.user.update"), 1L, null);
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(principal(), context);

        assertSame(context, token.getAuthorizationContext());
        assertEquals(List.of("ROLE_LOGIN", "ROLE_MANAGER"), token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
        assertTrue(token.getAuthorities().stream().allMatch(SimpleGrantedAuthority.class::isInstance));
        assertThrows(UnsupportedOperationException.class, () -> token.getAuthorities().clear());
    }

    @Test
    void standardSpringRoleCheckTracksContextReplacement() {
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(principal());
        AuthorityAuthorizationManager<Object> manager = AuthorityAuthorizationManager.hasRole("MANAGER");
        assertFalse(manager.authorize(() -> token, new Object()).isGranted());

        PrincipalAuthorizationContext context = new PrincipalAuthorizationContext(
                List.of("ROLE_MANAGER"), List.of("bsp.auth.user.update"), 1L, null);
        token.setAuthorizationContext(context);
        assertTrue(manager.authorize(() -> token, new Object()).isGranted());

        token.setAuthorizationContext(new PrincipalAuthorizationContext(
                List.of("ROLE_LOGIN"), List.of("bsp.auth.user.view"), 2L, null));
        assertFalse(manager.authorize(() -> token, new Object()).isGranted());
        assertEquals(List.of("ROLE_LOGIN"), token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
        assertEquals(List.of("bsp.auth.user.view"), token.getAuthorizationContext().permissionCodes());
    }

    @Test
    void getterDoesNotLoadMissingAuthorizationContext() {
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(principal());

        assertTrue(token.getAuthorities().isEmpty());
        assertTrue(token.getAuthorities().isEmpty());
        assertNull(token.getAuthorizationContext());

        token.setAuthorizationContext(PrincipalAuthorizationContext.empty());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void clearingContextAlsoClearsDerivedAuthorities() {
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(principal(),
                new PrincipalAuthorizationContext(List.of("ROLE_MANAGER"), List.of(), 1L, null));

        token.setAuthorizationContext(null);

        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void internalOnlyAuthenticationDoesNotGainBusinessRoles() {
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(null,
                new InternalServicePrincipal("bsp-service", "key", "request"), null);

        assertTrue(token.isAuthenticated());
        assertTrue(token.hasInternalServicePrincipal());
        assertFalse(token.hasBusinessPrincipal());
        assertTrue(token.getAuthorities().isEmpty());
    }

    @Test
    void rejectsAuthenticationWithoutAnyPrincipal() {
        assertThrows(IllegalArgumentException.class, () -> new PrincipalAuthenticationToken(null));
    }

    private DefaultAuthenticatedPrincipal principal() {
        return new DefaultAuthenticatedPrincipal().setPrincipalType(PrincipalType.USER)
                .setPrincipalId(1L).setTenantId(2L).setAppId(3L);
    }
}
