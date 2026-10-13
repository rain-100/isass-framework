// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.web.security.authorization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.web.security.CurrentPrincipalServiceImpl;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PrincipalAuthorizationContextResolverTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void explicitlyLoadsRolesThenPermissionsOnceAndGettersNeverPerformIo() {
        PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
        PrincipalAuthenticationToken token = token();
        PrincipalAuthorizationContext roles = PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null);
        PrincipalAuthorizationContext permissions = new PrincipalAuthorizationContext(
                List.of("ROLE_LOGIN"), List.of("bsp.auth.user.view"), 2L, null);
        when(loader.loadRoles(any())).thenReturn(roles);
        when(loader.load(any())).thenReturn(permissions);
        CurrentPrincipalServiceImpl current = new CurrentPrincipalServiceImpl(loader);

        assertNull(current.getAuthorizationContext());
        token.getAuthorities();
        verifyNoInteractions(loader);
        assertSame(roles, current.loadRoleContext());
        assertSame(roles, current.loadRoleContext());
        verify(loader, never()).load(any());
        assertSame(permissions, current.loadAuthorizationContext());
        assertSame(permissions, current.loadAuthorizationContext());
        assertSame(permissions, current.loadRoleContext());
        current.cacheAuthorizationContext(roles);
        assertSame(permissions, current.getAuthorizationContext());
        verify(loader).loadRoles(any());
        verify(loader).load(any());
    }

    @Test
    void loadedEmptyPermissionsAreNotQueriedAgain() {
        PrincipalAuthenticationToken token = token();
        token.setAuthorizationContext(PrincipalAuthorizationContext.empty());
        PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
        assertSame(token.getAuthorizationContext(), PrincipalAuthorizationContextResolver.permissions(token, loader));
        verifyNoInteractions(loader);
    }

    @Test
    void nullRolesResultClearsAuthenticationAndNeverBecomesAnEmptyGrant() {
        PrincipalAuthenticationToken token = token();
        assertThrows(AuthenticationServiceException.class,
                () -> PrincipalAuthorizationContextResolver.roles(token, mock(PrincipalAuthorizationLoader.class)));
        assertNull(token.getAuthorizationContext());
        assertFalse(token.isAuthenticated());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void roleOnlyResultCannotSatisfyFullPermissionLoad() {
        PrincipalAuthenticationToken token = token();
        PrincipalAuthorizationContext roles = PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null);
        token.setAuthorizationContext(roles);
        PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
        when(loader.load(any())).thenReturn(roles);
        assertThrows(AuthenticationServiceException.class,
                () -> PrincipalAuthorizationContextResolver.permissions(token, loader));
        assertNull(token.getAuthorizationContext());
        assertFalse(token.isAuthenticated());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void failedUpgradeDoesNotLeaveAnAuthenticatedOnlyFallback() {
        PrincipalAuthenticationToken token = token();
        token.setAuthorizationContext(PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null));
        PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
        when(loader.load(any())).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(AuthenticationServiceException.class,
                () -> PrincipalAuthorizationContextResolver.permissions(token, loader));
        assertNull(token.getAuthorizationContext());
        assertFalse(token.isAuthenticated());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void concurrentPermissionLoadsResolveOnlyOnce() throws Exception {
        PrincipalAuthenticationToken token = token();
        PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
        PrincipalAuthorizationContext full = new PrincipalAuthorizationContext(
                List.of("ROLE_LOGIN"), List.of("sample.view"), 1L, null);
        when(loader.load(any())).thenReturn(full);
        Callable<PrincipalAuthorizationContext> task =
                () -> PrincipalAuthorizationContextResolver.permissions(token, loader);
        try (var executor = Executors.newFixedThreadPool(4)) {
            for (var future : executor.invokeAll(Collections.nCopies(16, task))) {
                assertSame(full, future.get());
            }
        }
        verify(loader).load(any());
    }

    @Test
    void invalidatedTokenCannotReuseExistingGrantsOrReload() {
        PrincipalAuthenticationToken token = token();
        token.setAuthorizationContext(PrincipalAuthorizationContext.empty());
        token.setAuthenticated(false);
        PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> PrincipalAuthorizationContextResolver.roles(token, loader));
        verifyNoInteractions(loader);
    }

    private PrincipalAuthenticationToken token() {
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER).setPrincipalId(1L).setTenantId(2L).setAppId(3L));
        SecurityContextHolder.getContext().setAuthentication(token);
        return token;
    }
}
