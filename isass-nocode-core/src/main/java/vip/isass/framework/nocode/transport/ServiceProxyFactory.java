package vip.isass.framework.nocode.transport;

import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ServiceContract;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

/**
 * Creates the single Java-facing service contract backed by local or remote transports.
 */
public class ServiceProxyFactory {

    private final TransportResolver resolver;

    public ServiceProxyFactory(TransportResolver resolver) {
        this.resolver = resolver;
    }

    public <T> T create(
            Class<T> serviceInterface,
            ServiceContract contract,
            List<InvocationTransport> transports
    ) {
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException(" service contract must be an interface");
        }
        InvocationHandler handler = (proxy, method, arguments) ->
                invoke(proxy, method, arguments, contract, transports);
        return serviceInterface.cast(Proxy.newProxyInstance(
                serviceInterface.getClassLoader(), new Class<?>[]{serviceInterface}, handler));
    }

    private Object invoke(
            Object proxy,
            Method method,
            Object[] arguments,
            ServiceContract contract,
            List<InvocationTransport> transports
    ) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "ServiceProxy[" + contract.serviceInterface() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, arguments);
        }
        OperationContract operation = contract.operations().stream()
                .filter(candidate -> candidate.name().equals(method.getName()))
                .filter(candidate -> candidate.parameters().size() == method.getParameterCount())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Operation is absent from  contract: " + method));
        Invocation invocation = new Invocation(
                contract.serviceName(),
                contract.entityName(),
                operation.name(),
                arguments == null ? List.of() : Arrays.asList(arguments),
                operation.idempotent());
        return resolver.invoke(invocation, transports);
    }
}
