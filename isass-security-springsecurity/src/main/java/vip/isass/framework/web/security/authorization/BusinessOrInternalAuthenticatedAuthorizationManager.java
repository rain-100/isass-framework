// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.authorization.internal.InternalAccessRegistry;

import java.util.function.Supplier;

/** AUTHENTICATED 策略：业务主体存在，或内部主体存在且入口明确开放。 */
@Component
public final class BusinessOrInternalAuthenticatedAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final InternalAccessRegistry internalAccessRegistry;

    public BusinessOrInternalAuthenticatedAuthorizationManager(InternalAccessRegistry internalAccessRegistry) {
        this.internalAccessRegistry = internalAccessRegistry;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
                                           RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (!(current instanceof PrincipalAuthenticationToken token) || !current.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        boolean business = token.hasBusinessPrincipal();
        boolean internal = token.hasInternalServicePrincipal()
                && internalAccessRegistry.isAllowed(context.getRequest());
        return new AuthorizationDecision(business || internal);
    }
}
