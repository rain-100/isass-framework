// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.DefaultServiceDefinitionRegistry;
import vip.isass.framework.entrypoint.registry.EntrypointInvocationAuthorizer;
import vip.isass.framework.entrypoint.registry.EntrypointRegistryAutoConfiguration;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.security.NocodeAuthorizationContext;
import vip.isass.framework.nocode.security.NocodePermissionEvaluator;
import vip.isass.framework.nocode.service.ICrudService;
import vip.isass.framework.nocode.service.ITreeQueryService;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NocodeAutoConfigurationTest {

    @Test
    void authorizationPreservesNullableCursorArgumentsAndStillEvaluatesPermissions() {
        var operation = new OperationDefinition(
                "cursorPage", "游标分页", "", 0, HttpMethod.GET, null, null,
                List.of(), Object.class, true);
        var service = new ServiceDefinition(
                "test-service", "sample", "sampleGroup", CustomEntrypoint.class,
                List.of(operation), true);
        Object criteria = new Object();
        Object[] arguments = {criteria, null, null};
        var captured = new AtomicReference<NocodeAuthorizationContext>();
        var configuration = new NocodeAutoConfiguration();
        configuration.nocodeInvocationAuthorizer(evaluatorProvider(captured::set))
                .check(service, operation, arguments);
        assertEquals(Arrays.asList(criteria, null, null), captured.get().arguments());
        assertThrows(SecurityException.class,
                () -> configuration.nocodeInvocationAuthorizer(evaluatorProvider(context -> {
                    throw new SecurityException("denied");
                })).check(service, operation, arguments));
    }

    @Test
    void evaluatorIsResolvedOnlyWhenInvokingANocodeOperationAndDenialsStillPropagate() {
        var creations = new AtomicInteger();
        var beans = new DefaultListableBeanFactory();
        var definition = new RootBeanDefinition(NocodePermissionEvaluator.class);
        definition.setInstanceSupplier(() -> {
            creations.incrementAndGet();
            return (NocodePermissionEvaluator) context -> {
                throw new SecurityException("denied");
            };
        });
        beans.registerBeanDefinition("permissionEvaluator", definition);
        var authorizer = new NocodeAutoConfiguration()
                .nocodeInvocationAuthorizer(beans.getBeanProvider(NocodePermissionEvaluator.class));
        var operation = new OperationDefinition(
                "publish", "发布", "", 0, HttpMethod.POST, null, null,
                List.of(), Object.class, false);
        var nocode = new OperationDefinition(
                "page", "分页", "", 0, HttpMethod.GET, null, null,
                List.of(), Object.class, true);
        var service = new ServiceDefinition(
                "test-service", "sample", "sampleGroup", CustomEntrypoint.class,
                List.of(operation, nocode), true);

        assertEquals(0, creations.get());
        authorizer.check(service, operation, new Object[0]);
        assertEquals(0, creations.get());
        assertThrows(SecurityException.class, () -> authorizer.check(service, nocode, new Object[0]));
        assertEquals(1, creations.get());
    }

    @Test
    void registryAndRegistryBackedEvaluatorStartWithoutAllowingCircularReferences() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getDefaultListableBeanFactory().setAllowCircularReferences(false);
            context.register(RegistryAuthorizationConfiguration.class);
            context.refresh();

            assertNotNull(context.getBean(ServiceDefinitionRegistry.class));
            assertNotNull(context.getBean(NocodePermissionEvaluator.class));
            var operation = new OperationDefinition(
                    "page", "分页", "", 0, HttpMethod.GET, null, null,
                    List.of(), Object.class, true);
            var service = new ServiceDefinition(
                    "test-service", "sample", "sampleGroup", CustomEntrypoint.class,
                    List.of(operation), true);
            assertThrows(SecurityException.class, () -> context.getBean(EntrypointInvocationAuthorizer.class)
                    .check(service, operation, new Object[0]));
        }
    }

    private ObjectProvider<NocodePermissionEvaluator> evaluatorProvider(NocodePermissionEvaluator evaluator) {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("permissionEvaluator", evaluator);
        return beans.getBeanProvider(NocodePermissionEvaluator.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class RegistryAuthorizationConfiguration {
        @Bean
        DefaultServiceDefinitionRegistry registry(List<EntrypointInvocationAuthorizer> authorizers) {
            return new EntrypointRegistryAutoConfiguration()
                    .ServiceDefinitionRegistry(List.of(), List.of(), authorizers);
        }

        @Bean
        EntrypointInvocationAuthorizer authorizer(ObjectProvider<NocodePermissionEvaluator> evaluators) {
            return new NocodeAutoConfiguration().nocodeInvocationAuthorizer(evaluators);
        }

        @Bean
        RegistryBackedAccessRules accessRules(ServiceDefinitionRegistry registry) {
            return new RegistryBackedAccessRules(registry);
        }

        @Bean
        NocodePermissionEvaluator evaluator(RegistryBackedAccessRules rules) {
            return authorization -> {
                assertNotNull(rules.registry());
                throw new SecurityException("denied");
            };
        }
    }

    private record RegistryBackedAccessRules(ServiceDefinitionRegistry registry) {
    }

    @Test
    void classifierMarksStandardCrudAndTreeCapabilityOperationsAsNocode() {
        var classifier = new NocodeAutoConfiguration().nocodeEntrypointClassifier();
        var page = Arrays.stream(ICrudService.class.getMethods())
                .filter(method -> method.getName().equals("page"))
                .findFirst()
                .orElseThrow();
        var custom = Arrays.stream(CustomEntrypoint.class.getMethods())
                .filter(method -> method.getName().equals("publish"))
                .findFirst()
                .orElseThrow();
        var tree = Arrays.stream(ITreeQueryService.class.getMethods())
                .filter(method -> method.getName().equals("tree"))
                .findFirst()
                .orElseThrow();
        var descendantIds = Arrays.stream(ITreeQueryService.class.getMethods())
                .filter(method -> method.getName().equals("descendantIds"))
                .findFirst()
                .orElseThrow();

        assertTrue(classifier.isNocode(ICrudService.class, page));
        assertTrue(classifier.isNocode(ITreeQueryService.class, tree));
        assertTrue(classifier.isNocode(ITreeQueryService.class, descendantIds));
        assertFalse(classifier.isNocode(ICrudService.class, custom));
    }

    private interface CustomEntrypoint extends IEntrypoint {
        @EntrypointOperation(operationName = "publish", displayName = "发布", httpMethod = HttpMethod.POST)
        void publish();
    }
}
