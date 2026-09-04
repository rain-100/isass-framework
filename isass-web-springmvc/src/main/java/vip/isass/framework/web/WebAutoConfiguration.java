// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.web.config.ObjectMapperConfiguration;
import vip.isass.framework.web.config.WebConfig;
import vip.isass.framework.web.exception.ExceptionAdvice;
import vip.isass.framework.web.exception.IsassErrorController;
import vip.isass.framework.web.exception.WebStatusMapping;
import vip.isass.framework.web.response.AdvancedFeatureResponseAdvice;
import vip.isass.framework.web.interceptor.RestTemplateInterceptor;
import vip.isass.framework.web.interceptor.TraceIdInterceptor;
import vip.isass.framework.web.interceptor.UriMappingInterceptor;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.web.security.EntrypointAnonymousUrlProvider;
import vip.isass.framework.web.security.EntrypointAuthenticatedUrlProvider;
import vip.isass.framework.web.uri.UriPrefixProvider;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@AutoConfiguration
@Import({
        ObjectMapperConfiguration.class,
        WebConfig.class,
        IsassErrorController.class,
        ExceptionAdvice.class,
        UriPrefixProvider.class
})
public class WebAutoConfiguration {

    public static final int READ_TIMEOUT_IN_MILLIS = 50_000;

    @Bean
    public EntrypointAnonymousUrlProvider entrypointAnonymousUrlProvider(
            ServiceDefinitionRegistry serviceDefinitionRegistry
    ) {
        return new EntrypointAnonymousUrlProvider(serviceDefinitionRegistry);
    }

    @Bean
    public EntrypointAuthenticatedUrlProvider entrypointAuthenticatedUrlProvider(
            ServiceDefinitionRegistry serviceDefinitionRegistry
    ) {
        return new EntrypointAuthenticatedUrlProvider(serviceDefinitionRegistry);
    }

    @Bean
    public RestTemplate noBalancedRestTemplate(RestTemplateInterceptor restTemplateInterceptor) {
        SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
        httpRequestFactory.setReadTimeout(READ_TIMEOUT_IN_MILLIS);

        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        restTemplate.setInterceptors(Collections.singletonList(restTemplateInterceptor));
        return restTemplate;
    }

    @Bean
    public HttpMessageConverter<BufferedImage> bufferedImageHttpMessageConverter() {
        return new BufferedImageHttpMessageConverter();
    }

    @Bean
    public WebStatusMapping webStatusMapping() {
        return new WebStatusMapping();
    }

    @Bean
    @ConditionalOnBean(tools.jackson.databind.ObjectMapper.class)
    public AdvancedFeatureResponseAdvice advancedFeatureResponseAdvice(tools.jackson.databind.ObjectMapper objectMapper) {
        return new AdvancedFeatureResponseAdvice(objectMapper);
    }

    @Bean
    public TraceIdInterceptor traceIdInterceptor() {
        return new TraceIdInterceptor();
    }

    @Bean
    public UriMappingInterceptor uriMappingInterceptor(UriPrefixProvider uriPrefixProvider) {
        return new UriMappingInterceptor(uriPrefixProvider);
    }

    @Bean
    public RestTemplateInterceptor restTemplateInterceptor(
            @Autowired(required = false) List<AdditionalRequestHeaderProvider> additionalHeaderProviders) {
        return new RestTemplateInterceptor(additionalHeaderProviders);
    }

}
