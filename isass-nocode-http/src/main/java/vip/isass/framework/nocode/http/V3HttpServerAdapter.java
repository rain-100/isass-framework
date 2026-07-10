package vip.isass.framework.nocode.http;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3HttpMethod;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterSource;
import vip.isass.framework.nocode.v3.contract.V3RouteMatcher;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single dynamic HTTP entry point for all V3 service contracts.
 */
@RestController
public class V3HttpServerAdapter {

    private final V3ContractRegistry contracts;
    private final V3ServiceRegistry services;
    private final ObjectMapper objectMapper;

    public V3HttpServerAdapter(
            V3ContractRegistry contracts,
            V3ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        this.contracts = contracts;
        this.services = services;
        this.objectMapper = objectMapper;
    }

    @RequestMapping("/{serviceName}/{entityName}/v3")
    public Resp<?> invokeRoot(
            @PathVariable String serviceName,
            @PathVariable String entityName,
            @RequestParam MultiValueMap<String, String> query,
            @RequestBody(required = false) JsonNode body,
            org.springframework.web.context.request.WebRequest request
    ) {
        return invokeInternal(serviceName, entityName, "/", query, body, request);
    }

    @RequestMapping("/{serviceName}/{entityName}/v3/{*operationPath}")
    public Resp<?> invoke(
            @PathVariable String serviceName,
            @PathVariable String entityName,
            @PathVariable String operationPath,
            @RequestParam MultiValueMap<String, String> query,
            @RequestBody(required = false) JsonNode body,
            org.springframework.web.context.request.WebRequest request
    ) {
        return invokeInternal(serviceName, entityName, operationPath, query, body, request);
    }

    private Resp<?> invokeInternal(
            String serviceName,
            String entityName,
            String operationPath,
            MultiValueMap<String, String> query,
            JsonNode body,
            org.springframework.web.context.request.WebRequest request
    ) {
        V3HttpMethod httpMethod = V3HttpMethod.valueOf(
                request.getHeader("X-HTTP-Method-Override") == null
                        ? currentMethod(request)
                        : request.getHeader("X-HTTP-Method-Override"));
        String relativePath = operationPath.startsWith("/") ? operationPath : "/" + operationPath;
        V3OperationContract operation = contracts.requireOperation(
                serviceName, entityName, httpMethod, relativePath);
        IV3Service<?, ?> service = services.require(serviceName, entityName);
        Object result = invokeService(service, operation, relativePath, query, body);
        return result instanceof Resp<?> response ? response : Resp.bizSuccess(result);
    }

    private String currentMethod(org.springframework.web.context.request.WebRequest request) {
        String description = request.getDescription(false);
        int methodStart = description.indexOf("method=");
        if (methodStart >= 0) {
            int end = description.indexOf(';', methodStart);
            return description.substring(methodStart + 7, end < 0 ? description.length() : end);
        }
        if (request instanceof org.springframework.web.context.request.ServletWebRequest servlet) {
            return servlet.getRequest().getMethod();
        }
        throw new IllegalStateException("Cannot resolve HTTP method");
    }

    private Object invokeService(
            Object service,
            V3OperationContract operation,
            String relativePath,
            MultiValueMap<String, String> query,
            JsonNode body
    ) {
        Method method = findMethod(service, operation);
        List<Object> arguments = bindArguments(operation, relativePath, query, body);
        try {
            return method.invoke(service, arguments.toArray());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access V3 operation " + operation.name(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("V3 operation failed: " + operation.name(), cause);
        }
    }

    private Method findMethod(Object service, V3OperationContract operation) {
        return java.util.Arrays.stream(service.getClass().getMethods())
                .filter(method -> method.getName().equals(operation.name()))
                .filter(method -> method.getParameterCount() == operation.parameters().size())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Service method is missing: " + operation.name()));
    }

