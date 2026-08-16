// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.entrypoint.IEntrypoint;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(EntrypointClientProperties.class)
public class EntrypointRegistryAutoConfiguration {

    @Bean
    public static EntrypointProxyRegistrar entrypointProxyRegistrar() {
        return new EntrypointProxyRegistrar();
    }

    @Bean
    public DefaultServiceDefinitionRegistry ServiceDefinitionRegistry(
            List<IEntrypoint> localEntrypoints,
            List<EntrypointClassifier> classifiers,
            List<vip.isass.framework.entrypoint.registry.EntrypointInvocationAuthorizer> authorizers
    ) {
        return new DefaultServiceDefinitionRegistry(localEntrypoints, classifiers, authorizers);
    }
}
