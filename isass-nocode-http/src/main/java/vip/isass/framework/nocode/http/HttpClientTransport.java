// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ParameterSource;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.transport.Invocation;
import vip.isass.framework.nocode.transport.InvocationTransport;
import vip.isass.framework.nocode.transport.TransportInvocationException;
import vip.isass.framework.nocode.transport.TransportKind;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remote HTTP fallback for the same logical I service contract.
 */
public class HttpClientTransport implements InvocationTransport {

    private final NocodeHttpExchange exchange;
    private final HttpEndpointResolver endpoints;
    private final ContractRegistry contracts;
    private final ObjectMapper objectMapper;

    public HttpClientTransport(
            NocodeHttpExchange exchange,
            HttpEndpointResolver endpoints,
            ContractRegistry contracts,
            ObjectMapper objectMapper
    ) {
        this.exchange = exchange;
        this.endpoints = endpoints;
        this.contracts = contracts;
        this.objectMapper = objectMapper;
    }

    public TransportKind kind() {
        return TransportKind.HTTP;
    }

    public boolean available(Invocation invocation) {
        return endpoints.resolve(invocation.service()) != null;
    }

    public Object invoke(Invocation invocation) {
        ServiceContract service = contracts.requireService(
                invocation.service(), invocation.entity());
        OperationContract operation = service.operations().stream()
                .filter(candidate -> candidate.name().equals(invocation.operationName()))
                .findFirst().orElseThrow();
        try {
            Request request = request(invocation, operation);
            JsonNode response = exchange.exchange(
                    HttpMethod.valueOf(operation.httpMethod().name()),
                    request.uri(), request.query(), request.body());
            if (response != null && response.has("success") && !response.path("success").asBoolean()) {
                String message = response.path("detailMessage").asText();
                if (message.isBlank()) {
                    message = response.path("message").asText("Unknown remote business error");
                }
                throw new TransportInvocationException(
                        "HTTP invocation failed: " + invocation.service() + "/"
                                + invocation.entity() + "#" + invocation.operationName()
                                + ": " + message,
                        true);
            }
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || data.isMissingNode() || data.isNull()) {
                return null;
            }
            return objectMapper.convertValue(data,
                    objectMapper.getTypeFactory().constructFromCanonical(operation.returnJavaType()));
        } catch (TransportInvocationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TransportInvocationException(
                    " HTTP invocation failed: " + invocation.operationName(), true, exception);
        }
    }

    private Request request(Invocation invocation, OperationContract operation) {
        URI base = endpoints.resolve(invocation.service());
        if (base == null) {
            throw new TransportInvocationException(
                    "No HTTP endpoint for " + invocation.service(), false);
        }
        String path = operation.path();
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        Map<String, Object> bodies = new LinkedHashMap<>();
        int bodyCount = (int) operation.parameters().stream()
                .filter(parameter -> parameter.source() == ParameterSource.BODY).count();
        for (int index = 0; index < operation.parameters().size(); index++) {
            var parameter = operation.parameters().get(index);
            Object value = invocation.arguments().get(index);
            switch (parameter.source()) {
                case PATH -> path = path.replace("{" + parameter.name() + "}", String.valueOf(value));
                case QUERY -> {
                    if (value != null && !isSimpleValue(value)) {
                        Map<?, ?> map = objectMapper.convertValue(value, Map.class);
                        map.forEach((key, item) -> addQueryValue(query, String.valueOf(key), item));
                    } else if (value != null) {
                        addQueryValue(query, parameter.name(), value);
                    }
                }
                case BODY -> bodies.put(parameter.name(), value);
            }
        }
        String relative = "/".equals(path) ? "" : path;
        URI uri = base.resolve("/" + invocation.service() + "/" + invocation.entity() + relative);
        Object body = bodyCount == 0 ? null
                : bodyCount == 1 ? bodies.values().iterator().next()
                : bodies;
        if (body instanceof CharSequence) {
            body = objectMapper.valueToTree(body);
        }
        return new Request(uri, query, body);
    }

    private void addQueryValue(MultiValueMap<String, String> query, String name, Object value) {
        if (value instanceof Iterable<?> values) {
            values.forEach(item -> {
                if (isSimpleValue(item)) {
                    addQueryValue(query, name, item);
                }
            });
        } else if (isSimpleValue(value)) {
            query.add(name, String.valueOf(value));
        }
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private record Request(URI uri, MultiValueMap<String, String> query, Object body) {
    }
}
