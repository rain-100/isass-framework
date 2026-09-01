// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization.internal;

import org.junit.jupiter.api.Test;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalAccessRegistryTest {

    @Test
    void exposesOnlyOperationsDeclaredByJavaProvider() {
        InternalAccessRegistry registry = new InternalAccessRegistry(List.of(
                builder -> builder.allow(TestService.class, service -> service.read(null))),
                definitions(TestService.class, "read", false));

        assertTrue(registry.isAllowed("GET", "/test-service/sample/resource/read"));
        assertFalse(registry.isAllowed("POST", "/test-service/sample/resource/write"));
    }

    @Test
    void usesOperationLevelRouteClassificationFromEntrypointRegistry() {
        InternalAccessRegistry registry = new InternalAccessRegistry(List.of(
                builder -> builder.allow(TestService.class, service -> service.read(null))),
                definitions(TestService.class, "read", true));

        assertTrue(registry.isAllowed("GET", "/test-service/nocode/sample/resource/read"));
        assertFalse(registry.isAllowed("GET", "/test-service/sample/resource/read"));
    }

    @Test
    void exposesExplicitInfrastructureControllerRoute() {
        InternalAccessRegistry registry = new InternalAccessRegistry(List.of(
                builder -> builder.allowRoute("test-service/system/initialization#importData",
                        HttpMethod.POST, "/test-service/nocode/system/initialization/importData")),
                definitions(TestService.class, "read", false));

        assertTrue(registry.isAllowed("POST", "/test-service/nocode/system/initialization/importData"));
        assertFalse(registry.isAllowed("GET", "/test-service/nocode/system/initialization/importData"));
    }

    private ServiceDefinitionRegistry definitions(Class<? extends IEntrypoint> serviceInterface,
                                                  String operationName,
                                                  boolean nocode) {
        EntrypointInfo info = serviceInterface.getAnnotation(EntrypointInfo.class);
        Method method = List.of(serviceInterface.getMethods()).stream()
                .filter(candidate -> candidate.getName().equals(operationName))
                .findFirst().orElseThrow();
        EntrypointOperation operation = method.getAnnotation(EntrypointOperation.class);
        OperationDefinition operationDefinition = new OperationDefinition(
                operation.operationName(), operation.displayName(), operation.description(),
                operation.displayOrder(), operation.httpMethod(), operation.accessStrategy(), method,
                List.of(), method.getGenericReturnType(), nocode);
        ServiceDefinition definition = new ServiceDefinition(
                info.serviceName(), info.contextName(), info.resourceName(), serviceInterface,
                List.of(operationDefinition), true);
        return new ServiceDefinitionRegistry() {
            @Override
            public List<ServiceDefinition> all() {
                return List.of(definition);
            }

            @Override
            public Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName) {
                return definition.serviceName().equals(serviceName)
                        && definition.contextName().equals(contextName)
                        && definition.resourceName().equals(resourceName)
                        ? Optional.of(definition) : Optional.empty();
            }
        };
    }

    @EntrypointInfo(serviceName = "test-service", contextName = "sample", resourceName = "resource")
    interface TestService extends IEntrypoint {
        @EntrypointOperation(operationName = "read", displayName = "读", httpMethod = HttpMethod.GET)
        String read(String value);

        @EntrypointOperation(operationName = "write", displayName = "写", httpMethod = HttpMethod.POST)
        void write(String value);
    }
}
