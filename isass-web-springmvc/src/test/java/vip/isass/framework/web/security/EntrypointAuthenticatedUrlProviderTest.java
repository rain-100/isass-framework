// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import org.junit.jupiter.api.Test;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EntrypointAuthenticatedUrlProviderTest {

    @Test
    void exposesOnlyAuthenticatedOperationsWithLocalImplementations() throws Exception {
        Method method = TestEntrypoint.class.getMethod("invoke");
        ServiceDefinition local = new ServiceDefinition(
                "test-service", "sample", "localResource", TestEntrypoint.class,
                List.of(
                        operation(method, "authenticated", UrlAccessSecurityStrategy.AUTHENTICATED),
                        operation(method, "permissionProtected", UrlAccessSecurityStrategy.ROLE)),
                true);
        ServiceDefinition remote = new ServiceDefinition(
                "remote-service", "sample", "remoteResource", TestEntrypoint.class,
                List.of(operation(method, "authenticated", UrlAccessSecurityStrategy.AUTHENTICATED)),
                false);

        assertThat(new EntrypointAuthenticatedUrlProvider(new TestRegistry(List.of(local, remote))).getUrls())
                .containsExactly("/test-service/sample/localResource/authenticated");
    }

    private OperationDefinition operation(Method method, String operationName,
                                          UrlAccessSecurityStrategy accessStrategy) {
        return new OperationDefinition(operationName, operationName, "", 0, HttpMethod.GET,
                accessStrategy, method, List.of(), method.getGenericReturnType(), false);
    }

    private interface TestEntrypoint extends IEntrypoint {
        void invoke();
    }

    private record TestRegistry(List<ServiceDefinition> definitions) implements ServiceDefinitionRegistry {
        @Override
        public Collection<ServiceDefinition> all() {
            return definitions;
        }

        @Override
        public Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName) {
            return definitions.stream()
                    .filter(service -> service.serviceName().equals(serviceName)
                            && service.contextName().equals(contextName)
                            && service.resourceName().equals(resourceName))
                    .findFirst();
        }
    }
}
