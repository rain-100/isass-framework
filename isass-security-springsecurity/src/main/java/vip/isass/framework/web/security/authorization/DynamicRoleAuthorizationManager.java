package vip.isass.framework.web.security.authorization;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import vip.isass.framework.web.security.SecurityConst;
import vip.isass.framework.web.security.metadata.SecurityMetadataSourceProviderManager;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Applies manually configured role resources to resolved Spring MVC endpoints. */
@Component
public class DynamicRoleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final RequestMappingHandlerMapping handlerMapping;
    private final SecurityMetadataSourceProviderManager metadataSourceProviderManager;

    public DynamicRoleAuthorizationManager(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
                                           SecurityMetadataSourceProviderManager metadataSourceProviderManager) {
        this.handlerMapping = handlerMapping;
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
        if (resourceUri == null) return new AuthorizationDecision(true);
        Collection<String> requiredRoles = metadataSourceProviderManager.findRoleCodesByUri(resourceUri);
        if (requiredRoles == null || requiredRoles.isEmpty()) return new AuthorizationDecision(true);
        Set<String> granted = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toSet());
        boolean allowed = granted.contains(SecurityConst.ROLE_SUPER_DEV)
                || requiredRoles.stream().anyMatch(granted::contains);
        return new AuthorizationDecision(allowed);
    }

    private String resolveResourceUri(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (!(chain != null && chain.getHandler() instanceof HandlerMethod handler)) return null;
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
                if (!entry.getValue().equals(handler)) continue;
                Set<String> patterns = entry.getKey().getPathPatternsCondition() == null
                        ? entry.getKey().getPatternsCondition().getPatterns()
                        : entry.getKey().getPathPatternsCondition().getPatternValues();
                if (!patterns.isEmpty()) return request.getMethod().toUpperCase() + " " + patterns.iterator().next().trim();
            }
        } catch (Exception ignored) {
            // Unknown MVC mappings are still protected by the authentication rule.
        }
        return null;
    }
}
