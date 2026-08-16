// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import vip.isass.framework.entrypoint.registry.EntrypointClassifier;
import vip.isass.framework.nocode.service.CrudChangeExecutor;
import vip.isass.framework.nocode.service.CrudChangeExecutorProvider;
import vip.isass.framework.nocode.service.ICrudService;
import vip.isass.framework.nocode.service.ILocalCrudService;
import vip.isass.framework.nocode.service.AssociationQueryCoordinator;
import vip.isass.framework.nocode.service.AssociationQueryCoordinatorProvider;
import vip.isass.framework.nocode.service.AssociationWriteCoordinator;
import vip.isass.framework.nocode.security.NocodeAuthorizationContext;
import vip.isass.framework.nocode.security.NocodePermissionEvaluator;
import vip.isass.framework.nocode.initialization.NocodeInitializationController;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService;
import vip.isass.framework.nocode.initialization.NocodeInitializationProperties;

@org.springframework.boot.autoconfigure.AutoConfiguration
@EnableConfigurationProperties(NocodeInitializationProperties.class)
public class AutoConfiguration {

    @Bean
    public CrudChangeExecutor crudChangeExecutor(
            AssociationWriteCoordinator associations,
            ObjectProvider<PlatformTransactionManager> transactionManager
    ) {
        return new CrudChangeExecutor(associations, transactionManager.getIfAvailable());
    }

    @Bean
    public CrudChangeExecutorProvider crudChangeExecutorProvider(CrudChangeExecutor executor) {
        return new CrudChangeExecutorProvider(executor);
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
    public AssociationQueryCoordinatorProvider associationQueryCoordinatorProvider(
            AssociationQueryCoordinator coordinator) {
        return new AssociationQueryCoordinatorProvider(coordinator);
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
                        service.serviceName(), service.resourceName(), operation.operationName(),
                        java.util.List.of(arguments)));
            }
        };
    }

    @Bean
    public NocodeInitializationDataService nocodeInitializationDataService(
            java.util.List<ILocalCrudService<?, ?, ?>> services,
            tools.jackson.databind.ObjectMapper objectMapper) {
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
            vip.isass.framework.entrypoint.http.HttpEndpointResolver endpoints,
            tools.jackson.databind.ObjectMapper objectMapper,
            ObjectProvider<vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider> headers,
            NocodeInitializationProperties properties,
            vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry definitions) {
        var remote = new vip.isass.framework.nocode.initialization.NocodeInitializationRemoteClient(
                endpoints, objectMapper, headers.orderedStream().toList());
        return new vip.isass.framework.nocode.initialization.NocodeInitializationRunner(
                dataService, remote, properties, definitions).runner();
    }
}
