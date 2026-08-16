// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import vip.isass.framework.web.security.SecurityConst;
import vip.isass.framework.web.security.metadata.SecurityMetadataSourceProviderManager;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

/** Applies manually configured role resources to resolved Spring MVC endpoints. */
@Component
public class DynamicRoleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final SecurityMetadataSourceProviderManager metadataSourceProviderManager;

    public DynamicRoleAuthorizationManager(SecurityMetadataSourceProviderManager metadataSourceProviderManager) {
        this.metadataSourceProviderManager = metadataSourceProviderManager;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authenticationSupplier,
                                           RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }
        String resourceUri = resolveResourceUri(context.getRequest());
        if (resourceUri == null) return new AuthorizationDecision(false);
        Set<String> granted = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toSet());
        if (granted.contains(SecurityConst.ROLE_SUPER_DEV)) {
            return new AuthorizationDecision(true);
        }
        Collection<String> requiredRoles = metadataSourceProviderManager.findRoleCodesByUri(resourceUri);
        if (requiredRoles == null || requiredRoles.isEmpty()) return new AuthorizationDecision(false);
        boolean allowed = requiredRoles.stream().anyMatch(granted::contains);
        return new AuthorizationDecision(allowed);
    }

    private String resolveResourceUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri == null || requestUri.isBlank() ? null
                : request.getMethod().toUpperCase() + " " + requestUri;
    }
}
