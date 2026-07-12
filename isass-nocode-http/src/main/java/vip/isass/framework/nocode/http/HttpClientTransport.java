package vip.isass.framework.nocode.http;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
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

    private final RestClient restClient;
    private final HttpEndpointResolver endpoints;
    private final ContractRegistry contracts;
    private final ObjectMapper objectMapper;

    public HttpClientTransport(
            RestClient restClient,
            HttpEndpointResolver endpoints,
            ContractRegistry contracts,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.endpoints = endpoints;
        this.contracts = contracts;
        this.objectMapper = objectMapper;
    }

    public TransportKind kind() {
        return TransportKind.HTTP;
    }

    public boolean available(Invocation invocation) {
        return endpoints.resolve(invocation.serviceName()) != null;
    }

    public Object invoke(Invocation invocation) {
        ServiceContract service = contracts.requireService(
                invocation.serviceName(), invocation.entityName());
        OperationContract operation = service.operations().stream()
                .filter(candidate -> candidate.name().equals(invocation.operationName()))
                .findFirst().orElseThrow();
        try {
            Request request = request(invocation, operation);
            RestClient.RequestBodySpec spec = restClient.method(
                    HttpMethod.valueOf(operation.httpMethod().name())).uri(request.uri());
            JsonNode response = request.body() == null
                    ? spec.retrieve().body(JsonNode.class)
                    : spec.body(request.body()).retrieve().body(JsonNode.class);
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
        URI base = endpoints.resolve(invocation.serviceName());
        if (base == null) {
            throw new TransportInvocationException(
                    "No HTTP endpoint for " + invocation.serviceName(), false);
        }
        String path = operation.path();
        UriComponentsBuilder uri = UriComponentsBuilder.fromUri(base)
                .pathSegment(invocation.serviceName(), invocation.entityName());
        if (!"/".equals(path)) {
            uri.path(path);
        }
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
                        map.forEach((key, item) -> uri.queryParam(String.valueOf(key), item));
                    } else if (value != null) {
                        uri.queryParam(parameter.name(), value);
                    }
                }
                case BODY -> bodies.put(parameter.name(), value);
            }
        }
        UriComponentsBuilder finalUri = UriComponentsBuilder.fromUri(base)
                .pathSegment(invocation.serviceName(), invocation.entityName());
        if (!"/".equals(path)) {
            finalUri.path(path);
        }
        uri.build().getQueryParams().forEach(finalUri::queryParam);
        Object body = bodyCount == 0 ? null
                : bodyCount == 1 ? bodies.values().iterator().next()
                : bodies;
        return new Request(finalUri.build(true).toUri(), body);
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>;
    }

    private record Request(URI uri, Object body) {
    }
}
