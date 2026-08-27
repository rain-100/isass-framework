// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

/**
 * 为框架主动发起的跨服务调用附加当前微服务的 API Key。
 */
@Component
public class SpringApiKeyHeaderProvider implements AdditionalRequestHeaderProvider {

    private final BootstrapSecurityProperties properties;
    private final InternalServiceEndpointMatcher endpointMatcher;

    public SpringApiKeyHeaderProvider(BootstrapSecurityProperties properties,
                                      Environment environment,
                                      ListableBeanFactory beanFactory) {
        this.properties = properties;
        this.endpointMatcher = new InternalServiceEndpointMatcher(environment, beanFactory);
    }

    @Override
    public String getHeaderName() {
        return ApiKeyAuthenticationFilter.HEADER_NAME;
    }

    @Override
    public String getValue() {
        return properties.getApiKey();
    }

    @Override
    public boolean override() {
        return false;
    }

    @Override
    public boolean support(String method, String url) {
        if (!properties.apiKeyEnabled() || !endpointMatcher.matches(url) || excluded(url)) {
            return false;
        }
        return !(SecurityContextHolder.getContext().getAuthentication() instanceof PrincipalAuthenticationToken token)
                || token.getPrincipal().getPrincipalType() != PrincipalType.USER;
    }

    private boolean excluded(String url) {
        return url.contains("/bsp-service/auth/bootstrap/apiKey")
                || url.contains("/bsp-service/auth/bootstrap/register")
                || url.contains("/bsp-service/auth/authorization/apiKeyContext")
                || url.contains("/bsp-service/auth/authorization/jwtContext");
    }
}
