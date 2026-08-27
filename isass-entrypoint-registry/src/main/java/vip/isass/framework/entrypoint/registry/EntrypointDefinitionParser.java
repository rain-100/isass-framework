// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.BodyParam;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.annotation.FormFieldParam;
import vip.isass.framework.entrypoint.annotation.FormFileParam;
import vip.isass.framework.entrypoint.annotation.HeaderParam;
import vip.isass.framework.entrypoint.annotation.QueryParam;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterSource;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses and validates runtime entrypoint annotations without protocol-specific rules. */
public final class EntrypointDefinitionParser {

    private final List<EntrypointClassifier> classifiers;

    public EntrypointDefinitionParser(List<EntrypointClassifier> classifiers) {
        this.classifiers = classifiers == null ? List.of() : List.copyOf(classifiers);
    }

    public ServiceDefinition parse(Class<? extends IEntrypoint> serviceInterface, boolean localImplementation) {
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException("Entrypoint 必须是接口: " + serviceInterface.getName());
        }
        EntrypointInfo info = serviceInterface.getAnnotation(EntrypointInfo.class);
        if (info == null) {
            throw new IllegalArgumentException("Entrypoint 缺少 @EntrypointInfo: " + serviceInterface.getName());
        }
        requireSegment(info.serviceName(), "serviceName");
        requireSegment(info.contextName(), "contextName");
        requireSegment(info.resourceName(), "resourceName");

        Map<String, OperationDefinition> operations = new LinkedHashMap<>();
        for (Method method : serviceInterface.getMethods()) {
            EntrypointOperation annotation = method.getAnnotation(EntrypointOperation.class);
            if (annotation == null) {
                continue;
            }
            OperationDefinition operation = parseOperation(serviceInterface, method, annotation);
            if (operations.putIfAbsent(operation.operationName(), operation) != null) {
                throw new IllegalArgumentException("Entrypoint operationName 重复: " + operation.operationName());
            }
        }
        List<OperationDefinition> sorted = operations.values().stream()
                .sorted(Comparator.comparingInt(OperationDefinition::displayOrder)
                        .thenComparing(OperationDefinition::operationName)
                        .thenComparing(operation -> operation.httpMethod().name()))
                .toList();
        return new ServiceDefinition(info.serviceName(), info.contextName(), info.resourceName(),
                serviceInterface, sorted, localImplementation);
    }

    private OperationDefinition parseOperation(Class<? extends IEntrypoint> serviceInterface,
                                               Method method,
                                               EntrypointOperation annotation) {
        requireSegment(annotation.operationName(), "operationName");
        if (annotation.displayName().isBlank()) {
            throw new IllegalArgumentException("Entrypoint displayName 不能为空: " + method);
        }
        List<ParameterDefinition> parameters = new ArrayList<>();
        int bodyCount = 0;
        Parameter[] javaParameters = method.getParameters();
        for (int index = 0; index < javaParameters.length; index++) {
            Parameter parameter = javaParameters[index];
            Type resolvedType = org.springframework.core.GenericTypeResolver.resolveType(
                    parameter.getParameterizedType(), serviceInterface);
            ParameterDefinition definition = parseParameter(index, parameter, resolvedType);
            parameters.add(definition);
            if (definition.source() == ParameterSource.BODY) {
                bodyCount++;
            }
        }
        if (bodyCount > 1) {
            throw new IllegalArgumentException("一个 Entrypoint operation 最多只能有一个 @BodyParam: " + method);
        }
        boolean nocode = classifiers.stream()
                .anyMatch(classifier -> classifier.isNocode(serviceInterface, method));
        return new OperationDefinition(annotation.operationName(), annotation.displayName(),
                annotation.description(), annotation.displayOrder(), annotation.httpMethod(),
                annotation.allowAnonymous(), method,
                parameters, org.springframework.core.GenericTypeResolver.resolveType(
                        method.getGenericReturnType(), serviceInterface), nocode);
    }

    private ParameterDefinition parseParameter(int index, Parameter parameter, Type resolvedType) {
        List<Annotation> sources = Arrays.stream(parameter.getAnnotations())
                .filter(annotation -> annotation instanceof QueryParam
                        || annotation instanceof BodyParam
                        || annotation instanceof HeaderParam
                        || annotation instanceof FormFieldParam
                        || annotation instanceof FormFileParam)
                .toList();
        if (sources.size() != 1) {
            throw new IllegalArgumentException("Entrypoint 参数必须且只能声明一个来源注解: " + parameter);
        }
        Annotation source = sources.getFirst();
        if (source instanceof QueryParam query) {
            Class<?> resolvedRawType = org.springframework.core.ResolvableType.forType(resolvedType).resolve(Object.class);
            boolean objectQuery = !isSimple(resolvedRawType) && !resolvedRawType.isArray()
                    && !Iterable.class.isAssignableFrom(resolvedRawType);
            String name = query.value();
            if (!objectQuery && name.isBlank()) {
                throw new IllegalArgumentException("简单 @QueryParam 必须声明名称: " + parameter);
            }
            if (objectQuery && !name.isBlank()) {
                throw new IllegalArgumentException("对象 @QueryParam 不应声明名称: " + parameter);
            }
            return new ParameterDefinition(index, name, ParameterSource.QUERY,
                    resolvedType, objectQuery);
        }
        if (source instanceof BodyParam) {
            return new ParameterDefinition(index, "", ParameterSource.BODY,
                    resolvedType, false);
        }
        if (source instanceof HeaderParam header) {
            return named(index, header.value(), ParameterSource.HEADER, parameter, resolvedType);
        }
        if (source instanceof FormFieldParam formField) {
            return named(index, formField.value(), ParameterSource.FORM_FIELD, parameter, resolvedType);
        }
        FormFileParam formFile = (FormFileParam) source;
        return named(index, formFile.value(), ParameterSource.FORM_FILE, parameter, resolvedType);
    }

    private ParameterDefinition named(int index, String name, ParameterSource source,
                                      Parameter parameter, Type resolvedType) {
        if (name.isBlank()) {
            throw new IllegalArgumentException(source + " 参数名称不能为空: " + parameter);
        }
        return new ParameterDefinition(index, name, source, resolvedType, false);
    }

    private boolean isSimple(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type) || Boolean.class == type
                || Character.class == type || java.time.temporal.Temporal.class.isAssignableFrom(type)
                || java.util.UUID.class == type;
    }

    private void requireSegment(String value, String name) {
        if (value == null || !value.matches("[a-z][a-zA-Z0-9-]*")) {
            throw new IllegalArgumentException(name + " 必须是合法的 lowerCamelCase/服务名路径段: " + value);
        }
    }
}
