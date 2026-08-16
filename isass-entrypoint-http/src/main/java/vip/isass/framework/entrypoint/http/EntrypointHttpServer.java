// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.entrypoint.PropertyPresenceBinder;
import vip.isass.framework.entrypoint.stream.FileStream;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterSource;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.EntrypointInvocationGateway;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One runtime HTTP adapter for every local entrypoint operation. */
@RestController
public final class EntrypointHttpServer {

    private final ServiceDefinitionRegistry definitions;
    private final EntrypointInvocationGateway invocations;
    private final ObjectMapper objectMapper;

    public EntrypointHttpServer(ServiceDefinitionRegistry definitions,
                                EntrypointInvocationGateway invocations,
                                ObjectMapper objectMapper) {
        this.definitions = definitions;
        this.invocations = invocations;
        this.objectMapper = objectMapper;
    }

    @RequestMapping({
            "/{serviceName}/{contextName}/{resourceName}/{operationName}",
            "/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}"
    })
    public Object invoke(
            @PathVariable String serviceName,
            @PathVariable String contextName,
            @PathVariable String resourceName,
            @PathVariable String operationName,
            @RequestParam MultiValueMap<String, String> query,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        ServiceDefinition service = definitions.require(serviceName, contextName, resourceName);
        if (!service.localImplementation()) {
            throw new IllegalArgumentException("Entrypoint 在当前进程没有本地实现: " + service.key());
        }
        OperationDefinition operation = service.operations().stream()
                .filter(candidate -> candidate.operationName().equals(operationName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Unknown entrypoint operation: " + operationName));
        boolean nocodePath = request.getRequestURI().startsWith("/" + serviceName + "/nocode/");
        if (nocodePath != operation.nocode()) {
            throw new IllegalArgumentException("Entrypoint URL 命名空间错误: " + request.getRequestURI());
        }
        String actualMethod = request.getHeader("X-HTTP-Method-Override");
        if (actualMethod == null || actualMethod.isBlank()) actualMethod = request.getMethod();
        if (!operation.httpMethod().name().equalsIgnoreCase(actualMethod)) {
            throw new IllegalArgumentException("HTTP Method 不匹配: " + actualMethod);
        }
        Object[] arguments = new Object[operation.parameters().size()];
        for (ParameterDefinition parameter : operation.parameters()) {
            arguments[parameter.index()] = bind(parameter, query, request);
        }
        Object result = invocations.invoke(serviceName, contextName, resourceName, operationName, arguments);
        if (result instanceof FileStream fileStream) {
            writeFileResponse(fileStream, response);
            return null;
        }
        return result instanceof Resp<?> resp ? resp : Resp.bizSuccess(result);
    }

    private void writeFileResponse(FileStream fileStream, HttpServletResponse response) throws IOException {
        response.setContentType(fileStream.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                .builder(fileStream.download() ? "attachment" : "inline")
                .filename(fileStream.fileName(), StandardCharsets.UTF_8)
                .build().toString());
        if (fileStream.contentLength() != null && fileStream.contentLength() >= 0) {
            response.setContentLengthLong(fileStream.contentLength());
        }
        fileStream.writeTo(response.getOutputStream());
        response.flushBuffer();
    }

    private Object bind(ParameterDefinition parameter, MultiValueMap<String, String> query,
                        HttpServletRequest request) throws IOException {
        return switch (parameter.source()) {
            case QUERY -> parameter.objectQuery()
                    ? bindQueryObject(parameter.javaType(), query)
                    : convertQueryValues(parameter.javaType(), query.get(parameter.name()));
            case BODY -> bindBody(parameter, request);
            case HEADER -> convertQueryValues(parameter.javaType(),
                    java.util.Collections.list(request.getHeaders(parameter.name())));
            case FORM_FIELD -> bindFormField(parameter, request);
            case FORM_FILE -> bindFormFile(parameter, request);
        };
    }

    private Object bindBody(ParameterDefinition parameter, HttpServletRequest request) throws IOException {
        var tree = objectMapper.readTree(request.getInputStream());
        Object value = objectMapper.convertValue(tree,
                objectMapper.getTypeFactory().constructType(parameter.javaType()));
        PropertyPresenceBinder.bind(value, objectMapper.convertValue(tree, Object.class));
        return value;
    }

    private Object bindFormField(ParameterDefinition parameter, HttpServletRequest request) throws IOException {
        String value = request.getParameter(parameter.name());
        if (value == null) return null;
        Class<?> raw = rawClass(parameter.javaType());
        if (isSimple(raw)) return objectMapper.convertValue(value,
                objectMapper.getTypeFactory().constructType(parameter.javaType()));
        return objectMapper.readValue(value, objectMapper.getTypeFactory().constructType(parameter.javaType()));
    }

    private Object bindFormFile(ParameterDefinition parameter, HttpServletRequest request) throws IOException {
        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            throw new IllegalArgumentException("FormFileParam 要求 multipart/form-data");
        }
        MultipartFile file = multipartRequest.getFile(parameter.name());
        if (file == null || file.isEmpty()) return null;
        Class<?> raw = rawClass(parameter.javaType());
        if (raw == MultipartFile.class) return file;
        if (raw == byte[].class) return file.getBytes();
        if (java.io.InputStream.class.isAssignableFrom(raw)) return file.getInputStream();
        throw new IllegalArgumentException("不支持的 FormFileParam 类型: " + raw.getName());
    }

    private Object bindQueryObject(Type type, MultiValueMap<String, String> query) {
        Map<String, Object> source = new LinkedHashMap<>();
        Map<String, Map<String, Object>> associationCriteria = new LinkedHashMap<>();
        query.forEach((name, values) -> {
            Object value = values.size() == 1 ? values.getFirst() : values;
            if (name.equals("association.query")) {
                source.put("associationQueries", values);
            } else if (name.startsWith("association.") && name.contains(".criteria.")) {
                String remainder = name.substring("association.".length());
                int separator = remainder.indexOf(".criteria.");
                String association = remainder.substring(0, separator);
                String property = remainder.substring(separator + ".criteria.".length());
                associationCriteria.computeIfAbsent(association, ignored -> new LinkedHashMap<>())
                        .put(property, value);
            } else {
                source.put(name, value);
            }
        });
        if (!associationCriteria.isEmpty()) source.put("associationCriteria", associationCriteria);
        return bindQueryProperties(type, source);
    }

    private Object bindQueryProperties(Type type, Map<String, Object> source) {
        Class<?> raw = rawClass(type);
        try {
            var constructor = raw.getDeclaredConstructor();
            if (!constructor.canAccess(null)) constructor.setAccessible(true);
            Object target = constructor.newInstance();
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                Method setter = findQuerySetter(raw, entry.getKey());
                if (setter == null) {
                    continue;
                }
                Type parameterType = org.springframework.core.GenericTypeResolver.resolveType(
                        setter.getGenericParameterTypes()[0], raw);
                Object value = objectMapper.convertValue(entry.getValue(),
                        objectMapper.getTypeFactory().constructType(parameterType));
                if (!setter.canAccess(target)) setter.setAccessible(true);
                setter.invoke(target, value);
            }
            return target;
        } catch (NoSuchMethodException exception) {
            return objectMapper.convertValue(source, objectMapper.getTypeFactory().constructType(type));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalArgumentException("Query 对象绑定失败: " + raw.getName(), exception);
        }
    }

    private Method findQuerySetter(Class<?> type, String property) {
        String setterName = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(setterName) && method.getParameterCount() == 1)
                .findFirst()
                .orElse(null);
    }

    private Object convertQueryValues(Type type, List<String> values) {
        if (values == null || values.isEmpty()) return null;
        Class<?> raw = rawClass(type);
        Object source = raw.isArray() || Iterable.class.isAssignableFrom(raw) ? values : values.getFirst();
        return objectMapper.convertValue(source, objectMapper.getTypeFactory().constructType(type));
    }

    private Class<?> rawClass(Type type) {
        if (type instanceof Class<?> raw) return raw;
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) {
            return raw;
        }
        return Object.class;
    }

    private boolean isSimple(Class<?> type) {
        return type.isPrimitive() || Number.class.isAssignableFrom(type) || type == String.class
                || type == Boolean.class || type == Character.class || type.isEnum();
    }
}
