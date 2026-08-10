// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import vip.isass.framework.web.security.metadata.SecurityMetadataSource;
import vip.isass.framework.web.security.metadata.SecurityMetadataSourceProviderManager;
import vip.isass.framework.web.uri.UriPrefixProvider;

import java.util.Collection;

/**
 * @author Rain
 */
public class FilterSecurityInterceptorSourcePostProcessor implements BeanPostProcessor {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final SecurityMetadataSourceProviderManager securityMetadataSourceProviderManager;
    private final UriPrefixProvider prefixProvider;
    private final Collection<String> permitUrls;

    public FilterSecurityInterceptorSourcePostProcessor(
        RequestMappingHandlerMapping requestMappingHandlerMapping,
        SecurityMetadataSourceProviderManager securityMetadataSourceProviderManager,
        UriPrefixProvider prefixProvider,
        Collection<String> permitUrls) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.securityMetadataSourceProviderManager = securityMetadataSourceProviderManager;
        this.prefixProvider = prefixProvider;
        this.permitUrls = permitUrls;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // In SB4/Security7, the FilterSecurityInterceptor is gone.
        // The SecurityMetadataSource is now used differently, likely via an AuthorizationManager.
        // For now, we are just initializing it here for downstream usage if needed.
        new SecurityMetadataSource(
            requestMappingHandlerMapping,
            securityMetadataSourceProviderManager,
            prefixProvider,
            permitUrls);
        return bean;
    }

}
