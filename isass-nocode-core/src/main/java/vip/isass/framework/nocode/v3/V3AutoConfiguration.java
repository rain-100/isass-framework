package vip.isass.framework.nocode.v3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.nocode.v3.controller.V3Controller;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.util.List;

@Configuration
public class V3AutoConfiguration {

    @Bean
    public V3ServiceRegistry v3ServiceRegistry(List<IV3Service<?, ?>> services) {
        return new V3ServiceRegistry(services);
    }

    @Bean
    public V3CriteriaMapper v3CriteriaMapper() {
        return new V3CriteriaMapper();
    }

    @Bean
    public V3TableMetaRegistrar v3TableMetaRegistrar(V3ServiceRegistry serviceRegistry) {
        return new V3TableMetaRegistrar(serviceRegistry);
    }
}
