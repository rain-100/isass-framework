package vip.isass.framework.nocode.grpc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3ContractResourceLoader;

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
        if (generated.isEmpty()) {
            throw new IllegalStateException("Missing generated V3 contract META-INF/isass/v3-contract.json; "
                    + "run the isass-nocode-generator Maven goal before starting the service");
        }
        return new V3ContractRegistry(generated);
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
            V3GrpcInvocationHandler invocationHandler,
            ObjectMapper objectMapper
    ) {
        return new V3GrpcServerAdapter(contracts.contracts(), invocationHandler, objectMapper);
    }
}
