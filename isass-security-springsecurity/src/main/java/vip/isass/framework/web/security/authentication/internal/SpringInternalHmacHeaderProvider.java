// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.internal;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderContext;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.web.security.authentication.apikey.InternalServiceEndpointMatcher;
import vip.isass.framework.web.security.config.SecurityProperties;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 为所有 Entrypoint 内部 HTTP 调用附加统一 HMAC 身份。 */
@Component
public class SpringInternalHmacHeaderProvider implements AdditionalRequestHeaderProvider {

    private final SecurityProperties.Internal properties;
    private final InternalServiceEndpointMatcher endpointMatcher;
    private final String serviceName;

    public SpringInternalHmacHeaderProvider(SecurityProperties securityProperties,
                                            Environment environment,
                                            ListableBeanFactory beanFactory) {
        this.properties = securityProperties.getInternal();
        this.endpointMatcher = new InternalServiceEndpointMatcher(environment, beanFactory);
        this.serviceName = environment.getProperty("spring.application.name", "unknown-service");
    }

    @Override
    public String getHeaderName() {
        return InternalHmacHeaders.SIGNATURE;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public boolean override() {
        return true;
    }

    @Override
    public boolean support(String method, String uri) {
        return properties.enabled() && endpointMatcher.matches(uri);
    }

    @Override
    public Map<String, String> getHeaders(AdditionalRequestHeaderContext context) {
        if (!support(context.method(), context.uri().toString())) return Map.of();
        long timestamp = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        String contentSha256 = context.unsignedPayload()
                ? InternalHmacHeaders.UNSIGNED_PAYLOAD
                : InternalHmacUtil.contentSha256(context.body());
        String signature = InternalHmacUtil.signature(serviceName, properties.getHmacKeyId(), timestamp,
                requestId, context.method(), context.uri().getRawPath(), context.uri().getRawQuery(),
                contentSha256, properties.getHmacSecret());
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put(InternalHmacHeaders.SERVICE, serviceName);
        headers.put(InternalHmacHeaders.KEY_ID, properties.getHmacKeyId());
        headers.put(InternalHmacHeaders.TIMESTAMP, String.valueOf(timestamp));
        headers.put(InternalHmacHeaders.REQUEST_ID, requestId);
        headers.put(InternalHmacHeaders.CONTENT_SHA256, contentSha256);
        headers.put(InternalHmacHeaders.SIGNATURE, signature);
        return Map.copyOf(headers);
    }
}
