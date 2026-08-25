// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.entrypoint.http.HttpEndpointResolver;
import vip.isass.framework.entrypoint.registry.EntrypointClassifier;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.initialization.NocodeInitializationController;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService;
import vip.isass.framework.nocode.initialization.NocodeInitializationProperties;
import vip.isass.framework.nocode.initialization.NocodeInitializationRemoteClient;
import vip.isass.framework.nocode.initialization.NocodeInitializationRunner;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleListener;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleListener;
import vip.isass.framework.nocode.security.NocodeAuthorizationContext;
import vip.isass.framework.nocode.security.NocodePermissionEvaluator;
import vip.isass.framework.nocode.service.AssociationQueryCoordinator;
import vip.isass.framework.nocode.service.AssociationWriteCoordinator;
import vip.isass.framework.nocode.service.CrudWriteExecutor;
import vip.isass.framework.nocode.service.CrudWriteExecutorProvider;
import vip.isass.framework.nocode.service.CrudQueryExecutor;
import vip.isass.framework.nocode.service.CrudQueryExecutorProvider;
import vip.isass.framework.nocode.service.ICrudService;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(NocodeInitializationProperties.class)
public class NocodeAutoConfiguration {

    @Bean
    public CrudWriteExecutor crudWriteExecutor(
            AssociationWriteCoordinator associations,
            ObjectProvider<PlatformTransactionManager> transactionManager,
            ObjectProvider<CrudWriteLifecycleListener> listeners
    ) {
        return new CrudWriteExecutor(associations, transactionManager.getIfAvailable(),
                listeners.orderedStream().toList());
    }

    @Bean
    public CrudWriteExecutorProvider crudWriteExecutorProvider(CrudWriteExecutor executor) {
        return new CrudWriteExecutorProvider(executor);
    }

    @Bean
    public AssociationQueryCoordinator associationQueryCoordinator(
            java.util.List<ILocalCrudService<?, ?, ?>> services) {
        return new AssociationQueryCoordinator(services);
    }

    @Bean
    public AssociationWriteCoordinator associationWriteCoordinator(
            java.util.List<ILocalCrudService<?, ?, ?>> services) {
        return new AssociationWriteCoordinator(services);
    }

    @Bean
    public CrudQueryExecutor crudQueryExecutor(
            AssociationQueryCoordinator associations,
            ObjectProvider<CrudQueryLifecycleListener> listeners) {
        return new CrudQueryExecutor(associations, listeners.orderedStream().toList());
    }

    @Bean
    public CrudQueryExecutorProvider crudQueryExecutorProvider(CrudQueryExecutor executor) {
        return new CrudQueryExecutorProvider(executor);
    }

    @Bean
    public EntrypointClassifier nocodeEntrypointClassifier() {
        return (serviceInterface, operationMethod) -> ICrudService.class.isAssignableFrom(serviceInterface)
                && operationMethod.getDeclaringClass() == ICrudService.class;
    }

    @Bean
    @ConditionalOnMissingBean(NocodePermissionEvaluator.class)
    public NocodePermissionEvaluator nocodePermissionEvaluator() {
        return NocodePermissionEvaluator.ALLOW_ALL;
    }

    @Bean
    public vip.isass.framework.entrypoint.registry.EntrypointInvocationAuthorizer nocodeInvocationAuthorizer(
            NocodePermissionEvaluator permissionEvaluator
    ) {
        return (service, operation, arguments) -> {
            if (operation.nocode()) {
                permissionEvaluator.check(new NocodeAuthorizationContext(
                        service.serviceName(),
                        service.contextName(),
                        service.resourceName(),
                        operation.operationName(),
                        java.util.List.of(arguments)));
            }
        };
    }

    @Bean
    public NocodeInitializationDataService nocodeInitializationDataService(
            List<ILocalCrudService<?, ?, ?>> services,
            ObjectMapper objectMapper) {
        return new NocodeInitializationDataService(services, objectMapper);
    }

    @Bean
    public NocodeInitializationController nocodeInitializationController(
            NocodeInitializationDataService dataService) {
        return new NocodeInitializationController(dataService);
    }

    @Bean
    public org.springframework.boot.ApplicationRunner nocodeInitializationRunner(
            NocodeInitializationDataService dataService,
            HttpEndpointResolver endpoints,
            ObjectMapper objectMapper,
            ObjectProvider<AdditionalRequestHeaderProvider> headers,
            NocodeInitializationProperties properties,
            ServiceDefinitionRegistry definitions) {
        var remote = new NocodeInitializationRemoteClient(
                endpoints, objectMapper, headers.orderedStream().toList());
        return new NocodeInitializationRunner(dataService, remote, properties, definitions).runner();
    }
}
