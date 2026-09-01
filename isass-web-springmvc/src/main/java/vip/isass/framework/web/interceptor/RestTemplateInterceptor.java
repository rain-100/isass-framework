// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderContext;

import java.io.IOException;
import java.util.List;

/**
 * @author Rain
 */
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final List<AdditionalRequestHeaderProvider> additionalHeaderProviders;

    public RestTemplateInterceptor(List<AdditionalRequestHeaderProvider> additionalHeaderProviders) {
        this.additionalHeaderProviders = additionalHeaderProviders;
    }

    /**
     * todo 如果访问的url是微服务集群内部服务，添加头信息
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (additionalHeaderProviders == null) {
            return execution.execute(request, body);
        }

        HttpHeaders headers = request.getHeaders();
        AdditionalRequestHeaderContext context = new AdditionalRequestHeaderContext(
                request.getMethod().name(), request.getURI(), body);
        additionalHeaderProviders.forEach(provider -> {
            provider.getHeaders(context).forEach((name, value) -> {
                if (provider.override() || headers.getFirst(name) == null) {
                    headers.set(name, value);
                }
            });
        });
        return execution.execute(request, body);
    }

}