    private List<Object> bindArguments(
            V3OperationContract operation,
            String relativePath,
            MultiValueMap<String, String> query,
            JsonNode body
    ) {
        Map<String, String> pathVariables =
                V3RouteMatcher.requireVariables(operation.path(), relativePath);
        List<Object> arguments = new ArrayList<>();
        long bodyParameterCount = operation.parameters().stream()
                .filter(parameter -> parameter.source() == V3ParameterSource.BODY).count();
        for (V3ParameterContract parameter : operation.parameters()) {
            Object value = switch (parameter.source()) {
                case PATH -> pathVariables.get(parameter.name());
                case QUERY -> isSimple(parameter.javaType())
                        ? query.getFirst(parameter.name())
                        : bindQueryObject(query, parameter.javaType());
                case BODY -> bodyParameterCount > 1 && body != null
                        ? body.get(parameter.name())
                        : body;
            };
            arguments.add(convert(value, parameter.javaType()));
        }
        return arguments;
    }

    private Object bindQueryObject(MultiValueMap<String, String> query, String javaType) {
        try {
            tools.jackson.databind.JavaType type =
                    objectMapper.getTypeFactory().constructFromCanonical(javaType);
            Object target = type.getRawClass().getDeclaredConstructor().newInstance();
            for (Map.Entry<String, List<String>> entry : query.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                Optional<Method> setter = findSetter(type.getRawClass(), entry.getKey());
                if (setter.isEmpty()) {
                    continue;
                }
                Method method = setter.get();
                Object rawValue = Collection.class.isAssignableFrom(method.getParameterTypes()[0])
                        ? entry.getValue()
                        : entry.getValue().getFirst();
                Object converted = objectMapper.convertValue(rawValue,
                        objectMapper.getTypeFactory().constructType(resolveSetterParameterType(method, type.getRawClass())));
                method.invoke(target, new Object[]{converted});
            }
            return target;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Cannot bind V3 query parameter to " + javaType, exception);
        }
    }

    private Type resolveSetterParameterType(Method method, Class<?> targetClass) {
        Type parameterType = method.getGenericParameterTypes()[0];
        if (!(parameterType instanceof TypeVariable<?> variable)) {
            return parameterType;
        }
        return resolveTypeVariable(targetClass, method.getDeclaringClass(), variable)
                .orElse(parameterType);
    }

    private Optional<Type> resolveTypeVariable(
            Class<?> targetClass,
            Class<?> declaringClass,
            TypeVariable<?> variable
    ) {
        for (Type type : targetClass.getGenericInterfaces()) {
            Optional<Type> resolved = resolveTypeVariable(type, declaringClass, variable);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        Class<?> superclass = targetClass.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return Optional.empty();
        }
        return resolveTypeVariable(superclass, declaringClass, variable);
    }

    private Optional<Type> resolveTypeVariable(
            Type candidate,
            Class<?> declaringClass,
            TypeVariable<?> variable
    ) {
        if (!(candidate instanceof ParameterizedType parameterizedType)
                || !(parameterizedType.getRawType() instanceof Class<?> rawType)) {
            return Optional.empty();
        }
        if (rawType == declaringClass) {
            TypeVariable<?>[] variables = rawType.getTypeParameters();
            Type[] arguments = parameterizedType.getActualTypeArguments();
            for (int index = 0; index < variables.length; index++) {
                if (variables[index].getName().equals(variable.getName())) {
                    return Optional.of(arguments[index]);
                }
            }
        }
        for (Type type : rawType.getGenericInterfaces()) {
            Optional<Type> resolved = resolveTypeVariable(type, declaringClass, variable);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return Optional.empty();
    }

    private Optional<Method> findSetter(Class<?> targetClass, String propertyName) {
        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        return java.util.Arrays.stream(targetClass.getMethods())
                .filter(method -> method.getName().equals(setterName))
                .filter(method -> method.getParameterCount() == 1)
                .findFirst();
    }

    private Object convert(Object value, String javaType) {
        if (value == null) {
            return null;
        }
        try {
            tools.jackson.databind.JavaType type =
                    objectMapper.getTypeFactory().constructFromCanonical(javaType);
            if (value instanceof String text && type.isCollectionLikeType()) {
                value = List.of(text.split(","));
            }
            return objectMapper.convertValue(value, type);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Cannot convert V3 parameter to " + javaType, exception);
        }
    }

    private boolean isSimple(String javaType) {
        return javaType.startsWith("java.lang.")
                || javaType.startsWith("java.time.")
                || javaType.equals("java.io.Serializable");
    }
}
