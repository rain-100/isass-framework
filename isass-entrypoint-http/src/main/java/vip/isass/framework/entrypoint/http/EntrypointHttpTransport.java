// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.http;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderContext;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.entrypoint.PropertyPresenceBinder;
import vip.isass.framework.entrypoint.QueryParamConverter;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterSource;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.transport.EntrypointRemoteBusinessException;
import vip.isass.framework.entrypoint.transport.EntrypointTransport;
import vip.isass.framework.entrypoint.transport.EntrypointTransportException;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class EntrypointHttpTransport implements EntrypointTransport {

    private final RestClient restClient;
    private final HttpEndpointResolver endpoints;
    private final List<QueryParamConverter> queryConverters;
    private final List<AdditionalRequestHeaderProvider> headerProviders;

    public EntrypointHttpTransport(RestClient restClient, HttpEndpointResolver endpoints,
                                   List<AdditionalRequestHeaderProvider> headerProviders) {
        this(restClient, endpoints, headerProviders, List.of());
    }

    public EntrypointHttpTransport(RestClient restClient, HttpEndpointResolver endpoints,
                                   List<AdditionalRequestHeaderProvider> headerProviders,
                                   List<QueryParamConverter> queryConverters) {
        this.queryConverters = List.copyOf(queryConverters);
        this.restClient = restClient;
        this.endpoints = endpoints;
        this.headerProviders = List.copyOf(headerProviders);
    }

    @Override
    public String name() {
        return "HTTP";
    }

    @Override
    public boolean supports(ServiceDefinition service, OperationDefinition operation) {
        return endpoints.resolve(service.serviceName()) != null;
    }

    @Override
    public Object invoke(ServiceDefinition service, OperationDefinition operation, Object[] arguments) {
        URI endpoint = endpoints.resolve(service.serviceName());
        if (endpoint == null) {
            throw new EntrypointTransportException("未配置 HTTP 地址: " + service.serviceName(), true);
        }
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        Object body = null;
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        boolean multipart = operation.parameters().stream().anyMatch(parameter ->
                parameter.source() == ParameterSource.FORM_FIELD
                        || parameter.source() == ParameterSource.FORM_FILE);
        Set<String> namedQueryParameters = operation.parameters().stream()
                .filter(parameter -> parameter.source() == ParameterSource.QUERY && !parameter.objectQuery())
                .map(ParameterDefinition::name).collect(Collectors.toSet());
        for (ParameterDefinition parameter : operation.parameters()) {
            Object value = arguments[parameter.index()];
            switch (parameter.source()) {
                case QUERY -> addQuery(query, parameter, value, namedQueryParameters);
                case BODY -> body = value == null ? null : PropertyPresenceBinder.project(value,
                        JsonUtil.convertValue(value, Object.class));
                case HEADER -> {
                    if (value != null) headers.add(parameter.name(), String.valueOf(value));
                }
                case FORM_FIELD -> {
                    if (value != null) {
                        if (isSimple(value.getClass())) {
                            form.add(parameter.name(), String.valueOf(value));
                        } else {
                            MultiValueMap<String, String> fields = new LinkedMultiValueMap<>();
                            JsonUtil.valueToTree(value).properties().forEach(entry ->
                                    addValues(fields, entry.getKey(), entry.getValue()));
                            fields.forEach((name, values) -> values.forEach(item -> form.add(name, item)));
                        }
                    }
                }
                case FORM_FILE -> {
                    if (value != null) form.add(parameter.name(), formResource(parameter.name(), value));
                }
            }
        }
        if (multipart && body != null) {
            throw new EntrypointTransportException("multipart 入口不能同时声明 BodyParam", true);
        }
        URI uri = UriComponentsBuilder.fromUri(endpoint)
                .path(service.pathPrefix(operation)).pathSegment(operation.operationName())
                .queryParams(query).build().encode().toUri();
        byte[] serializedBody = body == null ? new byte[0] : JsonUtil.writeValueAsBytes(body);
        AdditionalRequestHeaderContext headerContext = new AdditionalRequestHeaderContext(
                operation.httpMethod().name(), uri, serializedBody, multipart);
        for (AdditionalRequestHeaderProvider provider : headerProviders) {
            provider.getHeaders(headerContext).forEach((name, value) -> {
                if (provider.override() || headers.getFirst(name) == null) {
                    headers.set(name, value);
                }
            });
        }
        try {
            RestClient.RequestBodySpec request = restClient.method(
                            org.springframework.http.HttpMethod.valueOf(operation.httpMethod().name()))
                    .uri(uri).headers(target -> target.addAll(headers)).accept(MediaType.APPLICATION_JSON);
            if (multipart) request.contentType(MediaType.MULTIPART_FORM_DATA).body(form);
            else if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(serializedBody);
            JsonNode response = request.retrieve().body(JsonNode.class);
            if (response == null) return null;
            if (response.has("success") && !response.path("success").asBoolean()) {
                throw new EntrypointRemoteBusinessException(
                        response.path("message").asString("远程业务调用失败"));
            }
            JsonNode data = response.has("data") ? response.path("data") : response;
            if (data.isNull() || data.isMissingNode()) return null;
            return JsonUtil.convertValue(data, operation.returnType());
        } catch (EntrypointTransportException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EntrypointTransportException(
                    "HTTP 调用失败: " + service.key() + "#" + operation.operationName(), false, exception);
        }
    }

    private Object formResource(String name, Object value) {
        if (value instanceof org.springframework.web.multipart.MultipartFile file) {
            return file.getResource();
        }
        if (value instanceof byte[] bytes) {
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return name;
                }
            };
        }
        if (value instanceof InputStream stream) {
            return new InputStreamResource(stream) {
                @Override
                public String getFilename() {
                    return name;
                }

                @Override
                public long contentLength() {
                    return -1;
                }
            };
        }
        throw new EntrypointTransportException("不支持的 FormFileParam 类型: " + value.getClass().getName(), true);
    }

    private void addQuery(MultiValueMap<String, String> query, ParameterDefinition parameter, Object value,
                          Set<String> namedQueryParameters) {
        if (value == null) return;
        QueryParamConverter converter = QueryParamConverter.select(queryConverters, parameter.javaType());
        if (converter != null) {
            converter.toQueryParams(value, parameter.javaType()).forEach((name, text) -> {
                if (parameter.objectQuery() && namedQueryParameters.contains(name)) return;
                if (query.containsKey(name)) throw new IllegalArgumentException("重复 QUERY 属性: " + name);
                query.add(name, text);
            });
        } else if (parameter.objectQuery()) {
            JsonUtil.valueToTree(value).properties().forEach(entry -> addValues(query, entry.getKey(), entry.getValue()));
        } else {
            addValues(query, parameter.name(), value);
        }
    }

    private void addValues(MultiValueMap<String, String> query, String name, Object value) {
        if (value == null) return;
        List<String> values = new ArrayList<>();
        collectValues(values, name, value);
        if (!values.isEmpty()) {
            query.add(name, String.join(",", values));
        }
    }

    private void collectValues(List<String> values, String name, Object value) {
        if (value == null) return;
        if (value instanceof JsonNode node) {
            if (node.isNull() || node.isMissingNode()) return;
            if (node.isArray()) {
                node.forEach(item -> collectValues(values, name, item));
            } else if (node.isValueNode()) {
                values.add(node.asString());
            } else if (!node.isEmpty()) {
                throw new IllegalArgumentException("Query 对象只允许展开一层，属性不能是复杂对象: " + name);
            }
        } else if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> collectValues(values, name, item));
        } else if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                collectValues(values, name, Array.get(value, index));
            }
        } else if (isSimple(value.getClass())) {
            values.add(String.valueOf(value));
        } else {
            throw new IllegalArgumentException("Query 对象只允许展开一层，属性不能是复杂对象: " + name);
        }
    }

    private boolean isSimple(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type) || Boolean.class == type
                || Character.class == type || java.time.temporal.Temporal.class.isAssignableFrom(type)
                || java.util.UUID.class == type;
    }
}
