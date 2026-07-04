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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                        : query.toSingleValueMap();
                case BODY -> bodyParameterCount > 1 && body != null
                        ? body.get(parameter.name())
                        : body;
            };
            arguments.add(convert(value, parameter.javaType()));
        }
        return arguments;
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
