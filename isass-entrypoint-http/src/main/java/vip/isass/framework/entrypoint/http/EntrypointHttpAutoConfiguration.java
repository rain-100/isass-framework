// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.http;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.entrypoint.QueryParamConverter;
import vip.isass.framework.entrypoint.registry.EntrypointInvocationGateway;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.util.List;

@AutoConfiguration
public class EntrypointHttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HttpEndpointResolver httpEndpointResolver(Environment environment, ListableBeanFactory beanFactory) {
        return new DefaultHttpEndpointResolver(environment, beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public EntrypointHttpTransport entrypointHttpTransport(
            HttpEndpointResolver endpoints,
            ObjectProvider<AdditionalRequestHeaderProvider> headerProviders,
            ObjectProvider<QueryParamConverter> queryConverters
    ) {
        return new EntrypointHttpTransport(RestClient.create(), endpoints,
                headerProviders.orderedStream().toList(), queryConverters.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public EntrypointHttpServer entrypointHttpServer(
            ServiceDefinitionRegistry definitions,
            EntrypointInvocationGateway invocations,
            ObjectProvider<QueryParamConverter> queryConverters
    ) {
        return new EntrypointHttpServer(definitions, invocations, queryConverters.orderedStream().toList());
    }
}
