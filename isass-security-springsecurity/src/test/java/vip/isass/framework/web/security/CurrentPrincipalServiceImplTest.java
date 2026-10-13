// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.web.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import vip.isass.framework.security.CurrentPrincipalUtil;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CurrentPrincipalServiceImplTest {
    private final CurrentPrincipalServiceImpl service = new CurrentPrincipalServiceImpl();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        CurrentPrincipalUtil.setCurrentPrincipalServiceProvider(null);
    }

    @Test
    void coreSpiReadsLoadedContextAndBothIdentitiesWithoutLoadingAuthorization() {
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER).setPrincipalId(1L);
        InternalServicePrincipal internal = new InternalServicePrincipal("asset-service", "key", "request");
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(principal, internal, null);
        SecurityContextHolder.getContext().setAuthentication(token);
        CurrentPrincipalUtil.setCurrentPrincipalService(service);

        assertSame(principal, CurrentPrincipalUtil.getPrincipal());
        assertSame(internal, CurrentPrincipalUtil.getInternalServicePrincipal());
        assertNull(CurrentPrincipalUtil.getAuthorizationContext());
        PrincipalAuthorizationContext context = PrincipalAuthorizationContext.empty();
        token.setAuthorizationContext(context);
        assertSame(context, CurrentPrincipalUtil.getAuthorizationContext());
        SecurityContextHolder.clearContext();
        assertNull(CurrentPrincipalUtil.getAuthorizationContext());
    }
}
