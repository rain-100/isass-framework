// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry snapshot for entrypoints implemented by the current process. */
public final class DefaultServiceDefinitionRegistry
        implements ServiceDefinitionRegistry, EntrypointInvocationGateway {

    private final Map<String, ServiceDefinition> definitions;
    private final Map<String, IEntrypoint> localImplementations;
    private final List<EntrypointInvocationAuthorizer> authorizers;

    public DefaultServiceDefinitionRegistry(
            Collection<? extends IEntrypoint> localBeans,
            List<EntrypointClassifier> classifiers,
            List<EntrypointInvocationAuthorizer> authorizers
    ) {
        EntrypointDefinitionParser parser = new EntrypointDefinitionParser(classifiers);
        Map<String, ServiceDefinition> parsed = new LinkedHashMap<>();
        Map<String, IEntrypoint> implementations = new LinkedHashMap<>();
        for (IEntrypoint bean : localBeans) {
            Class<? extends IEntrypoint> serviceInterface = resolveServiceInterface(bean.getClass());
            boolean local = !(bean instanceof RemoteEntrypointProxy);
            ServiceDefinition definition = parser.parse(serviceInterface, local);
            if (parsed.putIfAbsent(definition.key(), definition) != null) {
                throw new IllegalStateException("Entrypoint 定义重复: " + definition.key());
            }
            if (local) {
                implementations.put(definition.key(), bean);
            }
        }
        definitions = Map.copyOf(parsed);
        localImplementations = Map.copyOf(implementations);
        this.authorizers = List.copyOf(authorizers);
    }

    @Override
    public Collection<ServiceDefinition> all() {
        return definitions.values();
    }

    @Override
    public Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName) {
        return Optional.ofNullable(definitions.get(key(serviceName, contextName, resourceName)));
    }

    @Override
    public Object invoke(String serviceName, String contextName, String resourceName,
                         String operationName, Object[] arguments) {
        ServiceDefinition service = require(serviceName, contextName, resourceName);
        var operation = service.operations().stream()
                .filter(candidate -> candidate.operationName().equals(operationName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown entrypoint operation: " + operationName));
        IEntrypoint target = localImplementations.get(service.key());
        if (target == null) {
            throw new IllegalArgumentException("Entrypoint 在当前进程没有本地实现: " + service.key());
        }
        Object[] invocationArguments = arguments == null ? new Object[0] : arguments;
        authorizers.forEach(authorizer -> authorizer.check(service, operation, invocationArguments));
        try {
            return operation.javaMethod().invoke(target, invocationArguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Entrypoint operation 无法访问: " + operation.javaMethod(), exception);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Entrypoint operation 执行失败: " + operationName, exception.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends IEntrypoint> resolveServiceInterface(Class<?> implementationType) {
        List<Class<?>> matches = allInterfaces(implementationType).stream()
                .filter(IEntrypoint.class::isAssignableFrom)
                .filter(type -> type != IEntrypoint.class)
                .filter(type -> type != RemoteEntrypointProxy.class)
                .filter(type -> type.isAnnotationPresent(EntrypointInfo.class))
                .distinct()
                .toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Entrypoint 实现必须唯一对应一个带 @EntrypointInfo 的服务接口: "
                    + implementationType.getName() + " -> " + matches);
        }
        return (Class<? extends IEntrypoint>) matches.getFirst();
    }

    private List<Class<?>> allInterfaces(Class<?> type) {
        if (type == null || type == Object.class) {
            return List.of();
        }
        return java.util.stream.Stream.concat(
                        ArraysSupport.interfaces(type).stream(),
                        allInterfaces(type.getSuperclass()).stream())
                .distinct().toList();
    }

    private String key(String serviceName, String contextName, String resourceName) {
        return serviceName + "/" + contextName + "/" + resourceName;
    }

    private static final class ArraysSupport {
        private static List<Class<?>> interfaces(Class<?> type) {
            return java.util.Arrays.stream(type.getInterfaces())
                    .flatMap(candidate -> java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(candidate), interfaces(candidate).stream()))
                    .toList();
        }
    }
}
