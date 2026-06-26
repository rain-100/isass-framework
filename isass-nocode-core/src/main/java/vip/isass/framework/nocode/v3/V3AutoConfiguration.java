package vip.isass.framework.nocode.v3;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.nocode.v3.controller.V3Controller;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.util.List;

@AutoConfiguration
@ConditionalOnBean(IV3Service.class)
public class V3AutoConfiguration {

    @Bean
    V3ServiceRegistry v3ServiceRegistry(List<IV3Service<?, ?>> services) {
        return new V3ServiceRegistry(services);
    }

    @Bean
    V3CriteriaMapper v3CriteriaMapper() {
        return new V3CriteriaMapper();
    }

    @Bean
    V3TableMetaRegistrar v3TableMetaRegistrar(V3ServiceRegistry serviceRegistry) {
        return new V3TableMetaRegistrar(serviceRegistry);
    }

    @Bean
    V3Controller v3Controller(V3ServiceRegistry serviceRegistry,
                               V3CriteriaMapper criteriaMapper) {
        return new V3Controller(serviceRegistry, criteriaMapper);
    }
}
