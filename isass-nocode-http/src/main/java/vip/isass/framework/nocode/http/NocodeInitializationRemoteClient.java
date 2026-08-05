package vip.isass.framework.nocode.http;

import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/** Calls the target service's generic initialization endpoint without invoking business CRUD APIs. */
final class NocodeInitializationRemoteClient {

    private final NocodeHttpExchange exchange;
    private final HttpEndpointResolver endpoints;
    private final ObjectMapper objectMapper;

    NocodeInitializationRemoteClient(
            NocodeHttpExchange exchange,
            HttpEndpointResolver endpoints,
            ObjectMapper objectMapper
    ) {
        this.exchange = exchange;
        this.endpoints = endpoints;
        this.objectMapper = objectMapper;
    }

    NocodeInitializationDataService.ImportResult importData(
            String serviceName,
            Map<String, ? extends Collection<?>> document
    ) {
        URI base = endpoints.resolve(serviceName);
        if (base == null) throw new IllegalStateException("No HTTP endpoint for " + serviceName);
        URI uri = base.resolve("/" + serviceName + "/init-data/import");
        JsonNode response = exchange.exchange(HttpMethod.POST, uri, new LinkedMultiValueMap<>(), document);
        if (response == null || !response.path("success").asBoolean()) {
            String message = response == null ? "No response" : response.path("detailMessage").asText();
            if (message.isBlank() && response != null) message = response.path("message").asText();
            throw new IllegalStateException("Remote initialization failed for " + serviceName + ": " + message);
        }
        return objectMapper.convertValue(response.path("data"), NocodeInitializationDataService.ImportResult.class);
    }
}
