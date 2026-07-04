package vip.isass.framework.nocode.http;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3ContractResourceLoader;
import vip.isass.framework.nocode.v3.contract.V3RuntimeContractFactory;

@AutoConfiguration
public class V3HttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public V3ContractRegistry v3ContractRegistry(V3ServiceRegistry services, ObjectMapper objectMapper) {
        V3ContractResourceLoader loader = new V3ContractResourceLoader(
                objectMapper, Thread.currentThread().getContextClassLoader());
        java.util.List<vip.isass.framework.nocode.v3.contract.V3ServiceContract> generated =
                loader.load().stream().flatMap(document -> document.services().stream()).toList();
        return generated.isEmpty()
                ? V3RuntimeContractFactory.from(services)
                : new V3ContractRegistry(generated);
    }

    @Bean
    @ConditionalOnMissingBean
    public V3HttpServerAdapter v3HttpServerAdapter(
            V3ContractRegistry contracts,
            V3ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        return new V3HttpServerAdapter(contracts, services, objectMapper);
    }
}
