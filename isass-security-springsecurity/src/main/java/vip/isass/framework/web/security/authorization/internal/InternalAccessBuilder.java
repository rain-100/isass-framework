// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization.internal;

import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** 类型安全地捕获 Entrypoint 方法并生成内部访问规则。 */
public final class InternalAccessBuilder {

    private final List<InternalAccessRule> rules = new ArrayList<>();
    private final ServiceDefinitionRegistry serviceDefinitions;

    public InternalAccessBuilder(ServiceDefinitionRegistry serviceDefinitions) {
        this.serviceDefinitions = serviceDefinitions;
    }

    public <S extends IEntrypoint> InternalAccessBuilder allow(
            Class<S> serviceInterface, Consumer<S> operationCall) {
        if (serviceInterface == null || operationCall == null) {
            throw new IllegalArgumentException("Entrypoint 接口和操作调用必填");
        }
        CapturedMethod captured = new CapturedMethod();
        Object proxy = Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface}, (instance, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(instance, method, arguments);
                    }
                    if (captured.method != null) {
                        throw new IllegalArgumentException("一次 allow 只能捕获一个 Entrypoint 操作");
                    }
                    captured.method = method;
                    return defaultValue(method.getReturnType());
                });
        operationCall.accept(serviceInterface.cast(proxy));
        if (captured.method == null) {
            throw new IllegalArgumentException("未捕获到 Entrypoint 操作");
        }
        EntrypointInfo info = serviceInterface.getAnnotation(EntrypointInfo.class);
        EntrypointOperation operation = captured.method.getAnnotation(EntrypointOperation.class);
        if (info == null || operation == null) {
            throw new IllegalArgumentException("只能开放带 @EntrypointInfo/@EntrypointOperation 的方法");
        }
        ServiceDefinition service = serviceDefinitions.require(
                info.serviceName(), info.contextName(), info.resourceName());
        OperationDefinition definition = service.operations().stream()
                .filter(candidate -> candidate.operationName().equals(operation.operationName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown entrypoint operation: " + operation.operationName()));
        String operationKey = info.serviceName() + "/" + info.contextName() + "/"
                + info.resourceName() + "#" + operation.operationName();
        String path = service.pathPrefix(definition) + "/" + definition.operationName();
        rules.add(new InternalAccessRule(operationKey, definition.httpMethod().name(), path));
        return this;
    }

    /**
     * 开放没有 Entrypoint 接口的基础设施 Controller 路由。
     *
     * <p>业务入口应优先使用类型安全的 {@link #allow(Class, Consumer)}；该方法仅用于框架基础设施路由。</p>
     */
    public InternalAccessBuilder allowRoute(String operationKey, HttpMethod httpMethod, String path) {
        if (operationKey == null || operationKey.isBlank()) {
            throw new IllegalArgumentException("内部访问 operationKey 必填");
        }
        if (httpMethod == null) {
            throw new IllegalArgumentException("内部访问 HTTP Method 必填");
        }
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException("内部访问路径必须是以 / 开头的绝对路径");
        }
        rules.add(new InternalAccessRule(operationKey, httpMethod.name(), path));
        return this;
    }

    public Collection<InternalAccessRule> build() {
        return List.copyOf(rules);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private static Object objectMethod(Object proxy, Method method, Object[] arguments) {
        return switch (method.getName()) {
            case "toString" -> "InternalAccessEntrypointProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (arguments == null ? null : arguments[0]);
            default -> null;
        };
    }

    private static final class CapturedMethod {
        private Method method;
    }
}
