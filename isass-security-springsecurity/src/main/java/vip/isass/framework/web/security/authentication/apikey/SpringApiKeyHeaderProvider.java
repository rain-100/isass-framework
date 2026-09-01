// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.beans.factory.ListableBeanFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

/**
 * 在应用主体触发的跨微服务调用中透传原 API Key。
 */
@Component
public class SpringApiKeyHeaderProvider implements AdditionalRequestHeaderProvider {

    private final InternalServiceEndpointMatcher endpointMatcher;

    public SpringApiKeyHeaderProvider(Environment environment,
                                      ListableBeanFactory beanFactory) {
        this.endpointMatcher = new InternalServiceEndpointMatcher(environment, beanFactory);
    }

    @Override
    public String getHeaderName() {
        return ApiKeyAuthenticationFilter.HEADER_NAME;
    }

    @Override
    public String getValue() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String value = request.getHeader(ApiKeyAuthenticationFilter.HEADER_NAME);
        if (value != null && !value.isBlank()) return value;
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.regionMatches(true, 0,
                "Bearer isass_sk_", 0, "Bearer isass_sk_".length())
                ? authorization.substring("Bearer ".length()).trim() : null;
    }

    @Override
    public boolean override() {
        return false;
    }

    @Override
    public boolean support(String method, String url) {
        if (!endpointMatcher.matches(url)) {
            return false;
        }
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof PrincipalAuthenticationToken token)
                || token.getPrincipal() == null
                || token.getPrincipal().getPrincipalType() != PrincipalType.APPLICATION) {
            return false;
        }
        return getValue() != null;
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }
}
