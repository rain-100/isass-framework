// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.entrypoint.authorization.EntrypointPermissionResolver;
import vip.isass.framework.web.security.SecurityConst;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 使用当前进程的 Entrypoint—权限映射校验主体授权上下文。
 */
@Component
public class DynamicPermissionAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final IAuthorizationService authorizationService;
    private final Collection<EntrypointPermissionResolver> permissionResolvers;

    public DynamicPermissionAuthorizationManager(
            IAuthorizationService authorizationService,
            ObjectProvider<EntrypointPermissionResolver> permissionResolvers) {
        this.authorizationService = authorizationService;
        this.permissionResolvers = permissionResolvers.orderedStream().toList();
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authenticationSupplier,
                                           RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        if (!(authentication instanceof PrincipalAuthenticationToken token)
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }
        PrincipalAuthorizationContext authorizationContext = context(token);
        Set<String> roles = new LinkedHashSet<>(authorizationContext.roleCodes());
        if (roles.contains(SecurityConst.ROLE_SUPER_DEV)) {
            return new AuthorizationDecision(true);
        }
        Set<String> required = requiredPermissions(
                context.getRequest().getMethod(), context.getRequest().getRequestURI());
        if (required.isEmpty()) {
            return new AuthorizationDecision(false);
        }
        Set<String> granted = new LinkedHashSet<>(authorizationContext.permissionCodes());
        return new AuthorizationDecision(required.stream().anyMatch(granted::contains));
    }

    private PrincipalAuthorizationContext context(PrincipalAuthenticationToken token) {
        PrincipalAuthorizationContext context = token.getAuthorizationContext();
        if (context != null) {
            return context;
        }
        synchronized (token) {
            context = token.getAuthorizationContext();
            if (context == null) {
                if (token.getPrincipal().getPrincipalType() != PrincipalType.USER) {
                    context = PrincipalAuthorizationContext.empty();
                } else {
                    context = authorizationService.jwtContext();
                    if (context == null) context = PrincipalAuthorizationContext.empty();
                }
                token.setAuthorizationContext(context);
            }
            return context;
        }
    }

    private Set<String> requiredPermissions(String method, String path) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (EntrypointPermissionResolver resolver : permissionResolvers) {
            Collection<String> values = resolver.findRequiredPermissionCodes(method, path);
            if (values != null) {
                values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .forEach(result::add);
            }
        }
        return result;
    }
}
