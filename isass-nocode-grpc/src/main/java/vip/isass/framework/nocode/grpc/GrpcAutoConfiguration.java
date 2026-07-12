package vip.isass.framework.nocode.grpc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.ContractResourceLoader;

@AutoConfiguration
public class GrpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ContractRegistry GrpcContractRegistry(
            ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        var generated = new ContractResourceLoader(
                objectMapper, Thread.currentThread().getContextClassLoader())
                .load().stream().flatMap(document -> document.services().stream()).toList();
        if (generated.isEmpty()) {
            throw new IllegalStateException("Missing generated nocode contract META-INF/isass/nocode-contract.json; "
                    + "run the isass-nocode-generator Maven goal before starting the service");
        }
        return new ContractRegistry(generated);
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcInvocationHandler GrpcInvocationHandler(
            ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        return new GrpcLocalInvocationHandler(services, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcServerAdapter GrpcServerAdapter(
            ContractRegistry contracts,
            GrpcInvocationHandler invocationHandler,
            ObjectMapper objectMapper
    ) {
        return new GrpcServerAdapter(contracts.contracts(), invocationHandler, objectMapper);
    }
}
