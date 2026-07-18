package vip.isass.framework.nocode.http;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.ContractResourceLoader;

@AutoConfiguration
@EnableConfigurationProperties(HttpEndpointProperties.class)
public class HttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HttpEndpointResolver HttpEndpointResolver(HttpEndpointProperties properties) {
        return service -> properties.getEndpoints().get(service);
    }

    @Bean
    @ConditionalOnMissingBean
    public NocodeHttpExchange NocodeHttpExchange() {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(RestClient.create()))
                .build().createClient(NocodeHttpExchange.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpClientTransport HttpClientTransport(
            NocodeHttpExchange exchange,
            HttpEndpointResolver endpoints,
            ContractRegistry contracts,
            ObjectMapper objectMapper
    ) {
        return new HttpClientTransport(exchange, endpoints, contracts, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpRemoteTransportProvider HttpRemoteTransportProvider(
            HttpEndpointResolver endpoints,
            HttpClientTransport transport
    ) {
        return new HttpRemoteTransportProvider(endpoints, transport);
    }

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
