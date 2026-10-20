// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import vip.isass.framework.common.criteria.CriteriaEntityTypes;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.http.HttpEndpointResolver;
import vip.isass.framework.entrypoint.registry.EntrypointClassifier;
import vip.isass.framework.entrypoint.registry.EntrypointInvocationAuthorizer;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.initialization.NocodeInitializationController;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService;
import vip.isass.framework.nocode.initialization.NocodeInitializationImporter;
import vip.isass.framework.nocode.initialization.NocodeInitializationProperties;
import vip.isass.framework.nocode.initialization.NocodeInitializationRemoteClient;
import vip.isass.framework.nocode.initialization.NocodeInitializationRunner;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleListener;
import vip.isass.framework.nocode.lifecycle.CrudWriteLifecycleListener;
import vip.isass.framework.nocode.query.CriteriaQueryParamConverter;
import vip.isass.framework.nocode.security.NocodeAuthorizationContext;
import vip.isass.framework.nocode.security.NocodePermissionEvaluator;
import vip.isass.framework.nocode.security.TenantReadLifecycleListener;
import vip.isass.framework.nocode.security.TenantReadPolicyProvider;
import vip.isass.framework.nocode.service.AssociationQueryCoordinator;
import vip.isass.framework.nocode.service.AssociationWriteCoordinator;
import vip.isass.framework.nocode.service.CrudQueryExecutor;
import vip.isass.framework.nocode.service.CrudQueryExecutorProvider;
import vip.isass.framework.nocode.service.CrudWriteExecutor;
import vip.isass.framework.nocode.service.CrudWriteExecutorProvider;
import vip.isass.framework.nocode.service.ICrudService;
import vip.isass.framework.nocode.service.ILocalCrudService;
import vip.isass.framework.nocode.service.ITreeQueryService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@AutoConfiguration
@EnableConfigurationProperties(NocodeInitializationProperties.class)
public class NocodeAutoConfiguration {

    @Bean
    public TenantReadLifecycleListener tenantReadLifecycleListener(ObjectProvider<TenantReadPolicyProvider> providers) {
        return new TenantReadLifecycleListener(providers.orderedStream().toList());
    }

    @Bean
    public CriteriaQueryParamConverter criteriaQueryParamConverter() {
        return new CriteriaQueryParamConverter();
    }

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
            List<ILocalCrudService<?, ?, ?>> services) {
        return new AssociationQueryCoordinator(services);
    }

    @Bean
    public AssociationWriteCoordinator associationWriteCoordinator(
            List<ILocalCrudService<?, ?, ?>> services,
            ObjectProvider<NocodePermissionEvaluator> evaluators) {
        return new AssociationWriteCoordinator(services, (service, request) -> {
            EntrypointInfo info = Arrays.stream(ClassUtils.getAllInterfacesForClass(service.getClass()))
                    .map(contract -> contract.getAnnotation(EntrypointInfo.class))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("关联服务缺少 EntrypointInfo"));
            evaluators.getObject().check(new NocodeAuthorizationContext(info.serviceName(),
                    info.contextName(), info.resourceName(), "superCud", List.of(request)));
        });
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
        return (serviceInterface, operationMethod) -> {
            if (ICrudService.class.isAssignableFrom(serviceInterface)) {
                Class<?> criteria = ResolvableType.forClass(serviceInterface).as(ICrudService.class).getGeneric(1).resolve();
                if (criteria != null) CriteriaEntityTypes.register(criteria);
            }
            return (ICrudService.class.isAssignableFrom(serviceInterface)
                        && operationMethod.getDeclaringClass() == ICrudService.class)
                || (ITreeQueryService.class.isAssignableFrom(serviceInterface)
                        && operationMethod.getDeclaringClass() == ITreeQueryService.class);
        };
    }

    @Bean
    @ConditionalOnMissingBean(NocodePermissionEvaluator.class)
    public NocodePermissionEvaluator nocodePermissionEvaluator() {
        return NocodePermissionEvaluator.ALLOW_ALL;
    }

    @Bean
    public EntrypointInvocationAuthorizer nocodeInvocationAuthorizer(
            ObjectProvider<NocodePermissionEvaluator> permissionEvaluators
    ) {
        return (service, operation, arguments) -> {
            if (operation.nocode()) {
                // Registration must not initialize evaluators that themselves depend on the registry.
                permissionEvaluators.getObject().check(new NocodeAuthorizationContext(
                        service.serviceName(),
                        service.contextName(),
                        service.resourceName(),
                        operation.operationName(),
                        Arrays.asList(arguments)));
            }
        };
    }

    @Bean
    public NocodeInitializationDataService nocodeInitializationDataService(
            List<ILocalCrudService<?, ?, ?>> services) {
        return new NocodeInitializationDataService(services);
    }

    @Bean
    public NocodeInitializationRemoteClient nocodeInitializationRemoteClient(
            HttpEndpointResolver endpoints,
            ObjectProvider<AdditionalRequestHeaderProvider> headers) {
        return new NocodeInitializationRemoteClient(endpoints, headers.orderedStream().toList());
    }

    @Bean
    public NocodeInitializationImporter nocodeInitializationImporter(
            NocodeInitializationDataService dataService,
            NocodeInitializationRemoteClient remoteClient,
            ServiceDefinitionRegistry definitions) {
        return new NocodeInitializationImporter(dataService, remoteClient, definitions);
    }

    @Bean
    public NocodeInitializationController nocodeInitializationController(
            NocodeInitializationDataService dataService,
            NocodeInitializationImporter importer) {
        return new NocodeInitializationController(dataService, importer);
    }

    @Bean
    public ApplicationRunner nocodeInitializationRunner(
            NocodeInitializationDataService dataService,
            NocodeInitializationImporter importer,
            NocodeInitializationProperties properties) {
        return new NocodeInitializationRunner(dataService, importer, properties).runner();
    }
}
