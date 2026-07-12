package vip.isass.framework.nocode.http;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.HttpMethod;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ParameterContract;
import vip.isass.framework.nocode.contract.ParameterSource;
import vip.isass.framework.nocode.contract.RouteMatcher;
import vip.isass.framework.nocode.service.IService;
import vip.isass.framework.nocode.stream.FileStream;
import vip.isass.framework.nocode.stream.FileNotFoundException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single dynamic HTTP entry point for all  service contracts.
 */
@RestController
public class HttpServerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpServerAdapter.class);

    private final ContractRegistry contracts;
    private final ServiceRegistry services;
    private final ObjectMapper objectMapper;

    public HttpServerAdapter(
            ContractRegistry contracts,
            ServiceRegistry services,
            ObjectMapper objectMapper
    ) {
        this.contracts = contracts;
        this.services = services;
        this.objectMapper = objectMapper;
    }

    @RequestMapping("/{serviceName}/{entityName}")
    public Object invokeRoot(
            @PathVariable String serviceName,
            @PathVariable String entityName,
            @RequestParam MultiValueMap<String, String> query,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        return invokeInternal(serviceName, entityName, "/", query, request);
    }

    @RequestMapping("/{serviceName}/{entityName}/{*operationPath}")
    public Object invoke(
            @PathVariable String serviceName,
            @PathVariable String entityName,
            @PathVariable String operationPath,
            @RequestParam MultiValueMap<String, String> query,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        return invokeInternal(serviceName, entityName, operationPath, query, request);
    }

    private Object invokeInternal(
            String serviceName,
            String entityName,
            String operationPath,
            MultiValueMap<String, String> query,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        HttpMethod httpMethod = HttpMethod.valueOf(
                request.getHeader("X-HTTP-Method-Override") == null
                        ? currentMethod(request)
                        : request.getHeader("X-HTTP-Method-Override"));
        String relativePath = operationPath.startsWith("/") ? operationPath : "/" + operationPath;
        OperationContract operation = contracts.requireOperation(
                serviceName, entityName, httpMethod, relativePath);
        IService<?, ?> service = services.require(serviceName, entityName);
        if (isFileOperation(operation)) {
            return invokeFileOperation(service, operation, relativePath, query, request);
        }
        Object result = invokeService(service, operation, relativePath, query, request);
        return result instanceof Resp<?> response ? response : Resp.bizSuccess(result);
    }

    /**
     * 文件端点不使用 Resp：下载前的资源不存在、参数错误和服务异常分别直接映射为 404、400、5xx。
     */
    private Object invokeFileOperation(
            IService<?, ?> service,
            OperationContract operation,
            String relativePath,
            MultiValueMap<String, String> query,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        try {
            Object result = invokeService(service, operation, relativePath, query, request);
            if (!(result instanceof FileStream fileStream)) {
                throw new IllegalStateException(" 文件接口未返回 FileStream: " + operation.name());
            }
            streamResponse(fileStream, request);
            return null;
        } catch (FileNotFoundException exception) {
            return emptyFileResponse(request, HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException exception) {
            return emptyFileResponse(request, HttpStatus.BAD_REQUEST);
        } catch (Exception exception) {
            log.error(" 文件接口执行失败: {}", operation.name(), exception);
            return emptyFileResponse(request, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isFileOperation(OperationContract operation) {
        return FileStream.class.getName().equals(operation.returnJavaType());
    }

    private void streamResponse(
            FileStream fileStream,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        jakarta.servlet.http.HttpServletResponse response = request.getResponse();
        if (response == null) {
            throw new IllegalStateException(" 文件接口缺少 HttpServletResponse");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(fileStream.contentType()));
        headers.setContentDisposition((fileStream.download()
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(fileStream.fileName())
                .build());
        if (fileStream.contentLength() != null) {
            headers.setContentLength(fileStream.contentLength());
        }
        response.setStatus(HttpStatus.OK.value());
        headers.forEach((name, values) -> response.setHeader(name, String.join(",", values)));
        try {
            fileStream.writeTo(response.getOutputStream());
            response.flushBuffer();
        } catch (IOException | RuntimeException exception) {
            if (!response.isCommitted()) {
                log.error(" 文件接口传输失败: {}", fileStream.fileName(), exception);
                emptyFileResponse(request, HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                // 响应已提交时 HTTP 状态无法改写；记录错误并中断连接，客户端可由 Content-Length 识别不完整文件。
                log.error(" 文件流传输失败，响应已提交: {}", fileStream.fileName(), exception);
            }
        }
    }

    private Object emptyFileResponse(
            org.springframework.web.context.request.ServletWebRequest request,
            HttpStatus status
    ) {
        jakarta.servlet.http.HttpServletResponse response = request.getResponse();
        if (response != null && !response.isCommitted()) {
            response.reset();
            response.setStatus(status.value());
            response.setContentLength(0);
        }
        return null;
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
            OperationContract operation,
            String relativePath,
            MultiValueMap<String, String> query,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        Method method = findMethod(service, operation);
        List<Object> arguments = bindArguments(operation, relativePath, query, request);
        try {
            return method.invoke(service, arguments.toArray());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access  operation " + operation.name(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(" operation failed: " + operation.name(), cause);
        }
    }

    private Method findMethod(Object service, OperationContract operation) {
        return java.util.Arrays.stream(service.getClass().getMethods())
                .filter(method -> method.getName().equals(operation.name()))
                .filter(method -> method.getParameterCount() == operation.parameters().size())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Service method is missing: " + operation.name()));
    }

    private List<Object> bindArguments(
            OperationContract operation,
            String relativePath,
            MultiValueMap<String, String> query,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        Map<String, String> pathVariables =
                RouteMatcher.requireVariables(operation.path(), relativePath);
        List<Object> arguments = new ArrayList<>();
        long bodyParameterCount = operation.parameters().stream()
                .filter(parameter -> parameter.source() == ParameterSource.BODY).count();
        JsonNode body = readJsonBody(request);
        for (ParameterContract parameter : operation.parameters()) {
            Object value = switch (parameter.source()) {
                case PATH -> pathVariables.get(parameter.name());
                case QUERY -> isSimple(parameter.javaType())
                        ? query.getFirst(parameter.name())
                        : bindQueryObject(query, parameter.javaType());
                case BODY -> InputStream.class.getName().equals(parameter.javaType())
                        ? multipartInputStream(request, parameter.name())
                        : request.getRequest() instanceof MultipartHttpServletRequest multipart
                        ? bindMultipartObject(multipart, parameter.javaType())
                        : bodyParameterCount > 1 && body != null ? body.get(parameter.name()) : body;
            };
            arguments.add(convert(value, parameter.javaType()));
        }
        populateMultipartOriginalFileName(operation, arguments, request);
        return arguments;
    }

    private Object bindMultipartObject(MultipartHttpServletRequest multipart, String javaType) {
        org.springframework.util.LinkedMultiValueMap<String, String> form =
                new org.springframework.util.LinkedMultiValueMap<>();
        multipart.getParameterMap().forEach((name, values) -> form.put(name, java.util.Arrays.asList(values)));
        return bindQueryObject(form, javaType);
    }

    /**
     * Keep the legacy controller convention: when a multipart DTO exposes a writable
     * {@code fileName} but the caller omits it, use the uploaded file's original name.
     */
    private void populateMultipartOriginalFileName(
            OperationContract operation,
            List<Object> arguments,
            org.springframework.web.context.request.ServletWebRequest request
    ) {
        if (!(request.getRequest() instanceof MultipartHttpServletRequest multipart)) {
            return;
        }
        for (int index = 0; index < operation.parameters().size(); index++) {
            ParameterContract parameter = operation.parameters().get(index);
            if (!InputStream.class.getName().equals(parameter.javaType())) {
                continue;
            }
            MultipartFile file = multipart.getFile(parameter.name());
            if (file == null || file.isEmpty() || file.getOriginalFilename() == null
                    || file.getOriginalFilename().isBlank()) {
                continue;
            }
            for (Object argument : arguments) {
                if (argument == null) {
                    continue;
                }
                Optional<Method> getter = java.util.Arrays.stream(argument.getClass().getMethods())
                        .filter(method -> method.getName().equals("getFileName"))
                        .filter(method -> method.getParameterCount() == 0)
                        .findFirst();
                Optional<Method> setter = findSetter(argument.getClass(), "fileName");
                if (getter.isEmpty() || setter.isEmpty()) {
                    continue;
                }
                try {
                    Object fileName = getter.get().invoke(argument);
                    if (fileName == null || (fileName instanceof String text && text.isBlank())) {
                        setter.get().invoke(argument, file.getOriginalFilename());
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalArgumentException("Cannot populate multipart fileName", exception);
                }
            }
        }
    }

    private JsonNode readJsonBody(org.springframework.web.context.request.ServletWebRequest request) {
        if (request.getRequest() instanceof MultipartHttpServletRequest) {
            return null;
        }
        String contentType = request.getRequest().getContentType();
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).contains("json")) {
            return null;
        }
        try {
            return objectMapper.readTree(request.getRequest().getInputStream());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot read  JSON request body", exception);
        }
    }

    private InputStream multipartInputStream(
            org.springframework.web.context.request.ServletWebRequest request,
            String parameterName
    ) {
        if (!(request.getRequest() instanceof MultipartHttpServletRequest multipart)) {
            throw new IllegalArgumentException(" stream parameter '" + parameterName
                    + "' requires multipart/form-data");
        }
        MultipartFile file = multipart.getFile(parameterName);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Required multipart file parameter is missing: " + parameterName);
        }
        try {
            return file.getInputStream();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot open multipart file parameter: " + parameterName, exception);
        }
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
            throw new IllegalArgumentException("Cannot bind  query parameter to " + javaType, exception);
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
        if (InputStream.class.getName().equals(javaType) && value instanceof InputStream inputStream) {
            return inputStream;
        }
        try {
            tools.jackson.databind.JavaType type =
                    objectMapper.getTypeFactory().constructFromCanonical(javaType);
            if (value instanceof String text && type.isCollectionLikeType()) {
                value = List.of(text.split(","));
            }
            return objectMapper.convertValue(value, type);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Cannot convert  parameter to " + javaType, exception);
        }
    }

    private boolean isSimple(String javaType) {
        return javaType.startsWith("java.lang.")
                || javaType.startsWith("java.time.")
                || javaType.equals("java.io.Serializable");
    }
}
