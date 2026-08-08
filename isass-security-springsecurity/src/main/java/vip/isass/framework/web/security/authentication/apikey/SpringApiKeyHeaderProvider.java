package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;

/** 为框架主动发起的跨服务调用附加当前微服务的 API Key。 */
@Component
public class SpringApiKeyHeaderProvider implements AdditionalRequestHeaderProvider {

    private final ServiceAccountProperties properties;
    private final InternalServiceEndpointMatcher endpointMatcher;

    public SpringApiKeyHeaderProvider(ServiceAccountProperties properties, Environment environment,
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
        return properties.enabled() && endpointMatcher.matches(url);
    }
}
