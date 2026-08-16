// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.entrypoint.registry.EntrypointInvocationGateway;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

@AutoConfiguration
@EnableConfigurationProperties(EntrypointGrpcProperties.class)
public class EntrypointGrpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EntrypointGrpcTransport entrypointGrpcTransport(
            EntrypointGrpcProperties properties, ObjectMapper objectMapper) {
        return new EntrypointGrpcTransport(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EntrypointGrpcServerAdapter entrypointGrpcServerAdapter(
            ServiceDefinitionRegistry registry,
            EntrypointInvocationGateway invocations,
            ObjectMapper objectMapper) {
        return new EntrypointGrpcServerAdapter(registry, invocations, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EntrypointGrpcServerLifecycle entrypointGrpcServerLifecycle(
            EntrypointGrpcProperties properties, EntrypointGrpcServerAdapter adapter) {
        return new EntrypointGrpcServerLifecycle(properties, adapter);
    }
}
