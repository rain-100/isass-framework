// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.entrypoint.http.HttpEndpointResolver;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class NocodeInitializationRemoteClient {

    private final HttpEndpointResolver endpoints;
    private final ObjectMapper objectMapper;
    private final List<AdditionalRequestHeaderProvider> headerProviders;

    public NocodeInitializationRemoteClient(HttpEndpointResolver endpoints, ObjectMapper objectMapper,
                                            List<AdditionalRequestHeaderProvider> headerProviders) {
        this.endpoints = endpoints;
        this.objectMapper = objectMapper;
        this.headerProviders = List.copyOf(headerProviders);
    }

    public NocodeInitializationDataService.ImportResult importData(
            String serviceName, Map<String, ? extends Collection<?>> document) {
        URI endpoint = endpoints.resolve(serviceName);
        if (endpoint == null) throw new IllegalStateException("未配置初始化目标地址: " + serviceName);
        URI uri = endpoint.resolve("/" + serviceName + "/nocode/system/initialization/importData");
        HttpHeaders headers = new HttpHeaders();
        for (AdditionalRequestHeaderProvider provider : headerProviders) {
            if (provider.support("POST", uri.toString())
                    && (provider.override() || headers.getFirst(provider.getHeaderName()) == null)) {
                headers.set(provider.getHeaderName(), provider.getValue());
            }
        }
        JsonNode response = RestClient.create().post().uri(uri)
                .headers(target -> target.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(document).retrieve().body(JsonNode.class);
        if (response == null || !response.path("success").asBoolean()) {
            String message = response == null ? "无响应" : response.path("detailMessage").asText();
            if (message.isBlank() && response != null) message = response.path("message").asText();
            throw new IllegalStateException("远程初始化失败 " + serviceName + ": " + message);
        }
        return objectMapper.convertValue(response.path("data"),
                NocodeInitializationDataService.ImportResult.class);
    }
}
