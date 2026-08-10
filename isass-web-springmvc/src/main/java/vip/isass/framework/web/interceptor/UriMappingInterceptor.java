// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.interceptor;

import org.springframework.web.servlet.HandlerMapping;
import vip.isass.framework.common.support.UriRequestMapping;
import vip.isass.framework.web.uri.UriPrefixProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Rain
 */
public class UriMappingInterceptor implements IsassHandlerInterceptor {

    private final UriPrefixProvider uriPrefixProvider;

    public UriMappingInterceptor(UriPrefixProvider uriPrefixProvider) {
        this.uriPrefixProvider = uriPrefixProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String mapping = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        response.addHeader(
            UriRequestMapping.MAPPING_KEY,
            request.getMethod().toUpperCase() + " " + uriPrefixProvider.getUriPrefix() + mapping);
        return true;
    }

}
