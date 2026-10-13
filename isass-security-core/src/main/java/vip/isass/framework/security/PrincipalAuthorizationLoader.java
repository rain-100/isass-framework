// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.security;

/**
 * Loads authorization for an already authenticated business principal and its tenant/application scope.
 * Implementations must return a non-null context or fail; an empty context is a valid unprivileged result.
 */
@FunctionalInterface
public interface PrincipalAuthorizationLoader {
    PrincipalAuthorizationContext load(AuthenticatedPrincipal principal);

    /** Load roles without permissions. Providers that support staging should override this method. */
    default PrincipalAuthorizationContext loadRoles(AuthenticatedPrincipal principal) {
        return load(principal);
    }
}
