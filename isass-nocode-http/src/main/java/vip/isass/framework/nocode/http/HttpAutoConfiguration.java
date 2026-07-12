package vip.isass.framework.nocode.http;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.ContractResourceLoader;

@AutoConfiguration
public class HttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ContractRegistry ContractRegistry(ServiceRegistry services, ObjectMapper objectMapper) {
        ContractResourceLoader loader = new ContractResourceLoader(
                objectMapper, Thread.currentThread().getContextClassLoader());
        java.util.List<vip.isass.framework.nocode.contract.ServiceContract> generated =
                loader.load().stream().flatMap(document -> document.services().stream()).toList();
        if (generated.isEmpty()) {
            throw new IllegalStateException("Missing generated nocode contract META-INF/isass/nocode-contract.json; "
                    + "run the isass-nocode-generator Maven goal before starting the service");
        }
        return new ContractRegistry(generated);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpServerAdapter HttpServerAdapter(
            ContractRegistry contracts,
            ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        return new HttpServerAdapter(contracts, services, objectMapper);
    }
}
