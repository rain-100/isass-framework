package vip.isass.framework.nocode.http;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.ContractResourceLoader;
import vip.isass.framework.nocode.security.NocodePermissionEvaluator;

@AutoConfiguration
@EnableConfigurationProperties(NocodeInitializationProperties.class)
public class HttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HttpEndpointResolver HttpEndpointResolver(
            org.springframework.core.env.Environment environment,
            org.springframework.beans.factory.ListableBeanFactory beanFactory
    ) {
        return new DefaultHttpEndpointResolver(new HttpEndpointProperties(environment), beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public NocodeHttpExchange NocodeHttpExchange(
            ObjectProvider<java.util.List<AdditionalRequestHeaderProvider>> additionalHeaderProviders
    ) {
        RestClient.Builder client = RestClient.builder();
        java.util.List<AdditionalRequestHeaderProvider> providers = additionalHeaderProviders.getIfAvailable(java.util.List::of);
        client.requestInterceptor((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            for (AdditionalRequestHeaderProvider provider : providers) {
                if (!provider.support(request.getMethod().name(), request.getURI().toString())) continue;
                if (provider.override() || headers.getFirst(provider.getHeaderName()) == null) {
                    headers.set(provider.getHeaderName(), provider.getValue());
                }
            }
            return execution.execute(request, body);
        });
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client.build()))
                .build().createClient(NocodeHttpExchange.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public NocodePermissionEvaluator nocodePermissionEvaluator() {
        return NocodePermissionEvaluator.ALLOW_ALL;
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
            ObjectMapper objectMapper,
            ObjectProvider<NocodePermissionEvaluator> permissionEvaluator
    ) {
        return new HttpServerAdapter(contracts, services, objectMapper,
                permissionEvaluator.getIfAvailable(() -> NocodePermissionEvaluator.ALLOW_ALL));
    }

    @Bean
    @ConditionalOnMissingBean
    public NocodeInitializationDataService nocodeInitializationDataService(
            ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        return new NocodeInitializationDataService(services, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public NocodeInitializationRemoteClient nocodeInitializationRemoteClient(
            NocodeHttpExchange exchange,
            HttpEndpointResolver endpoints,
            ObjectMapper objectMapper
    ) {
        return new NocodeInitializationRemoteClient(exchange, endpoints, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public NocodeExportService nocodeExportService(
            NocodeInitializationDataService dataService,
            NocodeInitializationRemoteClient remoteClient,
            ObjectMapper objectMapper
    ) {
        return new NocodeExportService(dataService, remoteClient, new NocodeExportProfileLoader(objectMapper), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(name = "nocodeInitializationRunner")
    public org.springframework.boot.ApplicationRunner nocodeInitializationRunner(
            NocodeInitializationDataService dataService,
            NocodeInitializationRemoteClient remoteClient,
            NocodeInitializationProperties properties,
            ContractRegistry contracts
    ) {
        return new NocodeInitializationRunner(dataService, remoteClient, properties, contracts).runner();
    }

    @Bean
    @ConditionalOnMissingBean
    public NocodeInitializationController nocodeInitializationController(
            NocodeInitializationDataService dataService,
            NocodeExportService exportService
    ) {
        return new NocodeInitializationController(dataService, exportService);
    }
}
