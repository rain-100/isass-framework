// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.authentication.apikey.InternalServiceEndpointMatcher;

/**
 * 在用户请求触发的跨微服务 Entrypoint 调用中转发原 JWT，不把完整 JWT保存到主体对象。
 */
@Component
public class SpringJwtHeaderProvider implements AdditionalRequestHeaderProvider {

    private final InternalServiceEndpointMatcher endpointMatcher;

    public SpringJwtHeaderProvider(Environment environment, ListableBeanFactory beanFactory) {
        this.endpointMatcher = new InternalServiceEndpointMatcher(environment, beanFactory);
    }

    @Override
    public String getHeaderName() {
        return JwtConst.HEADER_NAME;
    }

    @Override
    public String getValue() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader(JwtConst.HEADER_NAME);
    }

    @Override
    public boolean override() {
        return false;
    }

    @Override
    public boolean support(String method, String uri) {
        if (!endpointMatcher.matches(uri)) {
            return false;
        }
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof PrincipalAuthenticationToken token)
                || token.getPrincipal() == null
                || token.getPrincipal().getPrincipalType() != PrincipalType.USER) {
            return false;
        }
        String value = getValue();
        return value != null && (value.startsWith(JwtConst.PREFIX)
                || value.startsWith(JwtConst.PREFIX_URL_ENCODED));
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }
}
