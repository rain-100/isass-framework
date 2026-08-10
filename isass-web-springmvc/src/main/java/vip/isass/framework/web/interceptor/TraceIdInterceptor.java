// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.interceptor;

import org.apache.skywalking.apm.toolkit.trace.TraceContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Rain
 */
public class TraceIdInterceptor implements IsassHandlerInterceptor {

    public static final String HEADER_NAME = "isass-trace-id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.addHeader(HEADER_NAME, TraceContext.traceId());
        return true;
    }

}
