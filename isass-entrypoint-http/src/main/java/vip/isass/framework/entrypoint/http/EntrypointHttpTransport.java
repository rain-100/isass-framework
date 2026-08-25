// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.http;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.entrypoint.PropertyPresenceBinder;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterSource;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.transport.EntrypointTransport;
import vip.isass.framework.entrypoint.transport.EntrypointTransportException;
import vip.isass.framework.entrypoint.transport.EntrypointRemoteBusinessException;

import java.lang.reflect.Array;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

public final class EntrypointHttpTransport implements EntrypointTransport {

    private final RestClient restClient;
    private final HttpEndpointResolver endpoints;
    private final ObjectMapper objectMapper;
    private final List<AdditionalRequestHeaderProvider> headerProviders;

    public EntrypointHttpTransport(RestClient restClient, HttpEndpointResolver endpoints,
                                   ObjectMapper objectMapper,
                                   List<AdditionalRequestHeaderProvider> headerProviders) {
        this.restClient = restClient;
        this.endpoints = endpoints;
        this.objectMapper = objectMapper;
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
        for (ParameterDefinition parameter : operation.parameters()) {
            Object value = arguments[parameter.index()];
            switch (parameter.source()) {
                case QUERY -> addQuery(query, parameter, value);
                case BODY -> body = value == null ? null : PropertyPresenceBinder.project(value,
                        objectMapper.convertValue(value, Object.class));
                case HEADER -> {
                    if (value != null) headers.add(parameter.name(), String.valueOf(value));
                }
                case FORM_FIELD -> {
                    if (value != null) form.add(parameter.name(), isSimple(value.getClass())
                            ? String.valueOf(value) : objectMapper.writeValueAsString(value));
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
        for (AdditionalRequestHeaderProvider provider : headerProviders) {
            if (provider.support(operation.httpMethod().name(), uri.toString())
                    && (provider.override() || headers.getFirst(provider.getHeaderName()) == null)) {
                headers.set(provider.getHeaderName(), provider.getValue());
            }
        }
        try {
            RestClient.RequestBodySpec request = restClient.method(
                            org.springframework.http.HttpMethod.valueOf(operation.httpMethod().name()))
                    .uri(uri).headers(target -> target.addAll(headers)).accept(MediaType.APPLICATION_JSON);
            if (multipart) request.contentType(MediaType.MULTIPART_FORM_DATA).body(form);
            else if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            JsonNode response = request.retrieve().body(JsonNode.class);
            if (response == null) return null;
            if (response.has("success") && !response.path("success").asBoolean()) {
                throw new EntrypointRemoteBusinessException(
                        response.path("message").asText("远程业务调用失败"));
            }
            JsonNode data = response.has("data") ? response.path("data") : response;
            if (data.isNull() || data.isMissingNode()) return null;
            return objectMapper.convertValue(data,
                    objectMapper.getTypeFactory().constructType(operation.returnType()));
        } catch (EntrypointTransportException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new EntrypointTransportException(
                    "HTTP 调用失败: " + service.key() + "#" + operation.operationName(), false, exception);
        }
    }

    private Object formResource(String name, Object value) {
        if (value instanceof byte[] bytes) {
            return new ByteArrayResource(bytes) {
                @Override public String getFilename() { return name; }
            };
        }
        if (value instanceof InputStream stream) {
            return new InputStreamResource(stream) {
                @Override public String getFilename() { return name; }
                @Override public long contentLength() { return -1; }
            };
        }
        throw new EntrypointTransportException("不支持的 FormFileParam 类型: " + value.getClass().getName(), true);
    }

    private void addQuery(MultiValueMap<String, String> query, ParameterDefinition parameter, Object value) {
        if (value == null) return;
        if (parameter.objectQuery()) {
            JsonNode properties = objectMapper.valueToTree(value);
            properties.properties().forEach(entry -> {
                String property = entry.getKey();
                JsonNode propertyValue = entry.getValue();
                if (property.equals("associationQueries")) {
                    addValues(query, "association.query", propertyValue);
                } else if (property.equals("whereConditions")) {
                    addWhereConditions(query, propertyValue);
                } else if (property.equals("associationCriteria") && propertyValue.isObject()) {
                    propertyValue.properties().forEach(association -> {
                        JsonNode criteria = association.getValue();
                        if (!criteria.isObject()) return;
                        criteria.properties().forEach(criterion -> addValues(query,
                                "association." + association.getKey() + ".criteria." + criterion.getKey(),
                                criterion.getValue()));
                    });
                } else {
                    addValues(query, property, propertyValue);
                }
            });
        } else {
            addValues(query, parameter.name(), value);
        }
    }

    private void addWhereConditions(MultiValueMap<String, String> query, JsonNode conditions) {
        if (conditions.isNull() || conditions.isMissingNode()) return;
        if (!conditions.isArray()) {
            throw new IllegalArgumentException("whereConditions 必须是数组");
        }
        boolean orNext = false;
        for (JsonNode conditionNode : conditions) {
            String condition = conditionNode.path("condition").asText();
            if (condition.equals("OR")) {
                orNext = true;
                continue;
            }
            String propertyName = conditionNode.path("propertyName").asText();
            if (propertyName.isBlank() || condition.isBlank()) {
                throw new IllegalArgumentException("whereConditions 缺少 propertyName 或 condition");
            }
            String queryName = orNext
                    ? "or" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1)
                    : propertyName;
            queryName += conditionSuffix(condition);
            JsonNode conditionValue = conditionNode.path("value");
            if (conditionValue.isNull() || conditionValue.isMissingNode()) {
                if (condition.equals("IS_NULL") || condition.equals("IS_NOT_NULL")
                        || condition.equals("IS_EMPTY") || condition.equals("IS_NOT_EMPTY")) {
                    query.add(queryName, "true");
                }
            } else {
                addValues(query, queryName, conditionValue);
            }
            orNext = false;
        }
        if (orNext) {
            throw new IllegalArgumentException("whereConditions 不能以 OR 结束");
        }
    }

    private String conditionSuffix(String condition) {
        return switch (condition) {
            case "EQUAL" -> "";
            case "NOT_EQUAL" -> "NotEqual";
            case "IN" -> "In";
            case "NOT_IN" -> "NotIn";
            case "IS_NULL" -> "IsNull";
            case "IS_NOT_NULL" -> "IsNotNull";
            case "IS_EMPTY" -> "IsEmpty";
            case "IS_NOT_EMPTY" -> "IsNotEmpty";
            case "GREATER_THAN" -> "GreaterThan";
            case "GREATER_THAN_EQUAL" -> "GreaterThanEqual";
            case "LESS_THAN" -> "LessThan";
            case "LESS_THAN_EQUAL" -> "LessThanEqual";
            case "START_WITH" -> "StartWith";
            case "LIKE" -> "Like";
            case "NOT_LIKE" -> "NotLike";
            case "CONTAINS_ALL" -> "ContainsAll";
            case "CONTAINS_ANY" -> "ContainsAny";
            case "JSON_OBJECT_PATH_EQUAL" -> "JsonObjectPathEqual";
            case "JSON_OBJECT_PATH_LIKE" -> "JsonObjectPathLike";
            case "JSON_ARRAY_CONTAINS" -> "JsonArrayContains";
            case "JSON_ARRAY_CONTAINS_ANY" -> "JsonArrayContainsAny";
            case "JSON_ARRAY_CONTAINS_ALL" -> "JsonArrayContainsAll";
            default -> throw new IllegalArgumentException("不支持的 whereCondition: " + condition);
        };
    }

    private void addValues(MultiValueMap<String, String> query, String name, Object value) {
        if (value == null) return;
        if (value instanceof JsonNode node) {
            if (node.isNull() || node.isMissingNode()) return;
            if (node.isArray()) {
                node.forEach(item -> addValues(query, name, item));
            } else if (node.isValueNode()) {
                query.add(name, node.asText());
            } else if (!node.isEmpty()) {
                throw new IllegalArgumentException("Query 对象只允许展开一层，属性不能是复杂对象: " + name);
            }
        } else if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> addValues(query, name, item));
        } else if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                addValues(query, name, Array.get(value, index));
            }
        } else if (isSimple(value.getClass())) {
            query.add(name, String.valueOf(value));
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
