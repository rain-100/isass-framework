package vip.isass.framework.nocode.grpc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3ContractResourceLoader;
import vip.isass.framework.nocode.v3.contract.V3RuntimeContractFactory;

@AutoConfiguration
public class V3GrpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public V3ContractRegistry v3GrpcContractRegistry(
            V3ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        var generated = new V3ContractResourceLoader(
                objectMapper, Thread.currentThread().getContextClassLoader())
                .load().stream().flatMap(document -> document.services().stream()).toList();
        return generated.isEmpty()
                ? V3RuntimeContractFactory.from(services)
                : new V3ContractRegistry(generated);
    }

    @Bean
    @ConditionalOnMissingBean
    public V3GrpcInvocationHandler v3GrpcInvocationHandler(
            V3ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        return new V3GrpcLocalInvocationHandler(services, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public V3GrpcServerAdapter v3GrpcServerAdapter(
            V3ContractRegistry contracts,
            V3GrpcInvocationHandler invocationHandler
    ) {
        return new V3GrpcServerAdapter(contracts.contracts(), invocationHandler);
    }
}
