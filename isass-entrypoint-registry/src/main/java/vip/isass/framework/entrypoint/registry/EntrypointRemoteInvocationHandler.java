// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import org.springframework.beans.factory.ListableBeanFactory;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.transport.EntrypointTransport;
import vip.isass.framework.entrypoint.transport.EntrypointTransportException;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

final class EntrypointRemoteInvocationHandler implements InvocationHandler {

    private final Class<? extends IEntrypoint> serviceInterface;
    private final ListableBeanFactory beanFactory;

    EntrypointRemoteInvocationHandler(
            Class<? extends IEntrypoint> serviceInterface,
            ListableBeanFactory beanFactory
    ) {
        this.serviceInterface = serviceInterface;
        this.beanFactory = beanFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "RemoteEntrypointProxy[" + serviceInterface.getName() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> method.invoke(this, arguments);
            };
        }
        EntrypointOperation annotation = method.getAnnotation(EntrypointOperation.class);
        if (annotation == null) {
            if (method.isDefault()) {
                return MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup())
                        .unreflectSpecial(method, method.getDeclaringClass())
                        .bindTo(proxy)
                        .invokeWithArguments(arguments == null ? List.of() : Arrays.asList(arguments));
            }
            throw new UnsupportedOperationException("未标注 @EntrypointOperation 的抽象方法不能远程调用: " + method);
        }

        List<EntrypointClassifier> classifiers = beanFactory.getBeanProvider(EntrypointClassifier.class)
                .orderedStream().toList();
        ServiceDefinition service = new EntrypointDefinitionParser(classifiers).parse(serviceInterface, false);
        OperationDefinition operation = service.operations().stream()
                .filter(candidate -> candidate.operationName().equals(annotation.operationName()))
                .findFirst().orElseThrow();
        EntrypointClientProperties properties = beanFactory.getBean(EntrypointClientProperties.class);
        List<String> order = properties.transportOrder(service.serviceName());
        List<EntrypointTransport> transports = beanFactory.getBeanProvider(EntrypointTransport.class)
                .orderedStream()
                .sorted(Comparator.comparingInt(transport -> indexOf(order, transport.name())))
                .toList();
        EntrypointTransportException unavailable = null;
        for (String requestedName : order) {
            EntrypointTransport transport = transports.stream()
                    .filter(candidate -> candidate.name().equalsIgnoreCase(requestedName))
                    .findFirst().orElse(null);
            if (transport == null || !transport.supports(service, operation)) {
                continue;
            }
            try {
                return transport.invoke(service, operation, arguments == null ? new Object[0] : arguments);
            } catch (EntrypointTransportException exception) {
                if (!exception.unavailableBeforeSend()) {
                    throw exception;
                }
                unavailable = exception;
            }
        }
        throw new EntrypointTransportException(
                "没有可用传输: " + service.key() + "#" + operation.operationName(), true, unavailable);
    }

    private int indexOf(List<String> order, String name) {
        for (int index = 0; index < order.size(); index++) {
            if (order.get(index).equalsIgnoreCase(name)) {
                return index;
            }
        }
        return Integer.MAX_VALUE;
    }
}
